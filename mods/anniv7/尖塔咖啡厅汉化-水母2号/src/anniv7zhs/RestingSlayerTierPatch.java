package anniv7zhs;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;

/**
 * Resting Slayer builds its trade options from the relic tier and card rarity enum names,
 * which are always English ("Starter", "Common", ...). Those names never pass through a
 * localization file, so they are translated here when the game runs in Simplified Chinese.
 *
 * <p>Only the six names the patron can offer are mapped; every other string (in particular
 * card names, which also go through this method) is returned unchanged. The Chinese text is
 * written with unicode escapes so it cannot be mangled by the compiler's source encoding.</p>
 */
@SpirePatch2(
    cls = "spireCafe.interactables.patrons.restingslayer.RestingSlayerPatron",
    method = "capitalize",
    paramtypez = { String.class }
)
public final class RestingSlayerTierPatch {

    private RestingSlayerTierPatch() {
    }

    @SpirePostfixPatch
    public static String Postfix(String __result) {
        if (Settings.language != Settings.GameLanguage.ZHS || __result == null) {
            return __result;
        }
        switch (__result) {
            case "Starter":
                return "\u521d\u59cb";
            case "Basic":
                return "\u521d\u59cb";
            case "Common":
                return "\u666e\u901a";
            case "Uncommon":
                return "\u7f55\u89c1";
            case "Rare":
                return "\u7a00\u6709";
            case "Shop":
                return "\u5546\u5e97";
            default:
                return __result;
        }
    }
}
