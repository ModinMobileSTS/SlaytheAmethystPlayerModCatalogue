package com.navsysfix;

import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;

/**
 * Entry point: restores the saved settings, then lets the patches take over.
 * The config page itself is NavSysConfigPanel, opened from the game's mod list.
 */
@SpireInitializer
public class NavSysFixInit {

    public NavSysFixInit() {
        ConfigRuntime.load();
    }

    public static void initialize() {
        new NavSysFixInit();
    }
}
