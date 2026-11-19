package yakuzapatch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import theYakuza.cards.RareSkills.YakuzaGrabMotorcycle;

@SpirePatch2(
    cls = "theYakuza.cards.RareSkills.YakuzaGrabMotorcycle",
    method = "<ctor>",
    paramtypez = {}
)
public final class YakuzaGrabMotorcycleCostPatch {
    private YakuzaGrabMotorcycleCostPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(YakuzaGrabMotorcycle __instance) {
        __instance.cost = 2;
        __instance.costForTurn = 2;
    }
}
