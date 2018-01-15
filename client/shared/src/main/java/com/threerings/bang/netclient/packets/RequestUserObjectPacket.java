package com.threerings.bang.netclient.packets;

import com.threerings.bang.data.Handle;

import java.io.Serializable;

public class RequestUserObjectPacket implements Serializable {

    public Handle handletoCheck;
    public int actionCode;

    public RequestUserObjectPacket(Handle handle, int action) {
        handletoCheck = handle;
        actionCode = action;
    }

}
