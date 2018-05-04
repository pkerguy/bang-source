package com.threerings.bang.server;

import com.samskivert.util.StringUtil;
import com.threerings.bang.util.DeploymentConfig;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.EventListener;

import javax.security.auth.login.LoginException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DiscordAPIManager implements Runnable, EventListener {

    public static final int MONITORING = 0, MONITORING_DETAILED = 1;

    private final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();

    private Thread thread;
    private boolean running;

    void start() {
        if(DeploymentConfig.beta_build) return;
        if (thread == null) {
            running = true;
            (thread = new Thread(this)).setDaemon(true);
            thread.start();
        }
    }

    void stop() {
        running = false;
        thread.interrupt();
        thread = null;
    }

    public void commit(int channel, String message) {
        messageQueue.add(new Message(channel, message));
    }

    public void commit(int channel, Object message, Object... args)
    {
        if(DeploymentConfig.beta_build) return;
        Throwable err = null;
        int nn = args.length;
        if (message instanceof Throwable) {
            err = (Throwable)message;
            commit(1, err);
            return;
        } else if (nn % 2 == 1 && (args[nn - 1] instanceof Throwable)) {
            err = (Throwable)args[--nn];
            commit(1, err);
            return;
        }
        String msg = String.valueOf(message);
        if (nn > 0) {
            StringBuilder buf = new StringBuilder(msg);
            if (msg.length() > 0) {
                buf.append(' ');
            }
            buf.append('[');
            for (int ii = 0; ii < nn; ii += 2) {
                if (ii > 0) {
                    buf.append(',').append(' ');
                }
                buf.append(args[ii]).append('=');
                try {
                    buf.append(StringUtil.toString(args[ii + 1]));
                } catch (Throwable t) {
                    buf.append("<toString() failure: ").append(t).append(">");
                }
            }
            msg = buf.append(']').toString();
        }
        commit(1, msg);
    }

    @Override
    public void run() {
        if(DeploymentConfig.beta_build) return;
        JDABuilder builder = new JDABuilder(AccountType.BOT)
                .setToken("NDAwMDE2MTk3MTQ1NTkxODMx.DTrJyA.NthDxG7abOIahjqBWz5SA97LNZg")
                .addEventListener(this, new DiscordMessageListener());
        JDA jda = null;

        MessageChannel monitoring = null, monitoringDetailed = null;

        while (running) {
            Message message;
            try {
                message = messageQueue.take();
            } catch (InterruptedException ex) {
                continue;
            }

            if (jda == null) { // JDA now only connects when a message needs to be sent for the first time ^_^
                try {
                    jda = builder.buildBlocking();
                } catch (LoginException e) {
                    // TODO invalid token
                } catch (InterruptedException e) {
                    // Don't mind
                } catch (RateLimitedException e) {
                    // TODO rate limit reached
                }

                if (jda == null) {
                    continue;
                }
                if ((monitoring = jda.getTextChannelById(400344379316895744L)) == null) {
                    // TODO Couldn't find monitoring channel
                }
                if ((monitoringDetailed = jda.getTextChannelById(400344599958388736L)) == null) {
                    // TODO Couldn't find detailed channel
                }
            }

            // I know, it's a weird way of doing this stuff but I want to keep things efficient
            MessageChannel channel;
            switch (message.channel) {
                case MONITORING: channel = monitoring; break;
                case MONITORING_DETAILED: channel = monitoringDetailed; break;
                default:
                    // TODO Unknown channel
                    continue;
            }

            if (channel != null) {
                BangServer.discordLog("[" + channel.getName() + "] " + message.message); // No more chances of deleting the traces in Discord.
                channel.sendMessage(message.message).queue(msg -> System.out.printf("Sent Message %s\n", msg.getContentDisplay()));
            }
        }
        if (jda != null) {
            jda.shutdown();
        }
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof ReadyEvent) {
            System.out.println("Connected to Discord");
        }
    }

    private static class Message {

        final int channel;
        final String message;

        Message(int channel, String message) {
            this.channel = channel;
            this.message = message;
        }
    }
}
