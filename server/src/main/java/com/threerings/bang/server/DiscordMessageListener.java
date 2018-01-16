package com.threerings.bang.server;

import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class DiscordMessageListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        if (event.isFromType(ChannelType.PRIVATE))
        {
            event.getPrivateChannel().close(); // Ignore Private messages
        }
        else
        {
            switch(event.getTextChannel().getName())
            {

            }
        }
    }
}
