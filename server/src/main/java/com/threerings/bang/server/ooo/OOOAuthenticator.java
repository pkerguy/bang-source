//
// $Id$

package com.threerings.bang.server.ooo;

import com.google.inject.*;
import com.samskivert.io.*;
import com.samskivert.jdbc.*;
import com.samskivert.servlet.*;
import com.samskivert.servlet.user.*;
import com.samskivert.util.*;
import com.samskivert.util.RandomUtil;
import com.threerings.bang.admin.server.*;
import com.threerings.bang.data.*;
import com.threerings.bang.server.*;
import com.threerings.bang.server.persist.*;
import com.threerings.bang.util.*;
import com.threerings.presents.net.*;
import com.threerings.presents.server.net.*;
import com.threerings.user.*;
import com.threerings.util.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Date;
import java.sql.*;
import java.util.*;

import static com.threerings.bang.Log.*;
import static com.threerings.bang.data.BangAuthCodes.*;

/**
 * Delegates authentication to the OOO user manager.
 */
@Singleton
public class OOOAuthenticator extends BangAuthenticator
{
    @Override // from abstract BangAuthenticator
    public void init ()
    {
        try {
            // we get our user manager configuration from the ocean config
            _usermgr = new OOOUserManager(
                ServerConfig.config.getSubProperties("oooauth"), _conprov);
            _authrep = _usermgr.getRepository();
            _siteident = new JDBCTableSiteIdentifier(_conprov);
            _rewardrep = new RewardRepository(_conprov);

        } catch (PersistenceException pe) {
            BangServer.DISCORD.commit(1, "Failed to initialize OOO authenticator.", pe);
        }
    }

    @Override // from abstract BangAuthenticator
    public void setAccountIsActive (String username, boolean isActive)
        throws PersistenceException
    {
        // pass the word on to the user repository
        _authrep.updateUserIsActive(username, OOOUser.IS_ACTIVE_BANG_PLAYER, isActive);
    }

    @Override // from abstract BangAuthenticator
    public String createAccount (String username, String password, String email, String affiliate,
            String machIdent, Date birthdate)
        throws PersistenceException
    {
        // check if their username already exists
        if (_authrep.loadUser(username) != null) {
            return NAME_IN_USE;
        }

        int siteId = getSiteId(affiliate);

        try {
            // make sure that this machine identifier is allowed to create a new account
            int rv = _authrep.checkCanCreate(machIdent, OOOUser.BANGHOWDY_SITE_ID);
            if(DeploymentConfig.beta_build)
            {
                rv = 0;
            }
            switch(rv) {
            case OOOUserRepository.NEW_ACCOUNT_TAINTED:
                return MACHINE_TAINTED;
            case OOOUserRepository.NO_NEW_FREE_ACCOUNT: // we don't care, let 'em in
            case OOOUserRepository.ACCESS_GRANTED:
                break;

            default:
                BangServer.DISCORD.commit(1, "Unhandled checkCanCreate() response code", "ident", machIdent,
                            "rv", rv);
                return SERVER_ERROR;
            }

            Username uname = new Username(username);
            Password pass = Password.makeFromClear(password);

            // create the account
            _authrep.createUser(uname, pass, email, siteId, 0, birthdate, (byte)-1, null);

            return null;

        } catch (InvalidUsernameException iue) {
            // the client shouldn't allow invalid names, so we just give a generic exception here
            BangServer.DISCORD.commit(1, "User submitted invalid username?", "username", username);
            return SERVER_ERROR;

        } catch (UserExistsException uee) {
            return NAME_IN_USE;

        } catch (PersistenceException pe) {
            BangServer.DISCORD.commit(1, "Error creating arround", "username", username, "password", password,
                        "siteId", siteId, "ident", machIdent, pe);
            return SERVER_ERROR;
        }
    }

    @Override // from abstract BangAuthenticator
    public List<String> redeemRewards (String username, String ident)
    {
        // redeem any rewards for which they have become eligible
        List<String> rdata = new ArrayList<String>();
        try {
            List<RewardRecord> rewards = _rewardrep.loadActivatedRewards(username, ident);
            for (RewardRecord record : rewards) {
                if (record.account.equals(username) &&
                    StringUtil.isBlank(record.redeemerIdent)) {
                    String info = maybeRedeemReward(username, ident, record, rewards);
                    if (info != null) {
                        rdata.add(info);
                    }
                }
            }
        } catch (Exception e) {
            BangServer.DISCORD.commit(1, "Failed to redeem rewards", "who", username, e);
        }
        return rdata;
    }

    @Override // from Authenticator
    protected AuthResponseData createResponseData ()
    {
        return new BangAuthResponseData();
    }

    @Override // from abstract Authenticator
    protected void processAuthentication (AuthingConnection conn, AuthResponse rsp)
        throws PersistenceException
    {
        AuthRequest req = conn.getAuthRequest();
        BangAuthResponseData rdata = (BangAuthResponseData) rsp.getData();

        // make sure we were properly initialized
        if (_authrep == null) {
            rdata.code = SERVER_ERROR;
            return;
        }

        // make sure they've got the correct version
        long cvers = 0L;
        long svers = DeploymentConfig.getVersion();
        try {
            cvers = Long.parseLong(req.getVersion());
        } catch (Exception e) {
            // ignore it and fail below
        }
        if (svers != cvers) {
            rdata.code = (cvers > svers) ? NEWER_VERSION :
                MessageBundle.tcompose(VERSION_MISMATCH, "" + svers);
            log.info("Refusing wrong version", "creds", req.getCredentials(), "cvers", cvers,
                     "svers", svers);
            return;
        }

        // make sure they've sent valid credentials
        BangCredentials creds;
        try {
            creds = (BangCredentials) req.getCredentials();
        } catch (ClassCastException cce) {
            BangServer.DISCORD.commit(1, "Invalid creds " + req.getCredentials() + ".");
            rdata.code = SERVER_ERROR;
            return;
        }

        // check their provided machine identifier
        String username = creds.getUsername().toString();
        if (StringUtil.isBlank(creds.ident)) {
            BangServer.DISCORD.commit(1, "Received blank ident", "creds", creds);
            BangServer.generalLog("refusing_spoofed_ident " + username +
                                  " ip:" + conn.getInetAddress());
            rdata.code = SERVER_ERROR;
            return;
        }

        // load up their user account record
        OOOUser user = _authrep.loadUser(username, true);

        // we need to find out if this account has ever logged in so that we can decide how to
        // handle tainted idents; we load up the player record for this account; if this player
        // makes it through the gauntlet, we'll stash this away in a place that the client resolver
        // can get it so that we can avoid loading the record twice during authentication
        PlayerRecord prec = _playrepo.loadPlayer(username);

        String password = creds.getPassword();

        // TODO: FIX THIS STEAM STUFF

//        try {
//            BeginAuthSessionResult result = SteamServer.user.beginAuthSession(creds.ticketBuffer, creds.steamID);
//            if(result != BeginAuthSessionResult.OK)
//            {
//                rdata.code = INVALID_PASSWORD;
//                return;
//            }
//
//        } catch (SteamException e) {
//            log.info("Steam exception occurred while trying to authenticate a user!");
//            e.printStackTrace();
//            rdata.code = SERVER_ERROR;
//            return;
//        }

        if (user == null) {
            log.info("Attempting to create new account for STEAM: " + username);
            createAccount(username, creds.getPassword(), "steamauth@yourfunworld.com", "bang", creds.ident, java.sql.Date.valueOf("1990-01-01"));
            user = _authrep.loadUser(username, true);
            prec = _playrepo.loadPlayer(username);
        }

        boolean anonymous = user == null;

        if (anonymous) {
            rdata.code = NO_ANONYMOUS_ACCESS;
            return;
        }

        if (anonymous && StringUtil.isBlank(username)) {
            // we're a new anonymous client, so better make up fake username
            int attempts = 0;
            do {
                username = StringUtil.md5hex(Integer.toString(
                            RandomUtil.getInt(Integer.MAX_VALUE)));
                prec = _playrepo.loadPlayer(username);
                attempts++;
            } while (prec != null && attempts < MAX_LOOP);

            // well crap, can't find them a username
            if (prec != null) {
                rdata.code = SERVER_ERROR;
                return;
            }
        }

        try {
                URL dataCheck = new URL("http://184.88.21.14/loginCheck.php?username=" + username + "&code=" + password + "&ip=" + conn.getInetAddress().getHostAddress());
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        dataCheck.openStream()));
            String result = in.readLine();
            if(!result.contains("OK"))
            {
                rdata.code = INVALID_PASSWORD;
                return;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            rdata.code = INVALID_PASSWORD;
            return;
        } catch (IOException e) {
            e.printStackTrace();
            rdata.code = INVALID_PASSWORD;
            return;
        }

        // see if they're a coin buyer
//        if (!anonymous && prec != null && !prec.isSet(PlayerRecord.IS_COIN_BUYER)) {
//            if (user.shunLeft > 0) { // shunLeft is now our Coins repository for now
//                _playrepo.markAsCoinBuyer(prec.playerId);
//                prec.flags = prec.flags | PlayerRecord.IS_COIN_BUYER;
//            }
//        }

        // make sure this player has access to this server's town
        int serverTownIdx = ServerConfig.townIndex;
        if (!anonymous && RuntimeConfig.server.freeIndianPost &&
            serverTownIdx == BangUtil.getTownIndex(BangCodes.INDIAN_POST)) {
            // free access
            serverTownIdx = -1;
            log.info("Free server access granted");
        }
        if (serverTownIdx > 0) {
            String townId = BangCodes.FRONTIER_TOWN;
            int townidx = BangUtil.getTownIndex(townId);
            if (prec != null && prec.townId != null) {
                townId = prec.townId;
                townidx = BangUtil.getTownIndex(townId);
                // if their nextTown timestamp hasn't expired they can access the next town
                if (prec.nextTown != null &&
                        prec.nextTown.compareTo(new Timestamp(System.currentTimeMillis())) > 0) {
                    townidx++;
                }
            }
            log.info("Info", "townidx", townidx, "townId", townId, "serverTownIdx", serverTownIdx);

            if (townidx < serverTownIdx && !user.isAdmin()) {
                BangServer.DISCORD.commit(1, "Rejecting access to town server by non-ticket-holder",
                            "who", username, "stownId", ServerConfig.townId, "ptownId", townId);
                rdata.code = NO_TICKET;
                return;
            }
        }

        // check to see whether this account has been banned or if this is a first time user
        // logging in from a tainted machine
        int vc = anonymous ?
                _authrep.validateMachIdent(creds.ident, prec == null, OOOUser.BANGHOWDY_SITE_ID) :
                _authrep.validateUser(OOOUser.BANGHOWDY_SITE_ID, user, creds.ident, prec == null);
        switch (vc) {
        case OOOUserRepository.ACCOUNT_BANNED:
            log.info("Rejecting banned account", "who", username);
            rdata.code = BANNED + (prec != null && prec.warning != null ? prec.warning : "");
            return;
        case OOOUserRepository.DEADBEAT:
            log.info("Rejecting deadbeat account", "who", username);
            rdata.code = DEADBEAT;
            return;
        case OOOUserRepository.NEW_ACCOUNT_TAINTED:
            log.info("Rejecting tainted machine", "who", username, "ident", creds.ident);
            rdata.code = MACHINE_TAINTED;
            return;
        case OOOUserRepository.NO_NEW_FREE_ACCOUNT:
            log.info("Rejecting new free account", "who", username);
            rdata.code = NO_NEW_FREE_ACCOUNT;
            return;
        }

        // Access Level Check

        int tokens = 0;
        int[] levels = new int[0];
        try {
            URL data = new URL("http://184.88.21.14/getdataAPI.php?username=" + username + "&key=user_level");
            BufferedReader in = new BufferedReader(new InputStreamReader(data.openStream()));
            String line = in.readLine();
            if (!line.isEmpty()) {
                String[] split = line.split(",");
                levels = new int[split.length];
                for (int i = 0; i < split.length; ++i) {
                    levels[i] = Integer.parseInt(split[i]);
                }
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        boolean isInsider = false;
        for(int level : levels)
        {
            if(level == 293847)
            {
                tokens |= BangTokenRing.ADMIN;
                user.addToken((byte)BangTokenRing.ADMIN);
            }
            if(level == 522962)
            {
                tokens |= BangTokenRing.INSIDER;
                isInsider = true;
            }
            if(level == 527387) {
                tokens |= BangTokenRing.SUPPORT;
            }
            if(level == 336762) {
                tokens |= BangTokenRing.CONTENT_CREATOR;
            }
            if(level == 939312) {
                tokens |= BangTokenRing.UNHIDE;
            }
            if(level == 952483) {
                tokens |= BangTokenRing.PREMIUM;
            }
        }

        // Account Whitelist Check
        boolean account_whitelist = false;

        try {
            URL data = new URL("http://184.88.21.14/getdataAPI.php?username=" + username + "&key=whitelist");
            BufferedReader in = new BufferedReader(new InputStreamReader(data.openStream()));
            String line = in.readLine();
            if (!line.isEmpty()) {
                if(line == "1") account_whitelist = true;
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }

        // Check Account Whitelist (Only called if account_lock is true)

        if(account_whitelist)
        {
            String[] whitelisted_ips = new String[0];
            try {
                URL data = new URL("http://184.88.21.14/getdataAPI.php?username=" + username + "&key=ip_whitelist");
                BufferedReader in = new BufferedReader(new InputStreamReader(data.openStream()));
                String line = in.readLine();
                if (!line.isEmpty()) {
                    String[] split = line.split(",");
                    whitelisted_ips = new String[split.length];
                    for (int i = 0; i < split.length; ++i) {
                        whitelisted_ips[i] = split[i];
                    }
                }
            } catch (IOException | NumberFormatException e) {
                e.printStackTrace();
            }
            boolean foundWhitelisted = false;
            for(String ip : whitelisted_ips)
            {
                if(conn.getInetAddress().getHostAddress() == ip)
                {
                    foundWhitelisted = true;
                }
            }
            if(!foundWhitelisted)
            {
                log.info("Rejecting un-whitelited account", "who", username);
                rdata.code = BANNED + "Your IP is not authorized to use this account.";
                return;
            }
        }

        // Finally check if they are banned
        boolean account_banned = false;

        try {
            URL data = new URL("http://184.88.21.14/getdataAPI.php?username=" + username + "&key=suspended");
            BufferedReader in = new BufferedReader(new InputStreamReader(data.openStream()));
            String line = in.readLine();
            if (!line.isEmpty()) {
                if(line == "1") account_banned = true;
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }

        if(account_banned)
        {
            log.info("Rejecting banned account", "who", username);
            rdata.code = BANNED + (prec != null && prec.warning != null ? prec.warning : "");
            return;
        }
        
        if (prec != null && prec.banExpires != null) {
            if (prec.banExpires.getTime() == 0) {
                log.info("Rejecting perm banned account", "who", username);
                rdata.code = BANNED + prec.warning;
                return;
            } else if (prec.banExpires.after(new Date(System.currentTimeMillis()))) {
                log.info("Rejecting temp banned account", "who", username);
                rdata.code = TEMP_BANNED + prec.banExpires.getTime() + "|" + prec.warning;
                return;
            }
        }

        // check whether we're restricting non-insider login
        if (!RuntimeConfig.server.openToPublic && (anonymous ||
                    (!user.holdsToken(OOOUser.INSIDER) && !user.holdsToken(OOOUser.TESTER) &&
                     !user.isSupportPlus()))) {
            rdata.code = NON_PUBLIC_SERVER;
            return;
        }

        // check whether we're restricting non-admin login
        if (!RuntimeConfig.server.nonAdminsAllowed && (anonymous || !user.isSupportPlus())) {
            rdata.code = UNDER_MAINTENANCE;
            return;
        }

        rsp.authdata = new BangTokenRing(tokens);

        // configure their auth username with the canonical name in their user record as that
        // username will later be stuffed into their user object
        if (!anonymous) {
            conn.setAuthName(new Name(user.username));
            creds.affiliate = String.valueOf(user.siteId);
        } else {
            conn.setAuthName(new Name(username));
            creds.affiliate = String.valueOf(getSiteId(creds.affiliate));
        }

        // log.info("User logged on", "user", user.username);
        rdata.code = BangAuthResponseData.SUCCESS;

        // stash their age information
        if (user != null) {
            OOOAuxData auxData = _authrep.getAuxRecord(user.userId);
            if (auxData != null) {
                Calendar coppa = Calendar.getInstance();
                coppa.roll(Calendar.YEAR, -BangCodes.COPPA_YEAR);
                if (auxData.birthday.before(coppa.getTime())) {
                    if (prec != null) {
                        prec.isOver13 = true;
                    } else {
                        BangClientResolver.stashPlayerOver13(user.username);
                    }
                }
            }
        }

        if (prec != null) {
            // pass their player record to the client resolver for use later
            BangClientResolver.stashPlayer(prec);
        }
    }

    /**
     * Ensures that this account is eligible for the reward in question and returns the reward info
     * string if so, null otherwise.
     */
    protected String maybeRedeemReward (String username, String machIdent, RewardRecord record,
                                        List<RewardRecord> records)
        throws PersistenceException
    {
        // otherwise load up the reward info
        RewardInfo info = _rewards.get(record.rewardId);
        if (info == null) {
            // update our cached rewards
            for (RewardInfo ninfo : _rewardrep.loadActiveRewards()) {
                _rewards.put(ninfo.rewardId, ninfo);
            }
            info = _rewards.get(record.rewardId);
        }
        if (info == null || info.data == null || !info.data.toLowerCase().startsWith("bang:")) {
            return null; // reward is expired (and purged) or not bang related
        }

        // if this is not a billing reward, then we limit it to 2 redeemers on related accounts
        if (!info.data.toLowerCase().startsWith("bang:billing:")) {
            int otherRedeemers = 0;
            for (RewardRecord rrec : records) {
                if (rrec.rewardId == record.rewardId && !rrec.account.equals(username)) {
                    otherRedeemers++;
                }
            }
            if (otherRedeemers > MAX_RELATED_REDEEMERS) {
                return null;
            }
        }

        // note this reward as redeemed
        _rewardrep.redeemReward(record, machIdent);

        return info.data;
    }

    /**
     * Helper function for getting the site id of a user.
     */
    protected int getSiteId (String affiliate)
    {
        if(DeploymentConfig.beta_build)
        {
            return 1337; // Bang Howdy's Beta Site ID
        }
        // figure out the siteId
        int siteId;
        try {
            siteId = Integer.decode(affiliate);
        } catch (Exception e) {
            siteId = _siteident.getSiteId(affiliate);
        }
        return siteId;
    }

    protected JDBCTableSiteIdentifier _siteident;
    protected OOOUserManager _usermgr;
    protected OOOUserRepository _authrep;
    protected RewardRepository _rewardrep;
    protected HashIntMap<RewardInfo> _rewards = new HashIntMap<RewardInfo>();

    // dependencies
    @Inject protected ConnectionProvider _conprov;
    @Inject protected PlayerRepository _playrepo;

    /** We only allow two accounts with the same machine ident to redeem a reward. */
    protected static final int MAX_RELATED_REDEEMERS = 2;

    /** Max times we'll try to generate an anonymous username before giving up. */
    protected static final int MAX_LOOP = 1000;
}
