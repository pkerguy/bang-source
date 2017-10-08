package com.threerings.bang.netclient.packets;

import java.io.Serializable;
import java.net.URL;

public class ShowURLPacket implements Serializable {

    public URL url;

    public ShowURLPacket(URL url) {
        this.url = url;
    }

}
