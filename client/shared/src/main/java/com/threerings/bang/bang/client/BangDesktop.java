//
// $Id$

package com.threerings.bang.bang.client;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.threerings.bang.bang.editor.EditorFrame;
import com.threerings.bang.client.BangApp;
import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.minigames.webapi.Server;
import com.threerings.bang.steam.SteamStorage;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BangDesktop {

    public static String username = "";
    public static String password = "";
    public static String server = "";

    public static boolean isSudoAllowed = true;
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
        System.out.println("Runnang! Howdy Steam");
        SteamStorage.init();
        System.out.println("Your Steam ID is: " + SteamStorage.user.getSteamID().toString());
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
        cfg.resizable = false; // This glitches the game when resized if not set.
        // TODO: cfg.setFromDisplayMode when in fullscreen mode
        new LwjglApplication(new BangApp(), cfg);
    }

}