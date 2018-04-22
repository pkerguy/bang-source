package com.threerings.bang.server;

import com.google.inject.Inject;
import com.jmr.wrapper.common.Connection;
import com.jmr.wrapper.common.listener.SocketListener;
import com.samskivert.util.Tuple;
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
            BangServer.clients.put(packet.username, packet);
            BangServer.clients.get(packet.username).setConnection(connection);
        }
        if(o instanceof WhoSendPacket)
        {
                ArrayList<WhoUserResponsePacket> dataResponse = new ArrayList<WhoUserResponsePacket>();
                for(Map.Entry<String, NewClientPacket> s : BangServer.clients.entrySet()) {
                    Tuple<BangClientInfo,Integer> remote = BangServer.peerManager.locateRemotePlayer(s.getValue().handle);
                    if(remote == null) continue;
                    if (remote.left.avatar != null) {
                        if(remote.right == ServerConfig.townIndex) // They are in this town
                        {
                            PlayerObject pobj = BangServer.locator.lookupPlayer(s.getValue().handle);
                            WhoUserResponsePacket constructPacket = new WhoUserResponsePacket(null, pobj.getVisibleName().getNormal(), true, pobj.townId, -1);
                            dataResponse.add(constructPacket);
                            continue; // Goto the next entry
                        } else { // They are not on this server.. Just return some basic info
                            WhoUserResponsePacket constructPacket = new WhoUserResponsePacket(null, remote.left.visibleName.getNormal(), true, BangCodes.TOWN_IDS[remote.right], -1);
                            dataResponse.add(constructPacket);
                            continue;
                        }
                    } else {
                        WhoUserResponsePacket constructPacket = new WhoUserResponsePacket(null, remote.left.username.getNormal(), true, BangCodes.TOWN_IDS[remote.right], -1);
                        dataResponse.add(constructPacket);
                        continue;
                    }
                }
                connection.sendComplexObjectTcp(new WhoResponsePacket(dataResponse));
        }
        if(o instanceof WhoSendAdminPacket)
        {
            ArrayList<WhoUserResponsePacket> dataResponse = new ArrayList<WhoUserResponsePacket>();
            WhoSendAdminPacket adminPacket = (WhoSendAdminPacket)o;
            for(Map.Entry<String, NewClientPacket> s : BangServer.clients.entrySet()) {
                Tuple<BangClientInfo,Integer> remote = BangServer.peerManager.locateRemotePlayer(s.getValue().handle);
                if(remote == null) continue;
                if (remote.left.avatar != null) {
                    if(remote.right == ServerConfig.townIndex) // They are in this town
                    {
                        PlayerObject pobj = BangServer.locator.lookupPlayer(s.getValue().handle);
                        WhoUserResponsePacket constructPacket = new WhoUserResponsePacket(pobj.username.getNormal(), pobj.getVisibleName().getNormal(), true, pobj.townId, pobj.getPlaceOid());
                        dataResponse.add(constructPacket);
                        continue; // Goto the next entry
                    } else { // They are not on this server.. Just return some basic info
                        WhoUserResponsePacket constructPacket = new WhoUserResponsePacket(remote.left.username.getNormal(), remote.left.visibleName.getNormal(), true, BangCodes.TOWN_IDS[remote.right], -1);
                        dataResponse.add(constructPacket);
                        continue;
                    }
                } else {
                    WhoUserResponsePacket constructPacket = new WhoUserResponsePacket(remote.left.username.getNormal(), remote.left.username.getNormal(), true, BangCodes.TOWN_IDS[remote.right], -1);
                    dataResponse.add(constructPacket);
                    continue;
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
