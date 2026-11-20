package natsu.translation;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;

@SpirePatch2(cls = "natsu.NatsuMod", method = "receiveEditRelics", paramtypez = {})
public final class ChineseRelicsRegistrationPatch {
    private ChineseRelicsRegistrationPatch() { }

    @SpirePrefixPatch
    public static void Prefix(natsu.NatsuMod __instance) {
        ChineseLocalization.applyStrings();
    }
}
