package neowpreview;

import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;

/**
 * 设置项：预览图片大小，以及「涅奥始终有四个选项」，写入 ModTheSpire 的配置文件。
 *
 * <p>100% 即原版大小，可调 50%-200%（默认 80%）。</p>
 */
public final class Config {

    private static final String MOD_ID = "neowpreview";
    private static final String FILE = "neowpreview_config";
    private static final String KEY_IMAGE = "imagePercent";
    private static final String KEY_VERSION = "configVersion";
    private static final String KEY_ALWAYS_FOUR = "alwaysFourOptions";
    /** 配置版本：2 起改用「100% = 原版大小」与新的默认值。 */
    private static final int VERSION = 2;

    public static final int MIN_PERCENT = 50;
    public static final int MAX_PERCENT = 200;
    public static final int DEFAULT_IMAGE = 80;
    public static final boolean DEFAULT_ALWAYS_FOUR = false;

    public static int imagePercent = DEFAULT_IMAGE;
    /** 涅奥始终给出四个选项（原版首次通关前只有两个）。 */
    public static boolean alwaysFourOptions = DEFAULT_ALWAYS_FOUR;

    private static SpireConfig config;
    private static boolean loaded = false;

    private Config() {
    }

    public static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            config = new SpireConfig(MOD_ID, FILE);
        } catch (Throwable t) {
            return;
        }
        // 开关是后加的，旧配置里没有这一项，读不到就用默认值
        alwaysFourOptions = readBool(KEY_ALWAYS_FOUR, DEFAULT_ALWAYS_FOUR);
        int version = 0;
        try {
            version = config.getInt(KEY_VERSION);
        } catch (Throwable ignored) {
        }
        if (version < VERSION) {
            // 旧版本存的是 20-150 的旧语义，直接回到新的默认值
            imagePercent = DEFAULT_IMAGE;
            return;
        }
        try {
            imagePercent = clamp(config.getInt(KEY_IMAGE), DEFAULT_IMAGE);
        } catch (Throwable ignored) {
        }
    }

    public static void save() {
        try {
            SpireConfig cfg = ensure();
            if (cfg == null) {
                return;
            }
            cfg.setInt(KEY_VERSION, VERSION);
            cfg.setInt(KEY_IMAGE, imagePercent);
            cfg.setBool(KEY_ALWAYS_FOUR, alwaysFourOptions);
            cfg.save();
        } catch (Throwable ignored) {
        }
    }

    public static void setImagePercent(int value) {
        imagePercent = clamp(value, DEFAULT_IMAGE);
        save();
    }

    /** 切换「涅奥始终有四个选项」，立刻生效（下一次涅奥界面就按新设置走）。 */
    public static void setAlwaysFour(boolean value) {
        alwaysFourOptions = value;
        save();
    }

    public static boolean alwaysFour() {
        load();
        return alwaysFourOptions;
    }

    /** 预览图片（卡面、遗物、药水）缩放系数，1.0 = 原版大小。 */
    public static float imageScale() {
        load();
        return imagePercent / 100.0f;
    }

    private static SpireConfig ensure() {
        if (config == null) {
            try {
                config = new SpireConfig(MOD_ID, FILE);
            } catch (Throwable t) {
                return null;
            }
        }
        return config;
    }

    private static boolean readBool(String key, boolean fallback) {
        try {
            return ensure() == null ? fallback : config.getBool(key);
        } catch (Throwable t) {
            return fallback;
        }
    }

    private static int clamp(int value, int fallback) {
        if (value <= 0) {
            return fallback;
        }
        return Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, value));
    }
}
