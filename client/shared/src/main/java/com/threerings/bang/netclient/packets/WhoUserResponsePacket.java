package com.threerings.bang.netclient.packets;

import java.io.Serializable;

public class WhoUserResponsePacket implements Serializable {


    public String username;
    public String publicDisplay;
    public boolean isActive;
    public String townId;
    public int placeOid;

    public WhoUserResponsePacket(String user_name, String displayName, boolean isNotIdle, String townID, int place) {
        username = user_name;
        publicDisplay =  displayName;
        isActive = isNotIdle;
        townId = townID;
        placeOid = place;
    }

}
