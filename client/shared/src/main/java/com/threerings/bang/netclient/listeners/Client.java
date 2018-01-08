package com.threerings.bang.netclient.listeners;

import com.jmex.bui.BLabel;
import com.jmr.wrapper.common.Connection;
import com.jmr.wrapper.common.listener.SocketListener;
import com.threerings.bang.admin.client.RuntimeConfigView;
import com.threerings.bang.admin.data.ServerConfigObject;
import com.threerings.bang.client.BangUI;
import com.threerings.bang.netclient.packets.NewClientPacket;
import com.threerings.bang.netclient.packets.ShowURLPacket;
import com.threerings.bang.util.BangContext;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Client implements SocketListener {

    protected BangContext _ctx;

    public Client(BangContext ctx)
    {
        this._ctx = ctx;
    }

    @Override
    public void received(Connection connection, Object o) {
        if(o instanceof ShowURLPacket)
        {
            ShowURLPacket packet = (ShowURLPacket)o;
            _ctx.showURL(packet.url);
        }
        if(o instanceof ServerConfigObject)
        {
            ServerConfigObject confobj = (ServerConfigObject)o;
            new RuntimeConfigView(confobj, _ctx);
        }
    }

    @Override
    public void connected(Connection connection) {
        System.out.println("Connected to Charlie!");
    }

    @Override
    public void disconnected(Connection connection) {
        System.out.println("Disconnected from Charlie! Shutting down...");
        BangUI.shutdown();
    }
}