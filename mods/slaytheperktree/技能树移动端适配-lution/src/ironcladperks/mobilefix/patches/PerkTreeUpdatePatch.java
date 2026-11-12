package ironcladperks.mobilefix.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import ironcladperks.mobilefix.MobileTreeUI;
import ironcladperks.mobilefix.PerkTreeReflect;
import ironcladperks.screens.PerkTreeScreen;

/**
 * Replaces the mouse/keyboard input handling of the perk tree with one-finger gestures.
 * Nothing else of the screen is touched, the original renderer keeps drawing it.
 */
@SpirePatch2(
        cls = "ironcladperks.screens.PerkTreeScreen",
        method = "update",
        paramtypez = {}
)
public final class PerkTreeUpdatePatch {
    private PerkTreeUpdatePatch() {
    }

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(PerkTreeScreen __instance) {
        if (!PerkTreeReflect.available()) {
            return SpireReturn.Continue();
        }
        MobileTreeUI.update(__instance);
        return SpireReturn.Return();
    }
}
