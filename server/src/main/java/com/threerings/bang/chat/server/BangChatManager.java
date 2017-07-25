//
// $Id$

package com.threerings.bang.chat.server;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.servlet.user.User;
import com.samskivert.util.Lifecycle;
import com.samskivert.util.StringUtil;

import com.threerings.bang.admin.server.BangAdminManager;
import com.threerings.bang.data.BangTokenRing;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.persist.PlayerRecord;
import com.threerings.user.OOOUser;
import com.threerings.user.OOOUserManager;
import com.threerings.user.OOOUserRepository;
import com.threerings.util.MessageBundle;
import com.threerings.presents.data.ClientObject;
import com.threerings.crowd.chat.server.SpeakUtil;


import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.gang.server.persist.GangRepository;
import com.threerings.bang.server.PlayerLocator;
import com.threerings.bang.server.ServerConfig;
import com.threerings.bang.server.persist.PlayerRepository;

import static com.threerings.bang.Log.log;

/**
 * Handles Bang chat stuffs.
 */
@Singleton
public class BangChatManager
    implements Lifecycle.InitComponent
{
    @Inject public BangChatManager (Lifecycle cycle, PlayerLocator locator)
    {
        cycle.addComponent(this);

        locator.addPlayerObserver(new PlayerLocator.PlayerObserver() {
            public void playerLoggedOn (PlayerObject user) {
                if (user.hasCharacter()) {
                    addWhitelistWords(user.handle.toString());
                }
            }
            public void playerLoggedOff (PlayerObject user) {
                // noop!
            }
            public void playerChangedHandle (PlayerObject user, Handle oldHandle) {
                if (user.hasCharacter()) {
                    addWhitelistWords(user.handle.toString());
                }
            }
        });
    }

    // from Lifecycle.InitComponent
    public void init ()
    {
        // read in our whitelist dictionaries (if any)
        for (String dict : ServerConfig.config.getValue("whitelist_dicts", new String[0])) {
            try {
                InputStream din = getClass().getClassLoader().getResourceAsStream(dict);
                if (din == null) {
                    log.warning("Missing whitelist dictionary", "path", dict);
                    continue;
                }
                BufferedReader bin = new BufferedReader(new InputStreamReader(din));
                String word;
                while ((word = bin.readLine()) != null) {
                    _whitelist.add(word);
                }
            } catch (Exception e) {
                log.warning("Failed to read whitelist dictionary", "dict", dict, e);
            }
        }

        // if we don't have a minimum number of words, our dictionary loading failed, so don't
        // enable the whitelist
        if (!whitelistEnabled()) {
            log.info("Unable to sufficient whitelist data. Disabling chat whitelist.");
            _whitelist.clear();
            return;
        }

        // load in the names of every player and gang in the database and add those to the whitelist
        try {
            addWhitelistWords(_playrepo.loadNameWords());
            addWhitelistWords(_gangrepo.loadNameWords());
        } catch (PersistenceException pe) {
            log.warning("Failed to load name words for whitelist.", pe);
        }

        log.info("Chat system using whitelist", "size", _whitelist.size());
    }

    /**
     * Adds words from the specified name to the chat whitelist.
     */
    public void addWhitelistWords (String name)
    {
        if (whitelistEnabled()) {
            Set<String> words = Sets.newHashSet();
            for (String word : name.split("\\s")) {
                words.add(word);
            }
            addWhitelistWords(words);
        }
    }

    /**
     * Adds words to the chat whitelist.
     */
    public void addWhitelistWords (Set<String> words)
    {
        if (whitelistEnabled()) {
            Iterables.addAll(_whitelist, Iterables.transform(
                                 Iterables.filter(words, VALID_NAME), DOWNCASE));
        }
    }

    /**
     * Returns true if we have any words in the whitelist.
     */
    public boolean whitelistEnabled ()
    {
        return _whitelist.size() > MIN_WHITELIST_SIZE;
    }

    /**
     * Checks the supplied message against the chat whitelist.
     *
     * @return true if the chat passes, false if it fails. In the event of failure a feedback
     * message will be sent to the client explaining the failure.
     */
    public boolean validateChat (ClientObject speaker, String message)
    {
        // I realize this is hacky but it's done like this for a reason
        boolean isSupport = false;
        boolean isAdmin = false;
        try {
            PlayerRecord pr = _playrepo.loadPlayer(speaker.username.getNormal());
            PlayerObject oooUser = BangServer.locator.lookupPlayer(pr.getHandle());
            if(oooUser.getTokens().isSupport()) isSupport = true;
            if(oooUser.getTokens().isAdmin()) isAdmin = true;
        } catch (PersistenceException e) {
        }
        if (message.startsWith("@")) {
            if(!isSupport) return false;
            if(!isAdmin) return false;
            String[] args = {};
            try { // Use a try catch so we can make absolutely sure args cannot throw an exception and bug the entire server
                args = message.split("|"); // Split arguments with the | rather than space so we can have spaces in arguments
            } catch (IndexOutOfBoundsException exception){}
            switch(message){
                case "@reboot":
                    if(!isAdmin){
                        SpeakUtil.sendInfo(
                                speaker, BangCodes.CHAT_MSGS,
                                "Insufficient privileges");
                        break;
                    }
                    if(args.length != 2)
                    {
                        SpeakUtil.sendInfo(
                                speaker, BangCodes.CHAT_MSGS,
                                "Usage: @reboot|Minutes until reboot>");
                        break;
                    }
                    try {
                        _adminmgr.scheduleReboot(Integer.parseInt(args[1]), speaker.username.getNormal());
                        SpeakUtil.sendInfo(
                                speaker, BangCodes.CHAT_MSGS,
                                "Reboot has been scheduled!");
                        break;
                    } catch(NumberFormatException ex)
                    {
                        SpeakUtil.sendInfo(
                                speaker, BangCodes.CHAT_MSGS,
                                "You didn't supply a number");
                        break;
                    }
            }

            return false; // Make sure we don't send the command in chat
        }


        if (_whitelist.isEmpty()) {
            return true;
        }

        // hackasaur! do some english word sanitizing
        String norm = message.toLowerCase();
        norm = norm.replace("'s", "");
        norm = norm.replace("'ll", "");
        norm = norm.replace("'ve", "");
        norm = norm.replace("'d", "");
        norm = norm.replace("'re", "");
        norm = norm.replace("'m", "");
        norm = norm.replace("n't", "");

        // now strip out any remaining non-word characters and split up the rest
        List<String> invalid = null;
        for (String word : norm.replaceAll("[^\\w ]", "").split(" ")) {
            if (!_whitelist.contains(word)) {
                if (invalid == null) {
                    invalid = Lists.newArrayList();
                }
                invalid.add(word);
            }
        }
        if (invalid == null) {
            return true;
        }

        log.info("Nixing chat", "who", speaker.who(), "message", message, "invalid", invalid);

        SpeakUtil.sendInfo(
            speaker, BangCodes.CHAT_MSGS,
            MessageBundle.tcompose("e.not_in_whitelist", StringUtil.toString(invalid, "", "")));
        return false;
    }

    /** Our whitelist chat dictionary. */
    protected Set<String> _whitelist = Sets.newTreeSet();

    // dependencies
    @Inject protected PlayerRepository _playrepo;
    @Inject protected GangRepository _gangrepo;
    @Inject protected BangAdminManager _adminmgr;

    /** A predicate used to filter out short name words from the whitelist. */
    protected static final Predicate<String> VALID_NAME = new Predicate<String>() {
        public boolean apply (String name) {
            return name.length() > 2;
        }
    };

    /** A predicate used to filter out short name words from the whitelist. */
    protected static final Function<String, String> DOWNCASE = new Function<String, String>() {
        public String apply (String name) {
            return name.toLowerCase();
        }
    };

    protected static final int MIN_WHITELIST_SIZE = 50000;
}
