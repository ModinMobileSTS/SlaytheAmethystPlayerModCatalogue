package shadowverse.lowres;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;

/**
 * 兜底把帧率上限锁到 30 FPS；不改分辨率、不改特效。
 */
@SpirePatch2(
    cls = "com.megacrit.cardcrawl.core.Settings",
    method = "initialize",
    paramtypez = { boolean.class }
)
public final class No4KFpsFallbackPatch {
    private No4KFpsFallbackPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(Object[] __args) {
        Settings.MAX_FPS = No4KFpsPatch.TARGET_FPS;
    }
}
