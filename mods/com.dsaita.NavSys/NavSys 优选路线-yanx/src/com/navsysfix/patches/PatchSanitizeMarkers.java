package com.navsysfix.patches;

import com.dsaita.NavSys.NavSysMarkers;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.navsysfix.Reflect;

import java.util.Set;

/**
 * NavSys' node markers are gone, so anything still stored from an older session
 * has to go as well.
 *
 * The markers live in java.util.prefs under "com/dsaita/NavSys/markers" and are
 * reloaded for the current act whenever setAct runs. Leftover entries were what
 * painted whole icons solid blue on the map (NavSysMod.renderMarkedNodes
 * re-renders every marked node in KLEIN_BLUE and connects them with blue edges)
 * long after mark mode itself was removed - it looked like the map was showing
 * corrupted nodes.
 *
 * setAct is the one place markers are loaded, so wiping the freshly loaded set
 * and saving it back removes the leftovers permanently instead of just hiding
 * them for this run.
 */
@SpirePatch2(clz = NavSysMarkers.class, method = "setAct", paramtypez = {int.class})
public class PatchSanitizeMarkers {

    @SpirePostfixPatch
    public static void Postfix() {
        Set<String> marked = Reflect.markedNodes();
        if (marked == null || marked.isEmpty()) {
            return;
        }
        marked.clear();
        Reflect.saveMarkers();
    }
}
