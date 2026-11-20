package patches;

import code.cards.AokumoRin.Ao_11_Moschoko;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

@SpirePatch2(cls = "code.cards.AokumoRin.Ao_11_Moschoko", method = "onMoveToDiscard", paramtypez = {})
public final class ChocolateDiscardTextPatch {
    @SpirePostfixPatch
    public static void Postfix(Ao_11_Moschoko __instance) {
        ChocolateApplyTextPatch.translate(__instance);
    }
}
