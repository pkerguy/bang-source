//
// $Id$

package com.threerings.bang.client;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.threerings.bang.steam.SteamStorage;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
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

public class BangDesktop extends Application {
    private static class Option {
        String flag, opt;
        public Option(String flag, String opt) { this.flag = flag; this.opt = opt; }
    }
    @Override
    public void start(final Stage stage) {
        final File f = new File("rsrc/media/yourfunworld-intro.mp4");

        final Media m = new Media(f.toURI().toString());
        final MediaPlayer mp = new MediaPlayer(m);
        final MediaView mv = new MediaView(mp);

        final DoubleProperty width = mv.fitWidthProperty();
        final DoubleProperty height = mv.fitHeightProperty();

        width.bind(Bindings.selectDouble(mv.sceneProperty(), "width"));
        height.bind(Bindings.selectDouble(mv.sceneProperty(), "height"));

        mv.setPreserveRatio(true);

        StackPane root = new StackPane();
        root.getChildren().add(mv);
        root.setBackground(new Background(new BackgroundFill(Color.BLACK, null, null)));

        final Scene scene = new Scene(root, 960, 540);
        stage.setScene(scene);
        stage.setTitle("Bang! Howdy");
        stage.setFullScreen(true);
        stage.show();

        mp.setOnEndOfMedia(new Runnable() {
            @Override public void run() {
                stage.hide();
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
        });
        mp.setVolume(.1);
        mp.play();

    }

}

