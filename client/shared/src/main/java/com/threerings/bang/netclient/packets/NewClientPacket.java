package com.threerings.bang.netclient.packets;

import com.threerings.bang.data.Handle;

import java.io.Serializable;

public class NewClientPacket implements Serializable {

    public String username;

    public NewClientPacket(String username) {
        this.username = username;
    }

}
