//
// $Id$

package com.threerings.bang.bang.client;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.threerings.bang.bang.editor.EditorFrame;
import com.threerings.bang.client.BangApp;
import com.threerings.bang.client.BangPrefs;
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

    private static class Option {
        String flag, opt;
        public Option(String flag, String opt) { this.flag = flag; this.opt = opt; }
    }

    private static String OS = System.getProperty("os.name").toLowerCase();

    public static void main(String[] args) {
        List<Option> optsList = new ArrayList<Option>();
        List<String> doubleOptsList = new ArrayList<String>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i].charAt(0)) {
                case '-':
                    if (args[i].length() < 2)
                        throw new IllegalArgumentException("Not a valid argument: "+args[i]);
                    if (args[i].charAt(1) == '-') {
                        if (args[i].length() < 3)
                            throw new IllegalArgumentException("Not a valid argument: "+args[i]);
                        // --opt
                        doubleOptsList.add(args[i].substring(2, args[i].length()));
                    } else {
                        if (args.length-1 == i)
                            throw new IllegalArgumentException("Expected arg after: "+args[i]);
                        // -opt
                        optsList.add(new Option(args[i], args[i+1]));
                        i++;
                    }
                    break;
            }
        }
        for(Option options : optsList)
        {
            if(options.flag.equalsIgnoreCase("-editor"))
            {
                new EditorFrame();
                return;a
        }
        if(options.flag.equalsIgnoreCse("-username"))
            {
                username = options.opt;
            }
            if(options.flag.equalsIgnoreCase("-server"))
            {
                server = options.opt;
            }
            if(options.flag.equalsIgnoreCase("-password"))
            {
                password = options.opt;
            }
        }
        if(username != "" && server != "" && password != "")
        {
            System.out.println("Running Bang! Howdy Steam");
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

}