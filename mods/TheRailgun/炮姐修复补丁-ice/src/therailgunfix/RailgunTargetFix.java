package therailgunfix;

import TheRailgun.cards.Railgun;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;

@SpirePatch2(clz = Railgun.class, method = "<ctor>", paramtypez = {})
public class RailgunTargetFix {
    @SpirePostfixPatch
    public static void postfix(Railgun __instance) {
        __instance.target = AbstractCard.CardTarget.NONE;
    }
}
