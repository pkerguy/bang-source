package com.threerings.bang.netclient.packets;

import java.io.Serializable;

public class NewClientPacket implements Serializable {

    public String username;

    public NewClientPacket(String username) {
        this.username = username;
    }

}
