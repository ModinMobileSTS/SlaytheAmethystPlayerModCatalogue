package yakuzapatch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import theYakuza.cards.RarePowers.Yakuza4thChairman;

@SpirePatch2(
    cls = "theYakuza.cards.RarePowers.Yakuza4thChairman",
    method = "<ctor>",
    paramtypez = {}
)
public final class Yakuza4thChairmanCostPatch {
    private Yakuza4thChairmanCostPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(Yakuza4thChairman __instance) {
        __instance.cost = 1;
        __instance.costForTurn = 1;
        String rd = __instance.rawDescription;
        rd = rd.replace("获得3层", "获得2层");
        rd = rd.replace("Gain 3", "Gain 2");
        __instance.rawDescription = rd;
        __instance.initializeDescription();
    }
}
