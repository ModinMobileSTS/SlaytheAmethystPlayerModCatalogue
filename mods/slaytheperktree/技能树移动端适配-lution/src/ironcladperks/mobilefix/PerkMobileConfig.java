package ironcladperks.mobilefix;

import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Persisted switches for this patch.
 *
 * <p>Currently only one switch: whether the perks bought in the skill tree are allowed to
 * apply their bonuses. The value is stored in the ModTheSpire preferences directory so it
 * survives restarts.</p>
 */
public final class PerkMobileConfig {
    private static final Logger logger = LogManager.getLogger(PerkMobileConfig.class.getName());

    private static final String CONFIG_MOD_ID = "slaytheperktree";
    private static final String CONFIG_FILE = "perk_tree_mobile";
    private static final String KEY_BONUSES = "bonusesEnabled";

    private static SpireConfig config;
    private static Boolean bonusesEnabled;

    private PerkMobileConfig() {
    }

    private static SpireConfig config() {
        if (config == null) {
            try {
                Properties defaults = new Properties();
                defaults.setProperty(KEY_BONUSES, "true");
                config = new SpireConfig(CONFIG_MOD_ID, CONFIG_FILE, defaults);
            } catch (Exception e) {
                logger.warn("PerkTreeMobile: could not open the config file, using in-memory defaults", e);
                config = null;
            }
        }
        return config;
    }

    public static boolean bonusesEnabled() {
        if (bonusesEnabled == null) {
            boolean value = true;
            SpireConfig cfg = config();
            if (cfg != null) {
                try {
                    value = cfg.getBool(KEY_BONUSES);
                } catch (Exception e) {
                    value = true;
                }
            }
            bonusesEnabled = value;
        }
        return bonusesEnabled;
    }

    public static void setBonusesEnabled(boolean enabled) {
        bonusesEnabled = enabled;
        SpireConfig cfg = config();
        if (cfg == null) {
            return;
        }
        try {
            cfg.setBool(KEY_BONUSES, enabled);
            cfg.save();
        } catch (Exception e) {
            logger.warn("PerkTreeMobile: failed to save the bonus switch", e);
        }
    }

    public static void toggleBonuses() {
        setBonusesEnabled(!bonusesEnabled());
        logger.info("PerkTreeMobile: perk bonuses are now " + (bonusesEnabled() ? "ENABLED" : "PAUSED"));
    }
}
