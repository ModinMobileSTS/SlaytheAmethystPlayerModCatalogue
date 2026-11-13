package tkfix;

import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.megacrit.cardcrawl.core.Settings;
import java.util.Properties;
import timekeepermod.patch.SkinSelectScreen_green;
import timekeepermod.patch.SkinSelectScreen_red;

/**
 * 把原版角色（铁甲战士 / 静默猎手）的皮肤选择写进独立的配置文件，
 * 并在进入选人界面、读档时恢复。
 * 母模组三个界面共用同一个存档键 "TimeCutter:skin"，互相覆盖，
 * 所以这里用独立文件记录，作为皮肤选择的权威来源。
 */
public final class SkinFixConfig {
    public static final String KEY_IRONCLAD = "IroncladSkinIndex";
    public static final String KEY_SILENT = "SilentSkinIndex";

    private static final String MOD_ID = "TimeKeeperMod";
    private static final String FILE_NAME = "timekeeper_skin_fix";

    private static SpireConfig config;
    private static boolean restored;
    private static boolean armed;

    private SkinFixConfig() {
    }

    public static void restoreOnce() {
        Diag.log("env", "letterbox=" + Settings.isLetterbox + " sixteenByTen=" + Settings.isSixteenByTen
                + " fourByThree=" + Settings.isFourByThree + " scale=" + Settings.scale
                + " renderScale=" + Settings.renderScale + " isMobile=" + Settings.isMobile);
        if (restored) {
            armed = true;
            return;
        }
        restored = true;
        applyIronclad(savedIndex(true));
        applySilent(savedIndex(false));
        armed = true;
        Diag.log("restore", "done red=" + indexRed() + " green=" + indexGreen());
    }

    /** 读档后母模组的共用键会把序号改回存档里的值，这里用独立配置覆盖回去。 */
    public static void restoreAfterLoad(boolean ironclad) {
        try {
            int saved = savedIndex(ironclad);
            if (saved <= 0) {
                return;
            }
            int live = ironclad ? indexRed() : indexGreen();
            if (live == saved) {
                return;
            }
            if (ironclad) {
                applyIronclad(saved);
            } else {
                applySilent(saved);
            }
            Diag.log("restore", "afterLoad " + (ironclad ? "red=" + indexRed() : "green=" + indexGreen()));
        } catch (Throwable t) {
            Diag.error("restoreAfterLoad", t);
        }
    }

    public static void save(String key, int value) {
        if (!armed || value < 0) {
            return;
        }
        SpireConfig cfg = config();
        if (cfg == null) {
            return;
        }
        try {
            cfg.setInt(key, value);
            cfg.save();
            Diag.log("save", key + "=" + value);
        } catch (Exception e) {
            Diag.error("save", e);
        }
    }

    private static SpireConfig config() {
        if (config == null) {
            try {
                Properties defaults = new Properties();
                defaults.setProperty(KEY_IRONCLAD, "0");
                defaults.setProperty(KEY_SILENT, "0");
                config = new SpireConfig(MOD_ID, FILE_NAME, defaults);
            } catch (Exception e) {
                Diag.error("config", e);
                config = null;
            }
        }
        return config;
    }

    private static int readInt(String key) {
        SpireConfig cfg = config();
        if (cfg == null) {
            return 0;
        }
        try {
            String raw = cfg.getString(key);
            if (raw == null) {
                return 0;
            }
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            Diag.error("readInt", e);
            return 0;
        }
    }

    /** 保存在独立配置里的皮肤序号（0 = 默认）。 */
    public static int savedIndex(boolean ironclad) {
        try {
            return readInt(ironclad ? KEY_IRONCLAD : KEY_SILENT);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static int indexRed() {
        try {
            return SkinSelectScreen_red.Inst == null ? -1 : SkinSelectScreen_red.Inst.index;
        } catch (Throwable t) {
            return -2;
        }
    }

    public static int indexGreen() {
        try {
            return SkinSelectScreen_green.Inst == null ? -1 : SkinSelectScreen_green.Inst.index;
        } catch (Throwable t) {
            return -2;
        }
    }

    private static void applyIronclad(int index) {
        try {
            SkinSelectScreen_red screen = SkinSelectScreen_red.Inst;
            if (screen == null) {
                return;
            }
            int valid = 0;
            for (int i = Math.max(index, 0); i >= 0; --i) {
                try {
                    screen.index = i;
                    SkinSelectScreen_red.getSkin_red();
                    valid = i;
                    break;
                } catch (IndexOutOfBoundsException ignored) {
                    // 这个序号在当前版本不存在，继续往下试
                }
            }
            screen.index = valid;
            screen.refresh_red();
        } catch (Throwable t) {
            Diag.error("applyRed", t);
        }
    }

    private static void applySilent(int index) {
        try {
            SkinSelectScreen_green screen = SkinSelectScreen_green.Inst;
            if (screen == null) {
                return;
            }
            int valid = 0;
            for (int i = Math.max(index, 0); i >= 0; --i) {
                try {
                    screen.index = i;
                    SkinSelectScreen_green.getSkin_green();
                    valid = i;
                    break;
                } catch (IndexOutOfBoundsException ignored) {
                    // 同上
                }
            }
            screen.index = valid;
            screen.refresh_green();
        } catch (Throwable t) {
            Diag.error("applyGreen", t);
        }
    }
}
