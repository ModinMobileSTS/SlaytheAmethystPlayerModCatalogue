package ironcladperks.mobilefix.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import ironcladperks.mobilefix.MobileStrings;
import ironcladperks.perks.Localization;

/** Swaps the "left click / right click / ESC" hint line for the touch hint line. */
@SpirePatch2(
        cls = "ironcladperks.perks.Localization",
        method = "ui",
        paramtypez = {String.class, String.class}
)
public final class LocalizationHintPatch {
    private LocalizationHintPatch() {
    }

    @SpirePrefixPatch
    public static SpireReturn<String> Prefix(Object[] __args) {
        if (__args != null && __args.length > 0 && "HINT_TEXT".equals(__args[0])) {
            return SpireReturn.Return(MobileStrings.hint());
        }
        return SpireReturn.Continue();
    }
}
