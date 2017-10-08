package com.threerings.bang.server;

import com.jme.util.ShaderAttribute;
import com.jmr.wrapper.common.Connection;
import com.jmr.wrapper.common.listener.SocketListener;
import com.threerings.bang.netclient.packets.NewClientPacket;

import java.util.*;

public class Server implements SocketListener {

    @Override
    public void received(Connection connection, Object o) {
        if(o instanceof NewClientPacket)
        {
            NewClientPacket packet = (NewClientPacket)o;
            System.out.println("Charlie registered user: " + packet.username);
            BangServer.clients.put(packet.username, connection);
        }
    }

    @Override
    public void connected(Connection connection) {

    }


    @Override
    public void disconnected(Connection connection) {
        List<String> toRemove = new ArrayList<String>();
        if(BangServer.clients.containsValue(connection))
        {
            for(Map.Entry<String, Connection> connected : BangServer.clients.entrySet())
            {
                if(connected.getValue().equals(connection))
                {
                    System.out.println("Charlie will be removing Charlie Object: " + connected.getKey());
                    toRemove.add(connected.getKey());
                    break;
                }
            }
            for(String removed : toRemove)
            {
                BangServer.clients.remove(removed);
            }
        }
    }
}
