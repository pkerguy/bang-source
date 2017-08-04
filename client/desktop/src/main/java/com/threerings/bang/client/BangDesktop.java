//
// $Id$

package com.threerings.bang.client;

import com.badlogic.gdx.backends.lwjgl.*;
import com.threerings.bang.editor.EditorFrame;
import com.threerings.bang.steam.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class BangDesktop
{
    private static class Option {
        String flag, opt;
        public Option(String flag, String opt) { this.flag = flag; this.opt = opt; }
    }
    public static void main (String[] args) {
        List<String> argsList = new ArrayList<String>();
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
                default:
                    // arg
                    argsList.add(args[i]);
                    break;
            }
        }
        if(doubleOptsList.contains("iamtheeditoroftheworld"))
        {
            new EditorFrame();
            return;
        }
        // etc
        System.out.println("Running Bang! Howdy Steam");
        SteamStorage.init();
        System.out.println("Your Steam ID is: " + SteamStorage.user.getSteamID().toString());
        LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
        cfg.title = "Bang! Howdy";
        cfg.width = BangPrefs.getDisplayWidth();
        cfg.height = BangPrefs.getDisplayHeight();
        cfg.depth = BangPrefs.getDisplayBPP();
        cfg.fullscreen = BangPrefs.isFullscreenSet();
        if(new File("safemode.txt").exists())
        {
            cfg.width = 800;
            cfg.height = 600;
            cfg.depth = BangPrefs.getDisplayBPP();
            cfg.fullscreen = false;
        }
        cfg.resizable = false; // This glitches the game when resized if not set.
        // TODO: cfg.setFromDisplayMode when in fullscreen mode
        new LwjglApplication(new BangApp(), cfg);
    }
}
