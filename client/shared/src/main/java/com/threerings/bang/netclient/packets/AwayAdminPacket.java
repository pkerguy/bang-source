package com.threerings.bang.netclient.packets;

import com.threerings.bang.data.Handle;

import java.io.Serializable;

public class AwayAdminPacket implements Serializable {

    public Handle username;
    public boolean away;

    public AwayAdminPacket(Handle player, boolean awayStatus) {
        username = player;
        away = awayStatus;
    }

}
