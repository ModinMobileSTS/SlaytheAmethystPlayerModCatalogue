package patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;

@SpirePatch2(cls = "code.cards.AbstractEasyCard", method = "getBottomText", paramtypez = {})
public final class CardCreditTextPatch {
    @SpirePostfixPatch
    public static String Postfix(String __result) {
        if (Settings.language != Settings.GameLanguage.ZHS || __result == null || !__result.startsWith("By ")) {
            return __result;
        }
        return "绘：" + ModMetadataTranslation.contributor(__result.substring(3));
    }
}
