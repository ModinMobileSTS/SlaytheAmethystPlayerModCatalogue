package yakuzapatch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import theYakuza.cards.RarePowers.YakuzaColiseum;

@SpirePatch2(
    cls = "theYakuza.cards.RarePowers.YakuzaColiseum",
    method = "<ctor>",
    paramtypez = {}
)
public final class YakuzaColiseumCostPatch {
    private YakuzaColiseumCostPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(YakuzaColiseum __instance) {
        __instance.cost = 1;
        __instance.costForTurn = 1;
    }
}
