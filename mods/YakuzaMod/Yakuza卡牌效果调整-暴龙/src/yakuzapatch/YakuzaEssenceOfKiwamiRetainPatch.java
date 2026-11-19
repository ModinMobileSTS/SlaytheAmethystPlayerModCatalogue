package yakuzapatch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import theYakuza.cards.RareAttacks.YakuzaEssenceOfKiwami;

@SpirePatch2(
    cls = "theYakuza.cards.RareAttacks.YakuzaEssenceOfKiwami",
    method = "<ctor>",
    paramtypez = {}
)
public final class YakuzaEssenceOfKiwamiRetainPatch {
    private YakuzaEssenceOfKiwamiRetainPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(YakuzaEssenceOfKiwami __instance) {
        __instance.selfRetain = true;
        __instance.rawDescription = __instance.rawDescription + " NL 保留";
        __instance.initializeDescription();
    }
}
