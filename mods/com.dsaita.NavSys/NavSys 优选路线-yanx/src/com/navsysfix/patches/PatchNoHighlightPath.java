package com.navsysfix.patches;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dsaita.NavSys.NavSysMod;
import com.dsaita.NavSys.pathing.PathInfo;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;

/**
 * Stops NavSys from painting a whole route solid blue.
 *
 * highlightPath sets every node's colour to the given colour and re-renders the
 * node, so the entire route turns into flat blue icons (the thing this patch
 * exists to avoid). It is driven by hovering a row of the left list - which is a
 * read-only report now - and by PathInfoPanel.selectedIndex. Leftover markers and
 * a stale selection were another source of unexpected blue nodes on the map, so
 * the pass is disabled outright; the two route colours are painted by
 * RoutePainter instead, which keeps the icons readable.
 */
@SpirePatch2(clz = NavSysMod.class, method = "highlightPath",
        paramtypez = {PathInfo.class, SpriteBatch.class, Color.class})
public class PatchNoHighlightPath {

    @SpirePrefixPatch
    public static SpireReturn Prefix() {
        return SpireReturn.Return();
    }
}
