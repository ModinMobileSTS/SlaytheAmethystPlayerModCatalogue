package therailgunfix;

import TheRailgun.cards.Scatter_Railgun;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;

@SpirePatch2(clz = Scatter_Railgun.class, method = "<ctor>", paramtypez = {})
public class ScatterRailgunTargetFix {
    @SpirePostfixPatch
    public static void postfix(Scatter_Railgun __instance) {
        __instance.target = AbstractCard.CardTarget.NONE;
    }
}
