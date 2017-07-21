//
// $Id$

package com.threerings.bang.client;

import com.badlogic.gdx.backends.lwjgl.*;
import com.threerings.bang.steam.*;

public class BangDesktop
{
    // Probably shouldn't be using Message Boxes for this purpose, but let's try it out anyway.
    public static void main (String[] args) {
        SteamStorage.init();
        LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
        cfg.title = "Bang! Howdy";
        cfg.width = 1024;
        cfg.height = 768;
        cfg.resizable = false; // This glitches the game when resized if not set.
        // TODO: cfg.setFromDisplayMode when in fullscreen mode
        new LwjglApplication(new BangApp(), cfg);
    }
}
