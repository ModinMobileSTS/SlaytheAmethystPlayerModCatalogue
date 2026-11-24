package therailgunfix;

import TheRailgun.cards.Supercharge;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

@SpirePatch2(clz = Supercharge.class, method = "<ctor>", paramtypez = {})
public class SuperchargeCostFix {
    @SpirePostfixPatch
    public static void postfix(Supercharge __instance) {
        __instance.cost = 1;
        __instance.costForTurn = 1;
    }
}
