package com.threerings.bang;

import com.threerings.bang.util.DeploymentConfig;
import com.yourfunworldstudios.nexusnet.BaseConfig;
import com.yourfunworldstudios.nexusnet.NexusServer;
import com.yourfunworldstudios.nexusnet.packets.PopupPacket;
import com.yourfunworldstudios.nexusnet.packets.handler.APIHandler;

import java.math.BigInteger;

public class TestServer {
    public static void main (String[] args) {
        if (DeploymentConfig.beta_build) {
            BaseConfig.debugMode = true;
        }
        NexusServer server = new NexusServer(new APIHandler() {
            @Override
            public PopupPacket checkKey(String s) {
                try {
                    Long l = Long.parseLong(s);
                    if (DeploymentConfig.getVersion() != l) {
                        return new PopupPacket("Outdated Version", "Bang! Howdy is currently out of date.. Please relaunch the game to update it.", true);
                    }
                    return null;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return new PopupPacket("Invalid Handler Packet", "Invalid startup packet specified... Try again!", true);
                }
            }
        });
        server.run("0.0.0.0", DeploymentConfig.getMasterPort(), DeploymentConfig.getMasterPort());
    }
}
