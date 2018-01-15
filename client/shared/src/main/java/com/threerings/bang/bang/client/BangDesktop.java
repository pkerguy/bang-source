//
// $Id$

package com.threerings.bang.bang.client;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.threerings.bang.client.BangApp;
import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.steam.SteamStorage;

import java.io.File;

public class BangDesktop {

    public static String username = "";
    public static String password = "";
    public static String server = "";

    public static boolean isSudoAllowed = false;
    public static boolean isMobileApp = false;

    public static String sudoUser = "UNKNOWN";

    private static class Option {
        String flag, opt;
        public Option(String flag, String opt) { this.flag = flag; this.opt = opt; }
    }

    private static String OS = System.getProperty("os.name").toLowerCase();

    public static void main(String[] args) {
        if(isSudoAllowed)
        {
            if(args.length == 1) {
                sudoUser = args[0];
                System.out.println("WARNING: RUNNING IN SUDO USER MODE!");
            } else {
                isSudoAllowed = false;
                System.out.println("WARNING: SUDO USER MODE WAS ENABLED BY ULTIMATELY WAS DISABLED!");
            }
        }
        System.out.println("Running Bang! Howdy Steam");
        SteamStorage.init();
        System.out.println("Your Steam ID is: " + SteamStorage.user.getSteamID().toString());
        boolean windowedFullScreen = false;
        if(new File("windowed.txt").exists()) {
            windowedFullScreen = true;
        }
        LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
        cfg.title = "Bang! Howdy";
        cfg.depth = BangPrefs.getDisplayBPP();
        if(new File("safemode.txt").exists()) {
            cfg.width = 800;
            cfg.height = 600;
            cfg.fullscreen = false;
        } else {
            cfg.width = BangPrefs.getDisplayWidth();
            cfg.height = BangPrefs.getDisplayHeight();
            cfg.fullscreen = BangPrefs.isFullscreenSet();
        }
        if(windowedFullScreen)
        {
            System.setProperty("org.lwjgl.opengl.Window.undecorated", "true");
            cfg.width = LwjglApplicationConfiguration.getDesktopDisplayMode().width;
            cfg.height = LwjglApplicationConfiguration.getDesktopDisplayMode().height;
            cfg.fullscreen = false;
        }
        cfg.resizable = false; // This glitches the game when resized if not set.
        // TODO: cfg.setFromDisplayMode when in fullscreen mode
        new LwjglApplication(new BangApp(), cfg);
    }

}