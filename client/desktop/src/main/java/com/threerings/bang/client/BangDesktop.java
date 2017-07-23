//
// $Id$

package com.threerings.bang.client;

import com.badlogic.gdx.backends.lwjgl.*;
import com.threerings.bang.steam.*;

public class BangDesktop
{
    public static void main (String[] args) {
        System.out.println("Running Bang! Howdy Steam");
        SteamStorage.init();
        System.out.println("Your Steam ID is: " + SteamStorage.user.getSteamID().toString());
        LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
        cfg.title = "Bang! Howdy";
        cfg.width = BangPrefs.getDisplayWidth();
        cfg.height = BangPrefs.getDisplayHeight();
        cfg.depth = BangPrefs.getDisplayBPP();
        cfg.fullscreen = BangPrefs.isFullscreenSet();
        cfg.resizable = false; // This glitches the game when resized if not set.
        // TODO: cfg.setFromDisplayMode when in fullscreen mode
        new LwjglApplication(new BangApp(), cfg);
    }
}
