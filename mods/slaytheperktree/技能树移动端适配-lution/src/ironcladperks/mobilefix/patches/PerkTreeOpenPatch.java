package ironcladperks.mobilefix.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import ironcladperks.mobilefix.MobileTreeUI;
import ironcladperks.mobilefix.PerkTreeReflect;
import ironcladperks.screens.PerkTreeScreen;

/** Every time the tree is opened the view is recentred and the gesture state is cleared. */
@SpirePatch2(
        cls = "ironcladperks.screens.PerkTreeScreen",
        method = "open",
        paramtypez = {}
)
public final class PerkTreeOpenPatch {
    private PerkTreeOpenPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(PerkTreeScreen __instance) {
        MobileTreeUI.resetGesture();
        PerkTreeReflect.setCameraX(__instance, 0.0f);
        PerkTreeReflect.setCameraY(__instance, 0.0f);
    }
}
