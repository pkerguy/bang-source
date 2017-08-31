//
// $Id$

package com.threerings.bang.client;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
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

public class BangDesktop extends Application {

    private static class Option {
        String flag, opt;
        public Option(String flag, String opt) { this.flag = flag; this.opt = opt; }
    }

    private static String OS = System.getProperty("os.name").toLowerCase();

    public static void main(String[] args) {
        launch(BangDesktop.class, args);
    }

    public static boolean isWindows() {
        return (OS.indexOf("win") >= 0);
    }

    public static boolean isMac() {
        return (OS.indexOf("mac") >= 0);
    }

    public static boolean isUnix() {
        return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0 );
    }

    public static boolean isSolaris() {
        return (OS.indexOf("sunos") >= 0);
    }

    public static String getOS(){
        if (isWindows()) {
            return "win";
        } else if (isMac()) {
            return "osx";
        } else if (isUnix()) {
            return "uni";
        } else if (isSolaris()) {
            return "sol";
        } else {
            return "err";
        }
    }

    @Override
    public void start(final Stage stage) {
        if (isWindows()) {
            System.out.println("This is Windows");
        } else if (isMac()) {
            System.out.println("This is Mac");
            System.out.println("Your OS is not supported yet!");
            System.exit(0);
        } else if (isUnix()) {
            System.out.println("This is Unix or Linux");
            startGame();
            return;
        } else if (isSolaris()) {
            System.out.println("This is Solaris");
        } else {
            System.out.println("Your OS is not supported!!");
            System.exit(0);
        }

        final File f = new File("rsrc/media/yourfunworld-intro.mp4");

        final Media m = new Media(f.toURI().toString());
        final MediaPlayer mp = new MediaPlayer(m);
        final MediaView mv = new MediaView(mp);

        mv.fitWidthProperty().bind(Bindings.selectDouble(mv.sceneProperty(), "width"));
        mv.fitHeightProperty().bind(Bindings.selectDouble(mv.sceneProperty(), "height"));

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
                startGame();
            }
        });
        mp.setVolume(.1);
        mp.play();

    }

    private void startGame() {
        System.out.println("Running Bang! Howdy Steam");
        SteamStorage.init();
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

