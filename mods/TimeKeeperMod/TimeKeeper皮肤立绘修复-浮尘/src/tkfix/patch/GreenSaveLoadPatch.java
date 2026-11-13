package tkfix.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import timekeepermod.patch.SkinSelectScreen_green;
import tkfix.Diag;
import tkfix.SkinFixConfig;

/**
 * 同 RedSaveLoadPatch，只是静默猎手。
 */
@SpirePatch2(clz = SkinSelectScreen_green.class, method = "onLoad", paramtypez = {Integer.class})
public class GreenSaveLoadPatch {

    @SpirePostfixPatch
    public static void Postfix(SkinSelectScreen_green __instance) {
        try {
            Diag.log("save", "onLoad green idx=" + __instance.index + " cfg=" + SkinFixConfig.savedIndex(false));
            SkinFixConfig.restoreAfterLoad(false);
        } catch (Throwable t) {
            Diag.error("save.green", t);
        }
    }
}
