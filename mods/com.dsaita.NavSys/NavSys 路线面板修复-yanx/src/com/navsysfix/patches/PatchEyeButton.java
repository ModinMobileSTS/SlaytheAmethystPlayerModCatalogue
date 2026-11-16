package com.navsysfix.patches;

import com.dsaita.NavSys.ui.EyeButton;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.navsysfix.PanelUi;

/**
 * The eye button belongs to the left-side UI, so it disappears together with
 * the panel. Otherwise collapsing the panel would leave a lone "[O]" floating
 * over the map with nothing to explain it.
 *
 * While collapsed the button reports "not clicked", and any active 4x view is
 * restored by the receiveRender patch as soon as the map closes.
 */
@SpirePatch2(clz = EyeButton.class, method = "drawAndUpdate", paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class, boolean.class})
public class PatchEyeButton {

    @SpirePrefixPatch
    public static SpireReturn Prefix() {
        if (PanelUi.hidden()) {
            return SpireReturn.Return(false);
        }
        return SpireReturn.Continue();
    }
}
