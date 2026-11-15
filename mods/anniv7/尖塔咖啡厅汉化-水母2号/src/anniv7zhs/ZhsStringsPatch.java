package anniv7zhs;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;

/**
 * Runs right after Spire Cafe finished loading its own strings, so the Chinese files
 * overwrite the English ones before any card, relic or potion is constructed.
 */
@SpirePatch2(
    cls = "spireCafe.Anniv7Mod",
    method = "receiveEditStrings",
    paramtypez = {}
)
public final class ZhsStringsPatch {

    private ZhsStringsPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(spireCafe.Anniv7Mod __instance) {
        if (Settings.language != Settings.GameLanguage.ZHS) {
            return;
        }
        ZhsLoader.loadAll();
    }
}
