package yakuzapatch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import theYakuza.cards.RarePowers.YakuzaDuctTape;

@SpirePatch2(
    cls = "theYakuza.cards.RarePowers.YakuzaDuctTape",
    method = "<ctor>",
    paramtypez = {}
)
public final class YakuzaDuctTapeCostPatch {
    private YakuzaDuctTapeCostPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(YakuzaDuctTape __instance) {
        __instance.cost = 1;
        __instance.costForTurn = 1;
    }
}
