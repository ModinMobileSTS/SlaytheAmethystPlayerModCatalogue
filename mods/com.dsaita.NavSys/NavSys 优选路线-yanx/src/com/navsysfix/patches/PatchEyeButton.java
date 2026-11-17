package com.navsysfix.patches;

import com.dsaita.NavSys.ui.EyeButton;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;

/**
 * The eye button is gone for good.
 *
 * It only toggled NavSys' 4x map view, and that view breaks map input: the game
 * reads the pointer in screen coordinates while node hitboxes live in world
 * coordinates, so any zoom other than 1 makes every tap miss its node - the map
 * looks fine but cannot be clicked. The button is hidden and the patch on
 * receiveRender also undoes the zoom if it ever ends up on.
 */
@SpirePatch2(clz = EyeButton.class, method = "drawAndUpdate", paramtypez = {com.badlogic.gdx.graphics.g2d.SpriteBatch.class, boolean.class})
public class PatchEyeButton {

    @SpirePrefixPatch
    public static SpireReturn Prefix() {
        return SpireReturn.Return(false);
    }
}
