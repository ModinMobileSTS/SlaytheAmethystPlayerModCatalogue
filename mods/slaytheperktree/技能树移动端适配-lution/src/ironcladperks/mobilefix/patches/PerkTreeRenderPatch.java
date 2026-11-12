package ironcladperks.mobilefix.patches;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import ironcladperks.mobilefix.MobileTreeUI;
import ironcladperks.screens.PerkTreeScreen;

/** Draws the touch buttons (close / bonus switch / tap mode) on top of the perk tree. */
@SpirePatch2(
        cls = "ironcladperks.screens.PerkTreeScreen",
        method = "render",
        paramtypez = {SpriteBatch.class}
)
public final class PerkTreeRenderPatch {
    private PerkTreeRenderPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(PerkTreeScreen __instance, Object[] __args) {
        if (__instance == null || !__instance.isOpen) {
            return;
        }
        if (__args == null || __args.length < 1 || !(__args[0] instanceof SpriteBatch)) {
            return;
        }
        MobileTreeUI.render((SpriteBatch) __args[0]);
    }
}
