package com.navsysfix.patches;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dsaita.NavSys.NavSysMod;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;

/**
 * Stops NavSys from painting marked nodes.
 *
 * renderMarkedNodes sets every marked node's colour to KLEIN_BLUE and re-renders
 * it, then draws blue edges between all of them. With markers no longer settable
 * (mark mode was removed because it swallowed map clicks) that pass can only ever
 * display leftovers, which is what produced the solid blue icons on the map.
 */
@SpirePatch2(clz = NavSysMod.class, method = "renderMarkedNodes", paramtypez = {SpriteBatch.class})
public class PatchNoMarkedNodes {

    @SpirePrefixPatch
    public static SpireReturn Prefix() {
        return SpireReturn.Return();
    }
}
