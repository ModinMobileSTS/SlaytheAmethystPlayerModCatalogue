package com.navsysfix.patches;

import com.dsaita.NavSys.NavSysMarkers;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;

/**
 * Markers are not a feature of this patch any more, so nothing may create them.
 *
 * NavSys toggles a marker from its right-click handler on the map; that path is
 * disabled as well, otherwise a stray entry would be written to the preferences
 * file again (and would keep the route list filtered by it).
 */
@SpirePatch2(clz = NavSysMarkers.class, method = "toggle", paramtypez = {int.class, int.class})
public class PatchNoMarkerToggle {

    @SpirePrefixPatch
    public static SpireReturn Prefix() {
        return SpireReturn.Return();
    }
}
