package com.threerings.bang.netclient.listeners;

import com.jme.util.ShaderAttribute;
import com.jmr.wrapper.common.Connection;
import com.jmr.wrapper.common.listener.SocketListener;
import com.threerings.bang.netclient.packets.NewClientPacket;

import java.util.*;

public class Server implements SocketListener {

    public static HashMap<String, Connection> clients = new HashMap<String, Connection>();


    @Override
    public void received(Connection connection, Object o) {
        if(o instanceof NewClientPacket)
        {
            NewClientPacket packet = (NewClientPacket)o;
            clients.put(packet.username, connection);
        }
    }

    @Override
    public void connected(Connection connection) {

    }


    @Override
    public void disconnected(Connection connection) {
        List<String> toRemove = new ArrayList<String>();
        if(clients.containsValue(connection))
        {
            for(Map.Entry<String, Connection> connected : clients.entrySet())
            {
                if(connected.getValue().equals(connection))
                {
                    toRemove.add(connected.getKey());
                    break;
                }
            }
            for(String removed : toRemove)
            {
                clients.remove(removed);
            }
        }
    }
}
