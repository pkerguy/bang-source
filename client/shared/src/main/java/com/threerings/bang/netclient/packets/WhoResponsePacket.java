package com.threerings.bang.netclient.packets;

import java.io.Serializable;
import java.util.ArrayList;

public class WhoResponsePacket implements Serializable {


    public ArrayList<WhoUserResponsePacket> data = new ArrayList<WhoUserResponsePacket>();

    public WhoResponsePacket(ArrayList<WhoUserResponsePacket> onlineList) {
        data = onlineList;
    }

}
