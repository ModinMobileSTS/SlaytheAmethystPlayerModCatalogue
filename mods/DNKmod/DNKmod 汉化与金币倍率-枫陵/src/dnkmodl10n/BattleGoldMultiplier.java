package dnkmodl10n;

import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import java.util.Properties;

/**
 * Persistent setting for the battle gold multiplier (1x - 100x).
 * Stored in its own SpireConfig file so it never conflicts with DNKmod's own config.
 */
public final class BattleGoldMultiplier {

    public static final int MIN = 1;
    public static final int MAX = 100;
    public static final int DEFAULT = 1;

    private static final String CONFIG_MOD_ID = "DNKmodLocalization";
    private static final String CONFIG_FILE = "dnkmod-l10n-config";
    private static final String KEY = "BattleGoldMultiplier";

    private static SpireConfig config;
    private static int multiplier = DEFAULT;
    private static boolean loaded = false;

    private BattleGoldMultiplier() {
    }

    public static int get() {
        ensureLoaded();
        return multiplier;
    }

    public static void set(int value) {
        ensureLoaded();
        int clamped = Math.max(MIN, Math.min(MAX, value));
        if (clamped == multiplier) {
            return;
        }
        multiplier = clamped;
        try {
            if (config != null) {
                config.setInt(KEY, multiplier);
                config.save();
            }
        } catch (Throwable ignored) {
            // Losing the persisted value is not worth interrupting the game.
        }
    }

    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            Properties defaults = new Properties();
            defaults.setProperty(KEY, Integer.toString(DEFAULT));
            config = new SpireConfig(CONFIG_MOD_ID, CONFIG_FILE, defaults);
            config.load();
            int stored = config.has(KEY) ? config.getInt(KEY) : DEFAULT;
            multiplier = Math.max(MIN, Math.min(MAX, stored));
        } catch (Throwable ignored) {
            multiplier = DEFAULT;
        }
    }
}
