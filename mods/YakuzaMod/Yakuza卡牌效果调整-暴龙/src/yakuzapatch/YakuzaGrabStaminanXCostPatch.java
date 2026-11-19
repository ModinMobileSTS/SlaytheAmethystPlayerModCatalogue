package yakuzapatch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import theYakuza.cards.RareSkills.YakuzaGrabStaminanX;

@SpirePatch2(
    cls = "theYakuza.cards.RareSkills.YakuzaGrabStaminanX",
    method = "<ctor>",
    paramtypez = {}
)
public final class YakuzaGrabStaminanXCostPatch {
    private YakuzaGrabStaminanXCostPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(YakuzaGrabStaminanX __instance) {
        __instance.cost = 1;
        __instance.costForTurn = 1;
    }
}
