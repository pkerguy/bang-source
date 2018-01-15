package com.threerings.bang.netclient.packets;

import java.io.Serializable;

public class AwayAdminPacket implements Serializable {

    public String adminuser;
    public boolean away;

    public AwayAdminPacket(String username, boolean awayStatus) {
        adminuser = username;
        away = awayStatus;
    }

}
