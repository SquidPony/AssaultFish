package com.squidpony.assaultfish.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.squidpony.assaultfish.AssaultFish;

/** Launches the desktop application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Assault Fish");
        configuration.setWindowedMode(80 * 18, (40 + 6) * 24);
        configuration.useVsync(true);
        new Lwjgl3Application(new AssaultFish(), configuration);
    }
}