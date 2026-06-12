package com.oakstory.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.oakstory.OakStoryGame;

/** Launches the desktop (LWJGL3) version of OakStory, used for development. */
public class Lwjgl3Launcher {

    public static void main(String[] args) {
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("OakStory");
        // Portrait-ish window mirroring the Pixel 2 target while developing.
        config.setWindowedMode(480, 800);
        config.useVsync(true);
        config.setForegroundFPS(60);
        return new Lwjgl3Application(new OakStoryGame(), config);
    }
}
