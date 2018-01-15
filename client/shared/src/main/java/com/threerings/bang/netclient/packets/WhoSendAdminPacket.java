package com.threerings.bang.netclient.packets;

import java.io.Serializable;

public class WhoSendAdminPacket implements Serializable {

    public String adminuser;

    public WhoSendAdminPacket(String username) {
        adminuser = username;
    }

}
