package skulzh.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import skulzh.SkulChinese;

@SpirePatch2(
    cls = "skulmod.cards.Cardmods.ChargeModifier",
    method = "modifyName",
    paramtypez = { String.class, com.megacrit.cardcrawl.cards.AbstractCard.class }
)
public final class ChargedNameTranslation {
    @SpirePostfixPatch
    public static String Postfix(String __result) {
        if (!SkulChinese.enabled()) return __result;
        if ("Charged Strike".equals(__result)) return "蓄力打击";
        if ("Charged Strike+".equals(__result)) return "蓄力打击+";
        return __result;
    }
}
