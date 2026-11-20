package natsu.translation;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;

@SpirePatch2(cls = "natsu.NatsuMod", method = "receiveEditCards", paramtypez = {})
public final class ChineseCardsRegistrationPatch {
    private ChineseCardsRegistrationPatch() { }

    @SpirePrefixPatch
    public static void Prefix(natsu.NatsuMod __instance) {
        ChineseLocalization.applyStrings();
    }
}
