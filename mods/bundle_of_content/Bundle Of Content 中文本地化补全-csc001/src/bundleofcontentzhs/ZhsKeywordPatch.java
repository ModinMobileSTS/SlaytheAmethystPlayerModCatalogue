package bundleofcontentzhs;

import bundle_of_content.BundleOfContent;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;

/**
 * Re-register the mod's keywords with Chinese descriptions after the parent mod has registered the
 * ones it read from its own zhs keyword file.
 */
@SpirePatch2(
    cls = "bundle_of_content.BundleOfContent",
    method = "receiveEditKeywords",
    paramtypez = {}
)
public final class ZhsKeywordPatch {
    private ZhsKeywordPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(BundleOfContent __instance) {
        if (Settings.language != Settings.GameLanguage.ZHS) {
            return;
        }
        ZhsKeywords.registerKeywords();
    }
}
