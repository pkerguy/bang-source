package com.threerings.bang.netclient.packets;

import com.threerings.bang.client.BangClient;

import java.io.Serializable;
import java.net.URL;

public class ShowConfigPacket implements Serializable {

    public String username;

    public ShowConfigPacket(String username) {
        this.username = username;
    }

}
