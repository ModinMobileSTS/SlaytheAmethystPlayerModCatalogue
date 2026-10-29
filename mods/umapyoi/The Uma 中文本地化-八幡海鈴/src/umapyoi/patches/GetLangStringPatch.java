package umapyoi.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.Settings;
import theuma.ModFile;

@SpirePatch2(
    cls = "theuma.ModFile",
    method = "getLangString",
    paramtypez = {}
)
public final class GetLangStringPatch {
    private GetLangStringPatch() {
    }

    @SpirePrefixPatch
    public static SpireReturn<String> Prefix(ModFile __instance) {
        if (Settings.language == Settings.GameLanguage.ZHS) {
            return SpireReturn.Return("zhs");
        }
        return SpireReturn.Continue();
    }
}
