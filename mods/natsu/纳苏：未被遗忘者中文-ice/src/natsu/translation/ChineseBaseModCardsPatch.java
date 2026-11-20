package natsu.translation;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;

@SpirePatch2(cls = "basemod.BaseMod", method = "publishEditCards", paramtypez = {})
public final class ChineseBaseModCardsPatch {
    private ChineseBaseModCardsPatch() { }

    @SpirePrefixPatch
    public static void Prefix() {
        System.out.println("[Natsu ZHS] cards registration; language=" + com.megacrit.cardcrawl.core.Settings.language
            + ", languagePack=" + (com.megacrit.cardcrawl.core.CardCrawlGame.languagePack != null));
        ChineseLocalization.applyStrings();
    }
}
