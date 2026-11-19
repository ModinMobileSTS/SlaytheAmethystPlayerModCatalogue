package yakuzapatch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import theYakuza.cards.RarePowers.YakuzaColiseum;

@SpirePatch2(
    cls = "theYakuza.cards.RarePowers.YakuzaColiseum",
    method = "upgrade",
    paramtypez = {}
)
public final class YakuzaColiseumUpgradeCostPatch {
    private YakuzaColiseumUpgradeCostPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(YakuzaColiseum __instance) {
        __instance.cost = 0;
        __instance.costForTurn = 0;
    }
}
