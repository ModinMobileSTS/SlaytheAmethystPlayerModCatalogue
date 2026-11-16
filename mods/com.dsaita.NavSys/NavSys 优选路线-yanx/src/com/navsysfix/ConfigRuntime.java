package com.navsysfix;

import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;

import java.util.Properties;

/**
 * Loads the saved settings into the fields of NavSysConfigPanel at boot.
 *
 * The config page (EasyConfigPanel) only pushes values into those fields when it
 * is built, which happens when the player opens it. Reading the same SpireConfig
 * file here means a run started without opening the page still uses the saved
 * colours and switches.
 */
public final class ConfigRuntime {

    private static final String CONFIG_NAME = "config";

    private ConfigRuntime() {
    }

    public static void load() {
        try {
            Properties defaults = new Properties();
            defaults.setProperty("showPanel", String.valueOf(NavSysConfigPanel.showPanel));
            defaults.setProperty("showSafeRoute", String.valueOf(NavSysConfigPanel.showSafeRoute));
            defaults.setProperty("showAggroRoute", String.valueOf(NavSysConfigPanel.showAggroRoute));
            defaults.setProperty("markMode", String.valueOf(NavSysConfigPanel.markMode));
            defaults.setProperty("tintPercent", String.valueOf(NavSysConfigPanel.tintPercent));
            defaults.setProperty("safeR", String.valueOf(NavSysConfigPanel.safeR));
            defaults.setProperty("safeG", String.valueOf(NavSysConfigPanel.safeG));
            defaults.setProperty("safeB", String.valueOf(NavSysConfigPanel.safeB));
            defaults.setProperty("aggroR", String.valueOf(NavSysConfigPanel.aggroR));
            defaults.setProperty("aggroG", String.valueOf(NavSysConfigPanel.aggroG));
            defaults.setProperty("aggroB", String.valueOf(NavSysConfigPanel.aggroB));

            SpireConfig config = new SpireConfig(NavSysConfigPanel.MOD_ID, CONFIG_NAME, defaults);

            NavSysConfigPanel.showPanel = config.getBool("showPanel");
            NavSysConfigPanel.showSafeRoute = config.getBool("showSafeRoute");
            NavSysConfigPanel.showAggroRoute = config.getBool("showAggroRoute");
            NavSysConfigPanel.markMode = config.getBool("markMode");
            NavSysConfigPanel.tintPercent = clampPercent(config.getInt("tintPercent"));
            NavSysConfigPanel.safeR = clamp(config.getInt("safeR"));
            NavSysConfigPanel.safeG = clamp(config.getInt("safeG"));
            NavSysConfigPanel.safeB = clamp(config.getInt("safeB"));
            NavSysConfigPanel.aggroR = clamp(config.getInt("aggroR"));
            NavSysConfigPanel.aggroG = clamp(config.getInt("aggroG"));
            NavSysConfigPanel.aggroB = clamp(config.getInt("aggroB"));
        } catch (Throwable ignored) {
            // A broken or unreadable config must never block mod loading;
            // the field defaults keep the patch usable.
        }
        RoutePainter.invalidate();
    }

    private static int clamp(int value) {
        if (value < 0) {
            return 0;
        }
        return Math.min(value, 255);
    }

    private static int clampPercent(int value) {
        if (value < 0) {
            return 0;
        }
        return Math.min(value, 100);
    }
}
