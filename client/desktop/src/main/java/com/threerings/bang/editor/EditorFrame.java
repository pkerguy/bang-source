//
// $Id$

package com.threerings.bang.editor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl.*;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.Guice;

import com.threerings.bang.client.BangApp;
import com.threerings.presents.data.InvocationMarshaller;

public class EditorFrame
{
    private EditorApp app;
    private LwjglApplicationConfiguration cfg;
    private JFrame frame;
    private LwjglCanvas canvas;
    LwjglApplicationConfiguration config;
    public EditorFrame() {
        cfg = new LwjglApplicationConfiguration();
        cfg.title = "Bang! Howdy Editor";
        cfg.width = 1024;
        cfg.height = 768;
        // cfg.resizble = false;
        // TODO: cfg.setFromDisplayMode when in fullscreen mode

        // configure our debug log
        BangApp.configureLog("editor.log");


        // create our editor server which we're going to run in the same JVM with the client
        Injector injector = Guice.createInjector(new EditorServer.Module());
        EditorServer server = injector.getInstance(EditorServer.class);
        try {
            server.init(injector);
        } catch (Exception e) {
            System.err.println("Unable to initialize server.");
            e.printStackTrace(System.err);
        }

        // let the BangClientController know we're in editor mode
        System.setProperty("editor", "true");

        // create a frame
        frame = new JFrame("Bang Editor");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // this is the entry point for all the "client-side" stuff
        app = injector.getInstance(EditorApp.class);
        app.frame = frame;
        app.frame.setSize(cfg.width, cfg.height);

        System.out.println("Starting the editor");
        canvas = new LwjglCanvas(app, cfg);
        canvas.getCanvas().setSize(cfg.width, cfg.height);
        app.canvas = canvas.getCanvas();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                Runtime.getRuntime().halt(0); // because of deadlocks with shut down
            }
        });

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                frame.setSize(cfg.width, cfg.height);
                frame.getContentPane().add(canvas.getCanvas());
                frame.setVisible(true);
                frame.pack();
                canvas.getCanvas().requestFocus();
            }
        });

//        canvas.postRunnable(new Runnable() {
//
//            @Override
//            public void run() {
//                app.frame.getContentPane().add(app.canvas);
//                frame.setVisible(true);
//                app.canvas.requestFocus();
//            }
//        });
//        frame.pack();
//        frame.setVisible(true);
//        canvas.executeRunnables();
    }
}