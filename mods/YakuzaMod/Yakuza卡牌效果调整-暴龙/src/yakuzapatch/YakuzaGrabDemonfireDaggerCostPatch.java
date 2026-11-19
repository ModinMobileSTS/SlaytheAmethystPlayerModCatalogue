package yakuzapatch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import theYakuza.cards.RareSkills.YakuzaGrabDemonfireDagger;

@SpirePatch2(
    cls = "theYakuza.cards.RareSkills.YakuzaGrabDemonfireDagger",
    method = "<ctor>",
    paramtypez = {}
)
public final class YakuzaGrabDemonfireDaggerCostPatch {
    private YakuzaGrabDemonfireDaggerCostPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(YakuzaGrabDemonfireDagger __instance) {
        __instance.cost = 1;
        __instance.costForTurn = 1;
    }
}
