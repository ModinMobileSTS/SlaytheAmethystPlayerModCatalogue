package yakuzapatch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import theYakuza.cards.RareAttacks.YakuzaEssenceOfTheDragonGod;

@SpirePatch2(
    cls = "theYakuza.cards.RareAttacks.YakuzaEssenceOfTheDragonGod",
    method = "<ctor>",
    paramtypez = {}
)
public final class YakuzaEssenceOfTheDragonGodCostPatch {
    private YakuzaEssenceOfTheDragonGodCostPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(YakuzaEssenceOfTheDragonGod __instance) {
        __instance.cost = 2;
        __instance.costForTurn = 2;
    }
}
