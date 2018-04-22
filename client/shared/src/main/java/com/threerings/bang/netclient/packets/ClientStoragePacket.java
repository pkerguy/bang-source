package com.threerings.bang.netclient.packets;

import com.jmr.wrapper.common.Connection;
import com.threerings.bang.data.Handle;

import java.io.Serializable;

public class ClientStoragePacket implements Serializable {

    public String username;
    public Connection connection;
    public Handle handle;

    public ClientStoragePacket(String username) {
        this.username = username;
    }

    public void setHandle(Handle handle) {
        this.handle = handle;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }
}
