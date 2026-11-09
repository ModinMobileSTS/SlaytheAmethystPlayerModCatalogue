package bundleofcontentzhs;

import basemod.BaseMod;
import bundle_of_content.BundleOfContent;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.localization.RelicStrings;

/**
 * After the parent mod has loaded its own eng + zhs string files, load the complete Chinese text on
 * top of them. BaseMod stores strings in a map keyed by the full relic id, so loading the same ids
 * again simply replaces the entries the parent just registered.
 */
@SpirePatch2(
    cls = "bundle_of_content.BundleOfContent",
    method = "receiveEditStrings",
    paramtypez = {}
)
public final class ZhsStringPatch {
    private ZhsStringPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(BundleOfContent __instance) {
        if (Settings.language != Settings.GameLanguage.ZHS) {
            return;
        }
        BaseMod.loadCustomStrings(RelicStrings.class, ZhsData.RELIC_STRINGS);
        BaseMod.loadCustomStrings(PowerStrings.class, ZhsData.POWER_STRINGS);
        BaseMod.loadCustomStrings(PotionStrings.class, ZhsData.POTION_STRINGS);
    }
}
