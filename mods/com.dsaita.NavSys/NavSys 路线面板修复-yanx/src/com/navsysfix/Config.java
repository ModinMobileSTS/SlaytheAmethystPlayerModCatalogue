package com.navsysfix;

import com.badlogic.gdx.graphics.Color;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;

import java.util.Properties;

/**
 * Persisted settings, stored through ModTheSpire's SpireConfig so they survive
 * restarts. Everything the settings overlay edits lives here; the rest of the
 * patch only reads these fields.
 *
 * Defaults follow the requested behaviour: the left-side panel starts hidden,
 * and the two routes (gold = safe, red = aggressive) are drawn on the map.
 */
public final class Config {

    private static final String MOD_ID = "NavSysFix";
    private static final String FILE_NAME = "NavSysFixConfig";

    private static final String K_SHOW_PANEL = "showPanel";
    private static final String K_SHOW_SAFE = "showSafeRoute";
    private static final String K_SHOW_AGGRO = "showAggroRoute";
    private static final String K_SAFE_R = "safeR";
    private static final String K_SAFE_G = "safeG";
    private static final String K_SAFE_B = "safeB";
    private static final String K_AGGRO_R = "aggroR";
    private static final String K_AGGRO_G = "aggroG";
    private static final String K_AGGRO_B = "aggroB";

    /** Left-side NavSys panel; off by default so the map stays clean. */
    public static boolean showPanel = false;
    /** Gold route: minimises combat. */
    public static boolean showSafeRoute = true;
    /** Red route: prefers combat. */
    public static boolean showAggroRoute = true;

    public static int safeR = 255;
    public static int safeG = 215;
    public static int safeB = 0;
    public static int aggroR = 255;
    public static int aggroG = 0;
    public static int aggroB = 0;

    private static SpireConfig config;

    private Config() {
    }

    public static void load() {
        try {
            Properties defaults = new Properties();
            defaults.setProperty(K_SHOW_PANEL, "false");
            defaults.setProperty(K_SHOW_SAFE, "true");
            defaults.setProperty(K_SHOW_AGGRO, "true");
            defaults.setProperty(K_SAFE_R, "255");
            defaults.setProperty(K_SAFE_G, "215");
            defaults.setProperty(K_SAFE_B, "0");
            defaults.setProperty(K_AGGRO_R, "255");
            defaults.setProperty(K_AGGRO_G, "0");
            defaults.setProperty(K_AGGRO_B, "0");
            config = new SpireConfig(MOD_ID, FILE_NAME, defaults);

            showPanel = config.getBool(K_SHOW_PANEL);
            showSafeRoute = config.getBool(K_SHOW_SAFE);
            showAggroRoute = config.getBool(K_SHOW_AGGRO);
            safeR = clampChannel(config.getInt(K_SAFE_R));
            safeG = clampChannel(config.getInt(K_SAFE_G));
            safeB = clampChannel(config.getInt(K_SAFE_B));
            aggroR = clampChannel(config.getInt(K_AGGRO_R));
            aggroG = clampChannel(config.getInt(K_AGGRO_G));
            aggroB = clampChannel(config.getInt(K_AGGRO_B));
        } catch (Throwable t) {
            // Keep the in-memory defaults; a broken config must not block loading.
            config = null;
        }
    }

    public static void save() {
        if (config == null) {
            return;
        }
        try {
            config.setBool(K_SHOW_PANEL, showPanel);
            config.setBool(K_SHOW_SAFE, showSafeRoute);
            config.setBool(K_SHOW_AGGRO, showAggroRoute);
            config.setInt(K_SAFE_R, safeR);
            config.setInt(K_SAFE_G, safeG);
            config.setInt(K_SAFE_B, safeB);
            config.setInt(K_AGGRO_R, aggroR);
            config.setInt(K_AGGRO_G, aggroG);
            config.setInt(K_AGGRO_B, aggroB);
            config.save();
        } catch (Throwable ignored) {
        }
    }

    public static int clampChannel(int value) {
        if (value < 0) {
            return 0;
        }
        if (value > 255) {
            return 255;
        }
        return value;
    }

    /** Step a colour channel by 16, wrapping around so one button covers 0..255. */
    public static int stepChannel(int value, boolean up) {
        int next = value + (up ? 16 : -16);
        if (next > 255) {
            return 0;
        }
        if (next < 0) {
            return 240;
        }
        return next;
    }

    public static Color safeColor() {
        return new Color(safeR / 255.0f, safeG / 255.0f, safeB / 255.0f, 1.0f);
    }

    public static Color aggroColor() {
        return new Color(aggroR / 255.0f, aggroG / 255.0f, aggroB / 255.0f, 1.0f);
    }
}
