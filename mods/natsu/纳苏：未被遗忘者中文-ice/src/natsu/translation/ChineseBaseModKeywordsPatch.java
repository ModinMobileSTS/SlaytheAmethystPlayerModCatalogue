package natsu.translation;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

@SpirePatch2(cls = "basemod.BaseMod", method = "publishEditKeywords", paramtypez = {})
public final class ChineseBaseModKeywordsPatch {
    private ChineseBaseModKeywordsPatch() { }

    @SpirePostfixPatch
    public static void Postfix() {
        ChineseLocalization.applyKeywords();
    }
}
