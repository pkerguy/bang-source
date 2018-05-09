package com.threerings.bang.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.io.PersistenceException;
import com.threerings.bang.admin.data.StatusObject;
import com.threerings.bang.admin.server.BangAdminManager;
import com.threerings.bang.admin.server.RuntimeConfig;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.persist.PlayerRepository;
import com.threerings.bang.util.DeploymentConfig;
import com.threerings.crowd.chat.server.ChatProvider;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.server.PresentsServer;
import com.threerings.presents.server.PresentsSession;
import com.threerings.presents.server.RebootManager;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.sql.Timestamp;
import java.time.Instant;

import static com.threerings.bang.server.BangServer.DISCORD;

public class DiscordMessageListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getChannel().getName().equalsIgnoreCase("banghowdy-control")) {
            Message message = event.getMessage();
            int serverPort = DeploymentConfig.getServerPorts(ServerConfig.townId)[0];
            if (message.mentionsEveryone() || message.getContentRaw().startsWith("!" + serverPort)) {
                String[] cmdRaw = message.getContentRaw().split(" ");
                switch (cmdRaw[1]) {
                    case "togglelogin": {
                        if (!event.getMember().hasPermission(Permission.MANAGE_CHANNEL)) {
                            DISCORD.commit(1, "Access denied.");
                            return;
                        }
                        if (RuntimeConfig.server.nonAdminsAllowed) {
                            RuntimeConfig.server.setNonAdminsAllowed(false);
                        } else {
                            RuntimeConfig.server.setNonAdminsAllowed(true);
                        }
                        DISCORD.commit(1, "Toggled LOGIN to: " + RuntimeConfig.server.nonAdminsAllowed);
                        break;
                    }
                    case "togglesaloon": {
                        if (!event.getMember().hasPermission(Permission.MANAGE_CHANNEL)) {
                            DISCORD.commit(1, "Access denied.");
                            return;
                        }
                        if (RuntimeConfig.server.saloonEnabled) {
                            RuntimeConfig.server.setSaloonEnabled(false);
                        } else {
                            RuntimeConfig.server.setSaloonEnabled(true);
                        }
                        DISCORD.commit(1, "Toggled SaloonS to: " + RuntimeConfig.server.saloonEnabled);
                        break;
                    }
                    case "togglegames": {
                        if (!event.getMember().hasPermission(Permission.MANAGE_CHANNEL)) {
                            DISCORD.commit(1, "Access denied.");
                            return;
                        }
                        if (RuntimeConfig.server.allowNewGames) {
                            RuntimeConfig.server.setAllowNewGames(false);
                        } else {
                            RuntimeConfig.server.setAllowNewGames(true);
                        }
                        DISCORD.commit(1, "Toggled Allow New Games to: " + RuntimeConfig.server.allowNewGames);
                        break;
                    }
                    case "shutdown": {
                        if (!event.getMember().hasPermission(Permission.MANAGE_CHANNEL)) {
                            DISCORD.commit(1, "Access denied.");
                            return;
                        }
                        DISCORD.commit(1, "SYSTEM ADMIN Authorized CONSOLE SHUTDOWN");
                        System.exit(0);
                        break;
                    }
                    case "reboot": {
                        if (!event.getMember().hasPermission(Permission.MANAGE_CHANNEL)) {
                            DISCORD.commit(1, "Access denied.");
                            return;
                        }
                        if (cmdRaw[2] == null) {
                            DISCORD.commit(1, "Please correct your usage.");
                            return;
                        }
                        try {
                            long time = Long.parseLong(cmdRaw[2]);
                            _rebmgr.scheduleReboot(time, event.getAuthor().getId());
                        } catch (Exception ex) {
                            DISCORD.commit(1, "Error with time specified!");
                        }
                    }
                    case "warn": {
                        PlayerObject player = BangServer.locator.lookupPlayer(new Handle(cmdRaw[2]));
                        try {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 3; i < cmdRaw.length; ++i) {
                                sb.append(cmdRaw[i]).append(' ');
                            }
                            _playrepo.setWarning(player.username.getNormal(), sb.toString());
                        } catch (PersistenceException e) {
                            e.printStackTrace();
                            return;
                        }
                        DISCORD.commit(1, "Successfully warned player!");
                    }
                    case "kwarn": {
                        PlayerObject player = BangServer.locator.lookupPlayer(new Handle(cmdRaw[2]));
                        try {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 3; i < cmdRaw.length; ++i) {
                                sb.append(cmdRaw[i]).append(' ');
                            }
                            _playrepo.setWarning(player.username.getNormal(), sb.toString());
                            PresentsSession pclient = BangServer.clmgr.getClient(player.username);
                            if (pclient != null) {
                                pclient.endSession();
                            }
                        } catch (PersistenceException e) {
                            e.printStackTrace();
                            return;
                        }
                        DISCORD.commit(1, "Successfully warned player!");
                    }
                    case "tempban": {
                        if (cmdRaw.length < 5) {
                            DISCORD.commit(1, "Please check your usage.");
                            return;
                        }
                        PlayerObject player = BangServer.locator.lookupPlayer(new Handle(cmdRaw[2]));
                        try {
                            long time = parseTimeSpec(cmdRaw[3], cmdRaw[4]);
                            StringBuilder sb = new StringBuilder();
                            for (int i = 5; i < cmdRaw.length; ++i) {
                                sb.append(cmdRaw[i]).append(' ');
                            }
                            _playrepo.setTempBan(player.username.getNormal(), Timestamp.from(Instant.ofEpochSecond(time)), sb.toString());
                            PresentsSession pclient = BangServer.clmgr.getClient(player.username);
                            if (pclient != null) {
                                pclient.endSession();
                            }
                        } catch (PersistenceException e) {
                            e.printStackTrace();
                            DISCORD.commit(1, "Something went wrong while trying to temporally ban that player.");
                            return;
                        }
                        DISCORD.commit(1, "Successfully warned player!");
                    }
                    case "ban": {
                        if (cmdRaw.length < 5) {
                            DISCORD.commit(1, "Please check your usage.");
                            return;
                        }
                        PlayerObject player = BangServer.locator.lookupPlayer(new Handle(cmdRaw[2]));
                        try {
                            long time = parseTimeSpec(cmdRaw[3], cmdRaw[4]);
                            StringBuilder sb = new StringBuilder();
                            for (int i = 5; i < cmdRaw.length; ++i) {
                                sb.append(cmdRaw[i]).append(' ');
                            }
                            _playrepo.setTempBan(player.username.getNormal(), new Timestamp(0), sb.toString());
                            PresentsSession pclient = BangServer.clmgr.getClient(player.username);
                            if (pclient != null) {
                                pclient.endSession();
                            }
                        } catch (PersistenceException e) {
                            e.printStackTrace();
                            DISCORD.commit(1, "Something went wrong while trying to temporally ban that player.");
                            return;
                        }
                        DISCORD.commit(1, "Successfully warned player!");
                    }
                }
            }
        }
    }
    @Singleton
    protected static class BangRebootManager extends RebootManager
    {
        @Inject public BangRebootManager (PresentsServer server, RootDObjectManager omgr) {
            super(server, omgr);
        }

        public void init (StatusObject statobj)
        {
            this.init();
            _statobj = statobj;
        }

        public void scheduleReboot (long rebootTime, String initiator) {
            super.scheduleReboot(rebootTime, initiator);
            _statobj.setServerRebootTime(rebootTime);
        }

        protected void broadcast (String message) {
            _chatprov.broadcast(null, BangCodes.BANG_MSGS, message, true, false);
        }

        protected int getDayFrequency () {
            return -1; // no automatically scheduled reboots for now
        }

        protected int getRebootHour () {
            return 8;
        }

        protected boolean getSkipWeekends () {
            return false;
        }

        protected String getCustomRebootMessage () {
            // for now we don't have auto-reboots, so let's not claim every hand scheduled reboot
            // is a "regularly scheduled reboot"
            return MessageBundle.taint("");
        }

        protected StatusObject _statobj;
        @Inject protected ChatProvider _chatprov;
    }

    public static long parseTimeSpec(String time, String unit)
            throws NumberFormatException {
        long sec = Integer.parseInt(time) * 60;
        if (unit.toLowerCase().startsWith(
                "h".substring(0, 1).toLowerCase())) {
            sec *= 60;
        } else if (unit.toLowerCase().startsWith(
                "d".substring(0, 1).toLowerCase())) {
            sec *= (60 * 24);
        } else if (unit.toLowerCase().startsWith(
                "w".substring(0, 1).toLowerCase())) {
            sec *= (7 * 60 * 24);
        } else if (unit.toLowerCase().startsWith(
                "mo".substring(0, 2).toLowerCase())) {
            sec *= (30 * 60 * 24);
        } else if (unit.toLowerCase().startsWith(
                "m".substring(0, 1).toLowerCase())) {
            sec *= 1;
        } else if (unit.toLowerCase().startsWith(
               "s".substring(0, 1).toLowerCase())) {
            sec /= 60;
        }
        return sec;
    }

    @Inject protected PlayerRepository _playrepo;
    @Inject protected BangRebootManager _rebmgr;



}
