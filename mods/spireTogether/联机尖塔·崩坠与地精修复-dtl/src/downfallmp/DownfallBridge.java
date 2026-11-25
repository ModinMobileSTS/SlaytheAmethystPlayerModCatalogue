package downfallmp;

import com.megacrit.cardcrawl.screens.custom.CustomMod;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** 崩坠是可选模组，仅在联机开局或还原状态时访问。 */
final class DownfallBridge {
    private static final Logger LOG = LogManager.getLogger("DownfallMPFix");
    private static final String EVIL_SELECT = "downfall.patches.EvilModeCharacterSelect";
    private static final String DAILY = "downfall.patches.DailyModeEvilPatch";
    private static final String DOWNFALL = "downfall.downfallMod";
    private static final String CURRENCY = "downfall.patches.ui.topPanel.GoldToSoulPatches";
    private static boolean warned;

    private DownfallBridge() {}

    static boolean setMode(boolean enabled) {
        try {
            Field field = Class.forName(EVIL_SELECT).getField("evilMode");
            field.setBoolean(null, enabled);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            warn(e);
            return false;
        }
    }

    static void clearSoloSelections() {
        try {
            Object option = Class.forName(DOWNFALL).getField("evilWithinSingleton").get(null);
            if (option instanceof CustomMod) ((CustomMod) option).selected = false;
            Class.forName(DAILY).getField("todaysRunIsEvil").setBoolean(null, false);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            warn(e);
        }
    }

    static void syncGoldDisplay(boolean evil) {
        try {
            Method method = Class.forName(CURRENCY).getMethod("changeGoldToSouls", boolean.class);
            method.invoke(null, !evil);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            warn(e);
        }
    }

    private static void warn(Throwable e) {
        if (warned) return;
        warned = true;
        LOG.warn("无法访问崩坠模式或显示设置", e);
    }
}
