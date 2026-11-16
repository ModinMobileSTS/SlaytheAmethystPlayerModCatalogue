package com.navsysfix.patches;

import com.dsaita.NavSys.NavSysMarkers;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.navsysfix.Reflect;

import java.util.Set;

/**
 * NavSysMarkers.clearCurrentAct() clears the whole marker set, including the
 * markers of every other act, and then saves that empty set into the current
 * act's preference key. It should only drop the current act's markers.
 */
@SpirePatch2(clz = NavSysMarkers.class, method = "clearCurrentAct", paramtypez = {})
public class PatchClearCurrentAct {

    @SpirePrefixPatch
    public static SpireReturn Prefix() {
        Set<String> marked = Reflect.markedNodes();
        if (marked == null) {
            // Reflection failed: let the original run rather than do nothing.
            return SpireReturn.Continue();
        }
        String actPrefix = Reflect.currentAct() + ":";
        marked.removeIf(key -> key != null && key.startsWith(actPrefix));
        Reflect.saveMarkers();
        return SpireReturn.Return();
    }
}
