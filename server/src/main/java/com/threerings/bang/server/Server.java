package com.threerings.bang.server;

import com.google.inject.Inject;
import com.jmr.wrapper.common.Connection;
import com.jmr.wrapper.common.listener.SocketListener;
import com.threerings.bang.admin.server.RuntimeConfig;
import com.threerings.bang.data.*;
import com.threerings.bang.netclient.packets.*;
import com.threerings.presents.peer.server.PeerNode;
import com.threerings.presents.server.PresentsSession;
import com.threerings.util.Name;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server implements SocketListener {

    @Override
    public void received(Connection connection, Object o) {
        if(o instanceof NewClientPacket)
        {
            NewClientPacket packet = (NewClientPacket)o;
            System.out.println("Charlie registered user: " + packet.username);
            PresentsSession client = BangServer.clmgr.getClient(new Name(packet.username));
            BangServer.clients.put(packet.username, connection);
        }
        if(o instanceof WhoSendPacket)
        {
                ArrayList<WhoUserResponsePacket> dataResponse = new ArrayList<WhoUserResponsePacket>();
                for(Map.Entry<String, Connection> s : BangServer.clients.entrySet()) {
                    PlayerObject user = (PlayerObject) BangServer.locator._clmgr.getClientObject(new Name(s.getKey()));
                    if(user == null) continue;
                    if (user.tokens.isSupport()) {
                        continue; // Don't show staff
                    }
                    if (user.hasCharacter()) {
                        WhoUserResponsePacket constructPacket = new WhoUserResponsePacket(null, user.getVisibleName().getNormal(), user.isActive(), user.townId, -1);
                        dataResponse.add(constructPacket);
                        continue; // Goto the next entry
                    } else {
                        continue; // Don't send any entries of people that don't have characters
                    }
                }
                connection.sendComplexObjectTcp(new WhoResponsePacket(dataResponse));
        }
        if(o instanceof WhoSendAdminPacket)
        {
            ArrayList<WhoUserResponsePacket> dataResponse = new ArrayList<WhoUserResponsePacket>();
            WhoSendAdminPacket adminPacket = (WhoSendAdminPacket)o;
            for(Map.Entry<String, Connection> s : BangServer.clients.entrySet()) {
                PlayerObject user = (PlayerObject) BangServer.locator._clmgr.getClientObject(new Name(s.getKey()));
                if(user == null) continue;
                if (user.hasCharacter()) {
                    WhoUserResponsePacket constructPacket = new WhoUserResponsePacket(user.username.getNormal(), user.getVisibleName().getNormal(), user.isActive(), user.townId, user.getPlaceOid());
                    dataResponse.add(constructPacket);
                    continue; // Goto the next entry
                } else {
                    WhoUserResponsePacket constructPacket = new WhoUserResponsePacket(user.username.getNormal(), user.username.getNormal(), user.isActive(), user.townId, user.getPlaceOid());
                    dataResponse.add(constructPacket);
                    continue; // Goto the next entry
                }
            }
            connection.sendComplexObjectTcp(new WhoResponsePacket(dataResponse));
        }
        if(o instanceof AwayAdminPacket)
        {
            /*
            AwayAdminPacket packet = (AwayAdminPacket)o;
            if(BangServer.peerManager.isRunning())
                {
                    System.out.println(packet.username); // Debugging stuff
                    PeerNode peer = BangServer.peerManager.getPlayerPeer(packet.username);
                    PlayerObject player = (PlayerObject)peer.getClient().getClientObject();
                    player.startTransaction();
                    if(packet.away)
                    {
                        player.awayMessage = "Howdy, ah see ya wanna contact a sheriff or deputy. Ther dreadfully busy people, please contact em at support@yourfunworld.com";
                        player.setAwayMessage("Howdy, ah see ya wanna contact a sheriff or deputy. Ther dreadfully busy people, please contact em at support@yourfunworld.com");
                    } else {
                        player.awayMessage = null;
                        player.setAwayMessage(null);
                    }
                    player.commitTransaction();
                    peer.clientObjectDidChange(peer.getClient()); // Relay that the object was changed!
                } */
        }
    }

    @Override
    public void connected(Connection connection) {

    }


    @Override
    public void disconnected(Connection connection) {

    }


}
