package com.threerings.bang.netclient.packets;

import com.threerings.bang.data.Handle;

import java.io.Serializable;

public class RequestUserObjectResponsePacket implements Serializable {

    public Handle handletoCheck;
    public int actionCode;

    public RequestUserObjectResponsePacket(Handle handle, int action) {
        handletoCheck = handle;
        actionCode = action;
    }

}
