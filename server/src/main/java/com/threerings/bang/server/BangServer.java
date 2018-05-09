//
// $Id$

package com.threerings.bang.server;

import com.google.inject.*;
import com.jmr.wrapper.common.Connection;
import com.jmr.wrapper.server.Server;
import com.samskivert.depot.*;
import com.samskivert.io.PersistenceException;
import com.samskivert.util.*;
import com.threerings.admin.server.*;
import com.threerings.bang.admin.server.*;
import com.threerings.bang.avatar.data.*;
import com.threerings.bang.avatar.server.*;
import com.threerings.bang.avatar.util.*;
import com.threerings.bang.bang.client.BangDesktop;
import com.threerings.bang.bank.data.BankConfig;
import com.threerings.bang.bank.server.BankManager;
import com.threerings.bang.bounty.data.*;
import com.threerings.bang.bounty.server.*;
import com.threerings.bang.bounty.server.persist.*;
import com.threerings.bang.chat.server.*;
import com.threerings.bang.data.*;
import com.threerings.bang.gang.data.*;
import com.threerings.bang.gang.server.*;
import com.threerings.bang.netclient.packets.ClientStoragePacket;
import com.threerings.bang.netclient.packets.NewClientPacket;
import com.threerings.bang.ranch.data.*;
import com.threerings.bang.ranch.server.*;
import com.threerings.bang.saloon.data.*;
import com.threerings.bang.saloon.server.*;
import com.threerings.bang.server.persist.PlayerRepository;
import com.threerings.bang.station.data.*;
import com.threerings.bang.station.server.*;
import com.threerings.bang.store.data.*;
import com.threerings.bang.store.server.*;
import com.threerings.bang.tourney.server.*;
import com.threerings.bang.util.*;
import com.threerings.cast.bundle.*;
import com.threerings.crowd.chat.server.*;
import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.server.*;
import com.threerings.parlor.server.ParlorManager;
import com.threerings.presents.annotation.*;
import com.threerings.presents.data.*;
import com.threerings.presents.net.*;
import com.threerings.presents.peer.server.*;
import com.threerings.presents.server.*;
import com.threerings.presents.server.net.*;
import com.threerings.resource.*;
import com.threerings.user.depot.*;
import com.threerings.util.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.threerings.bang.Log.*;

/**
 * Creates and manages all the services needed on the bang server.
 */
public class BangServer extends CrowdServer
{

    public static boolean isBoardServer = false;

    public static class CommandHandler implements Runnable {

        @Override
        public void run()
        {
            Scanner scanner = new Scanner(System.in);

            while(true)
            {
                String command = scanner.nextLine();
                switch(command.split(" ")[0].toLowerCase())
                {
                    case "boards": {
                        isBoardServer = true;
                        RuntimeConfig.server.setSaloonEnabled(true);
                        RuntimeConfig.server.setBankEnabled(true);
                        RuntimeConfig.server.setFreeIndianPost(true);
                        RuntimeConfig.server.setHideoutEnabled(false);
                        RuntimeConfig.server.setBarberEnabled(true);
                        RuntimeConfig.server.setRanchEnabled(false);
                        RuntimeConfig.server.setOfficeEnabled(false);
                        RuntimeConfig.server.setStoreEnabled(false);
                        RuntimeConfig.server.setNonAdminsAllowed(true);
                        DISCORD.commit(1, "Set server as a board server");
                        break;
                    }
                    case "togglebank": {
                        if(RuntimeConfig.server.bankEnabled)
                        {
                            RuntimeConfig.server.setBankEnabled(false);
                        } else {
                            RuntimeConfig.server.setBankEnabled(true);
                        }
                        DISCORD.commit(1, "Toggled BANK to: " + RuntimeConfig.server.bankEnabled);
                        break;
                    }
                    case "togglelogin": {
                        if(RuntimeConfig.server.nonAdminsAllowed)
                        {
                            RuntimeConfig.server.setNonAdminsAllowed(false);
                        } else {
                            RuntimeConfig.server.setNonAdminsAllowed(true);
                        }
                        DISCORD.commit(1, "Toggled LOGIN to: " + RuntimeConfig.server.nonAdminsAllowed);
                        break;
                    }
                    case "togglesaloon": {
                        if(RuntimeConfig.server.saloonEnabled)
                        {
                            RuntimeConfig.server.setSaloonEnabled(false);
                        } else {
                            RuntimeConfig.server.setSaloonEnabled(true);
                        }
                        DISCORD.commit(1, "Toggled SaloonS to: " + RuntimeConfig.server.saloonEnabled);
                        break;
                    }
                    case "togglegames": {
                        if(RuntimeConfig.server.allowNewGames)
                        {
                            RuntimeConfig.server.setAllowNewGames(false);
                        } else {
                            RuntimeConfig.server.setAllowNewGames(true);
                        }
                        DISCORD.commit(1, "Toggled Allow New Games to: " + RuntimeConfig.server.allowNewGames);
                        break;
                    }
//                    case "set_exchangefrom": {
//                        try {
//                            int newValue = Integer.parseInt(command.split(" ")[1]);
//                            DISCORD.commit(1, "Current value of exchange from scrip to gold is: " + RuntimeConfig.server.scripToGoldRate);
//                            RuntimeConfig.server.setExchangeTo(newValue);
//                            DISCORD.commit(1, "Set exchange from scrip to gold is now: " + RuntimeConfig.server.scripToGoldRate);
//                            break;
//                        } catch(NumberFormatException e)
//                        {
//                            e.printStackTrace();
//                        }
//                    }
//                    case "set_exchangeto": {
//                        try {
//                            int newValue = Integer.parseInt(command.split(" ")[1]);
//                            DISCORD.commit(1, "Current value of exchange gold to scrip is: " + RuntimeConfig.server.goldToScripRate);
//                            RuntimeConfig.server.setExchangeFrom(newValue);
//                            DISCORD.commit(1, "Set exchange from gold to scrip to: " + RuntimeConfig.server.goldToScripRate);
//                            break;
//                        } catch(NumberFormatException e)
//                        {
//                            e.printStackTrace();
//                        }
//                    }
                    case "reloadboards": {
                        // Create backups of previous data in-case the reload fails
                        final BoardManager.BoardMap[] backupBoard = BangServer.boardManager._byname;
                        final HashMap<String,BoardManager.BoardList[]> backupScenerio = BangServer.boardManager._byscenario;

                        // Lock people from making new games for now so we have no client errors
                        RuntimeConfig.server.setAllowNewGames(false);
                        // Clear the current boards data
                        BangServer.boardManager._byname = null;
                        BangServer.boardManager._byscenario.clear();
                        // Load boards again
                        try {
                            BangServer.boardManager.init();
                        } catch (PersistenceException e) {
                            e.printStackTrace();
                            BangServer.boardManager._byname = backupBoard;
                            BangServer.boardManager._byscenario = backupScenerio;
                            RuntimeConfig.server.setAllowNewGames(true);
                            DISCORD.commit(1, "Failed to reload boards as described in the stacktrace. Restored data before the reload!");
                            return;
                        }
                        // Allow making of games again.. We're all done!
                        RuntimeConfig.server.setAllowNewGames(true);
                        // Report the dead has been done!
                        DISCORD.commit(1, "Boards reloaded!");
                        break;
                    }
                    case "shutdown": {
                                DISCORD.commit(1,"SYSTEM ADMIN Authorized CONSOLE SHUTDOWN");
                                System.exit(0);
                        break;
                    }
                    default: break;
                }

            }
        }
    }
    /** Configures dependencies needed by the Bang server. */
    public static class Module extends CrowdServer.CrowdModule implements EventListener {
        @Override protected void configure () {
            super.configure();

            // we need a legacy samskivert JDBC connection provider for our ye olde JORA and Simple
            // repositories; I'm not too keen to rewrite this decade+ old code... blah.
            com.samskivert.jdbc.ConnectionProvider legconprov =
                new com.samskivert.jdbc.StaticConnectionProvider(ServerConfig.getJDBCConfig());
            bind(com.samskivert.jdbc.ConnectionProvider.class).toInstance(legconprov);

            ConnectionProvider conprov = new StaticConnectionProvider(ServerConfig.getJDBCConfig());
            bind(ConnectionProvider.class).toInstance(conprov);
            // depot dependencies (we will initialize this persistence context later when the
            // server is ready to do database operations; not initializing it now ensures that no
            // one sneaks any database manipulations into the dependency resolution phase)
            PersistenceContext pctx = new PersistenceContext();
            bind(PersistenceContext.class).toInstance(pctx);
            bind(PeerManager.class).to(BangPeerManager.class);
            bind(ReportManager.class).to(BangReportManager.class);
            bind(ChatProvider.class).to(BangChatProvider.class);
            bind(Authenticator.class).to(ServerConfig.getAuthenticator());
            bind(BodyLocator.class).to(PlayerLocator.class);
            bind(ConfigRegistry.class).to(BangConfigRegistry.class);
            // bang dependencies
            ResourceManager rsrcmgr = new ResourceManager("rsrc");
            AccountActionRepository aarepo = new AccountActionRepository(pctx);
            AvatarLogic alogic;
            try {
                rsrcmgr.initBundles(null, "config/resource/manager.properties", null);
                alogic = new AvatarLogic(rsrcmgr, new BundledComponentRepository(
                    rsrcmgr, null, AvatarCodes.AVATAR_RSRC_SET));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            bind(ResourceManager.class).toInstance(rsrcmgr);
            bind(AccountActionRepository.class).toInstance(aarepo);
            bind(AvatarLogic.class).toInstance(alogic);

            final int latestINIT = 1; // Change value whenever we forcefully to to reinitialize the DB based Config values (DO NOT DO, but it's there in-case)

            DISCORD.start();
        }

        @Override protected void bindInvokers() {
            // replace the presents invoker with a custom version
            bind(Invoker.class).annotatedWith(MainInvoker.class).to(BangInvoker.class);
            bind(PresentsInvoker.class).to(BangInvoker.class);
            bind(Invoker.class).annotatedWith(AuthInvoker.class).to(PresentsAuthInvoker.class);
        }
    }

    /** The connection provider used to obtain access to our JDBC databases. */
    public static ConnectionProvider conprov;

    /** Used to provide database access to our Depot repositories. */
    public static PersistenceContext perCtx;

    /** A reference to the authenticator in use by the server. */
    public static BangAuthenticator author;

    /** Manages global player related bits. */
    public static PlayerManager playmgr;

    /** Manages gangs. */
    public static GangManager gangmgr;

    /** Manages tournaments. */
    public static BangTourniesManager tournmgr;

    /** Manages rating bits. */
    public static RatingManager ratingmgr;

    /** Keeps an eye on the Ranch, a good man to have around. */
    public static RanchManager ranchmgr;

    /** Manages the Saloon and match-making. */
    public static SaloonManager saloonmgr;

    /** Manages the General Store and item purchase. */
    public static StoreManager storemgr;

    /** Manages the Barber and avatar customization. */
    public static BarberManager barbermgr;

    /** Manages the Train Station and inter-town travel. */
    public static StationManager stationmgr;

    /** Manages the Hideout and Gangs. */
    public static HideoutManager hideoutmgr;

    /** Manages the Sheriff's Office and Bounties. */
    public static OfficeManager officemgr;

    public static BankManager bankManager;

    public static PlayerRepository playerRepository;

    /** Manages tracking and discouraging of misbehaving players. */
    public static NaughtyPlayerManager npmgr = new NaughtyPlayerManager();

    /** Contains information about the whole town. */
    public static TownObject townobj;

    public static BoardManager boardManager;

    // legacy static Presents services; try not to use these
    public static Invoker invoker;
    public static PresentsConnectionManager conmgr;
    public static ClientManager clmgr;
    public static PresentsDObjectMgr omgr;
    public static InvocationManager invmgr;

    // legacy static Crowd services; try not to use these
    public static PlayerLocator locator;
    public static PlaceRegistry plreg;
    public static LocationManager locman;
    public static BangPeerManager peerManager;

    // Statics relating to Tournament data
    public static boolean isTournamentServer = false;
    public static int amountofPlayers = 0, parlorCount = 0;
    public static Map<BodyObject, Integer> round = new ConcurrentHashMap<>();
    public static String[] scenerioIds;

    //public static DiscordAPIManager DISCORD = new DiscordAPIManager();
    public static DiscordAPIManager DISCORD = new DiscordAPIManager();


    /**
     * Ensures that the calling thread is the distributed object event dispatch thread, throwing an
     * {@link IllegalStateException} if it is not.
     */
    public static void requireDObjThread ()
    {
        if (!omgr.isDispatchThread()) {
            String errmsg = "This method must be called on the distributed object thread.";
            throw new IllegalStateException(errmsg);
        }
    }

    /**
     * Ensures that the calling thread <em>is not</em> the distributed object event dispatch
     * thread, throwing an {@link IllegalStateException} if it is.
     */
    public static void refuseDObjThread ()
    {
        if (omgr.isDispatchThread()) {
            String errmsg = "This method must not be called on the distributed object thread.";
            throw new IllegalStateException(errmsg);
        }
    }



    /**
     * The main entry point for the Bang server.
     */
    public static void main (String[] args)
    {
        // // if we're on the dev server, up our long invoker warning to 3 seconds
        // if (ServerConfig.config.getValue("auto_restart", false)) {
        //     Invoker.setDefaultLongThreshold(3000L);
        // }

        Injector injector = Guice.createInjector(new Module());
        BangServer server = injector.getInstance(BangServer.class);
        try {
            if(ServerConfig.townIndex == 0)
            {
                int charleyPort = Integer.parseInt(System.getProperty("charley_port"));
                _netserver = new Server(charleyPort, charleyPort);
                _netserver.setListener(new com.threerings.bang.server.Server());
                if(!_netserver.isConnected())
                {
                    System.out.println( "Charlie failed to start!");
                    System.exit(255);
                }
            }
            server.init(injector);
            server.run();
            // annoyingly some background threads are hanging, so stick a fork in them for the time
            // being; when run() returns the dobj mgr and invoker thread will already have exited
            System.out.println("Server exiting...");
            System.exit(0);
        } catch (Exception e) {
            System.out.println("Server initialization failed.");
            e.printStackTrace();
            System.exit(255);
        }
    }

    @Override // documentation inherited
    public void init (final Injector injector)
        throws Exception
    {
        // create out database connection provider this must be done before calling super.init()
        conprov = _conprov;
        perCtx = _perCtx;

        // TODO: FIX THIS STEAM STUFF
        // SteamServer.init(); // We're using this instead of the Shared one (SteamStorage) because some things like System.exit and Messageboxes aren't wanted

        // make sure we have a valid payment type configured
        try {
            DeploymentConfig.getPaymentType();
        } catch (Exception e) {
            System.out.println("deployment.properties payment_type invalid: " + e.getMessage());
            System.exit(255);
        }

        String initConfig = System.getProperty("init");
        if(initConfig != null && initConfig.equalsIgnoreCase("tourny"))
        {
            log.info("THIS IS A TOURNAMENT SERVER");
            isTournamentServer = true;
        }
        String maxPlayers = System.getProperty("maxPlayers");
        if(maxPlayers != null)
        {
            try {
                amountofPlayers = Integer.parseInt(maxPlayers);
            } catch (NumberFormatException ex)
            {
                log.info("MaxPlayers didn't use a number.");
                queueShutdown();
                return;
            }
        }
        if(System.getProperty("scenerios") != null)
        {
            scenerioIds = System.getProperty("scenerios").split(",");
        }
        if(isTournamentServer && amountofPlayers == 0 && scenerioIds.length > 0)
        {
            log.info("Tournament mode is active and no player count or scenerioIds where defined.");
            queueShutdown();
            return;
        }

        // set up some legacy static references
        peerManager = _peermgr;
        invoker = _invoker;
        conmgr = _conmgr;
        clmgr = _clmgr;
        omgr = _omgr;
        invmgr = _invmgr;
        locator = _locator;
        plreg = _plreg;
        locman = _locman;

        // create and set up our configuration registry and admin service
        ConfigRegistry confreg = new DatabaseConfigRegistry(perCtx, invoker, ServerConfig.nodename);

        // initialize our depot repositories; running all of our schema and data migrations
        _perCtx.init("bangdb", _conprov, null);
        _perCtx.initializeRepositories(true);

        // create our various supporting managers
        playmgr = _playmgr;
        gangmgr = _gangmgr;
        boardManager = _boardmgr;
        tournmgr = injector.getInstance(BangTourniesManager.class);
        ratingmgr = injector.getInstance(RatingManager.class);


        // now initialize our runtime configuration
        RuntimeConfig.init(omgr, confreg);

        // do the base server initialization
        super.init(injector);

        // initialize our managers
        _boardmgr.init();
        _playmgr.init();
        _gangmgr.init();
        tournmgr.init();
        ratingmgr.init();
        _adminmgr.init();

        // start up our periodic server status reporting
        _repmgr.activatePeriodicReport(omgr);

        // create the town object and initialize the locator which will keep it up-to-date
        townobj = omgr.registerObject(new TownObject());
        _locator.init();

        // create our managers
        saloonmgr = (SaloonManager)plreg.createPlace(new SaloonConfig());
        storemgr = (StoreManager)plreg.createPlace(new StoreConfig());
        ranchmgr = (RanchManager)plreg.createPlace(new RanchConfig());
        barbermgr = (BarberManager)plreg.createPlace(new BarberConfig());
        stationmgr = (StationManager)plreg.createPlace(new StationConfig());
        hideoutmgr = (HideoutManager)plreg.createPlace(new HideoutConfig());
        officemgr = (OfficeManager)plreg.createPlace(new OfficeConfig());
        bankManager = (BankManager) plreg.createPlace(new BankConfig());
        playerRepository = _playrepo;

        // if we have a shared secret, assume we're running in a cluster
        String node = System.getProperty("node");
        System.out.println("Got here");
        if (node != null && ServerConfig.sharedSecret != null) {
            log.info("Running in cluster mode as node '" + ServerConfig.nodename + "'.");
            _peermgr.init(ServerConfig.nodename, ServerConfig.sharedSecret,
                          ServerConfig.hostname, ServerConfig.publicHostname, DeploymentConfig.getServerPorts(ServerConfig.townId)[0]);
        }

        // set up our authenticator
        author = (BangAuthenticator)_author;
        author.init();

        // configure the client manager to use the appropriate client class
        clmgr.setDefaultSessionFactory(new SessionFactory() {
            public Class<? extends PresentsSession> getSessionClass (AuthRequest areq) {
                return BangSession.class;
            }
            public Class<? extends ClientResolver> getClientResolverClass (Name username) {
                return BangClientResolver.class;
            }
        });

        // start up an interval that checks to see if our code has changed and auto-restarts the
        // server as soon as possible when it has
        if (ServerConfig.config.getValue("auto_restart", false)) {
            _codeModified = new File(ServerConfig.serverRoot, "dist/bang-code.jar").lastModified();
            new Interval(omgr) {
                public void expired () {
                    checkAutoRestart();
                }
            }.schedule(AUTO_RESTART_CHECK_INTERVAL, true);
        }
        log.info("Setting presents server to timeout immediately!");

        log.info("Done setting presents server timeout.");
        log.info("Bang server v" + DeploymentConfig.getVersion() + " initialized.");
        DISCORD.commit(DiscordAPIManager.MONITORING, "INIT for node complete: " + ServerConfig.nodename);
        Thread t = new Thread(new CommandHandler());
        t.start();
    }

    /**
     * Loads a message to the general audit log.
     */
    public static void generalLog (String message)
    {
        _glog.log(message);
    }

    /**
     * Loads a message to the item audit log.
     */
    public static void itemLog (String message)
    {
        _ilog.log(message);
    }

    /**
     * Loads a message to the client performance log.
     */
    public static void perfLog (String message)
    {
        _plog.log(message);
    }

    /**
     * Loads a message to the client discord log.
     */
    public static void discordLog (String message)
    {
        _discordlog.log(message);
    }


    /**
     * Creates an audit log with the specified name (which should includ the <code>.log</code>
     * suffix) in our server log directory.
     */
    public static AuditLogger createAuditLog (String logname)
    {
        // qualify our log file with the nodename to avoid collisions
        logname = logname + "_" + ServerConfig.nodename;
        return new AuditLogger(_logdir, logname + ".log");
    }

    @Override // documentation inherited
    protected int[] getListenPorts ()
    {
        return DeploymentConfig.getServerPorts(ServerConfig.townId);
    }

    @Override // from PresentsServer
    protected void invokerDidShutdown ()
    {
        super.invokerDidShutdown();

        // shutdown our persistence context
        perCtx.shutdown();

        // close our audit logs
        _glog.close();
        _ilog.close();
        _stlog.close();
        _plog.close();

        // shutdown discord API
        //DISCORD.stop();
    }

    protected void checkAutoRestart ()
    {
        long lastModified = new File(ServerConfig.serverRoot, "dist/bang-code.jar").lastModified();
        if (lastModified > _codeModified) {
            int players = 0;
            for (Iterator<ClientObject> iter = clmgr.enumerateClientObjects(); iter.hasNext(); ) {
                if (iter.next() instanceof PlayerObject) {
                    players++;
                }
            }
            if (players == 0) {
                _adminmgr.scheduleReboot(0, "codeUpdateAutoRestart");
            }
        }
    }

    /** Used to direct our server reports to an audit log file. */
    protected static class BangReportManager extends ReportManager
    {
        @Override protected void logReport (String report) {
            _stlog.log(report);
        }
    }

    @Singleton
    protected static class BangConfigRegistry extends DatabaseConfigRegistry
    {
        @Inject public BangConfigRegistry (PersistenceContext perCtx,
                                           @MainInvoker Invoker invoker) {
            super(perCtx, invoker, ServerConfig.nodename);
        }
    }

    protected long _codeModified;

    @Inject protected ConnectionProvider _conprov;
    @Inject protected PersistenceContext _perCtx;
    @Inject protected Authenticator _author;
    @Inject protected ParlorManager _parmgr;
    @Inject protected BodyManager _bodymgr;
    @Inject protected ResourceManager _rsrcmgr;

    @Inject protected PlayerLocator _locator;
    @Inject protected BangAdminManager _adminmgr;
    @Inject protected BoardManager _boardmgr;
    @Inject protected GangManager _gangmgr;
    @Inject protected PlayerManager _playmgr;
    @Inject protected BangPeerManager _peermgr;
    @Inject protected BangChatManager _chatmgr;
    @Inject protected BangReportManager _repmgr;
    @Inject protected PlayerRepository _playrepo;


    public static HashMap<String, ClientStoragePacket> clients = new HashMap<>();

    // need to inject this guy here as he's otherwise not referenced until the office manager is
    // created which is too late in our initialization for safe repository creation
    @Inject protected BountyRepository _bountyrepo;

    // reference needed to bring these managers into existence
    @Inject protected AccountActionManager _actionmgr;

    protected static File _logdir = new File(ServerConfig.serverRoot, "log");
    protected static AuditLogger _glog = createAuditLog("server");
    protected static AuditLogger _ilog = createAuditLog("item");
    protected static AuditLogger _stlog = createAuditLog("state");
    protected static AuditLogger _plog = createAuditLog("perf");
    protected static AuditLogger _discordlog = createAuditLog("discord");

    /** Check for modified code every 30 seconds. */
    protected static final long AUTO_RESTART_CHECK_INTERVAL = 30 * 1000L;

    public static Server _netserver;
}
