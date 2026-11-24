package shadowverse.lowres;

import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.DisplayConfig;

/**
 * 把显示配置里的帧率上限锁到 30 FPS：不动分辨率，也不改特效。
 */
@SpirePatch2(
    cls = "com.megacrit.cardcrawl.core.DisplayConfig",
    method = "readConfig",
    paramtypez = {}
)
public final class No4KFpsPatch {
    public static final int TARGET_FPS = 30;

    private No4KFpsPatch() {
    }

    @SpirePostfixPatch
    public static DisplayConfig Postfix(DisplayConfig __result) {
        if (__result != null) {
            ReflectionHacks.setPrivate(__result, DisplayConfig.class, "fps_limit", Integer.valueOf(TARGET_FPS));
        }
        return __result;
    }
}
