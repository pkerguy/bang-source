package com.threerings.bang;

import com.threerings.bang.bang.client.BangDesktop;
import com.yourfunworld.loader.packets.shared.GameClient;
import com.yourfunworld.plugins.base.annotations.PluginImplementation;
import com.yourfunworld.plugins.base.annotations.events.Init;
import com.yourfunworld.plugins.base.annotations.events.Shutdown;

@PluginImplementation
public class LaunchClient implements GameClient {

    @Init
    public void init() {
            new BangDesktop().main(null); // Launch the game with this hacky method for now.
    }

    @Shutdown
    public void shutdown() {

    }
}
