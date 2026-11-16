package com.navsysfix.patches;

import com.dsaita.NavSys.NavSysMarkers;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.navsysfix.Reflect;

import java.util.Set;

/**
 * Markers are stored as "act:x:y" strings in java.util.prefs. PathInfoPanel
 * parses them without any validation, so one malformed entry left behind by an
 * older build throws NumberFormatException on every map frame. Entries are only
 * ever loaded through setAct, so sanitising the set right after loading fixes
 * the whole class of failures at its source.
 */
@SpirePatch2(clz = NavSysMarkers.class, method = "setAct", paramtypez = {int.class})
public class PatchSanitizeMarkers {

    @SpirePostfixPatch
    public static void Postfix() {
        Set<String> marked = Reflect.markedNodes();
        if (marked == null) {
            return;
        }
        marked.removeIf(key -> !isWellFormed(key));
    }

    private static boolean isWellFormed(String key) {
        if (key == null) {
            return false;
        }
        String[] parts = key.split(":");
        if (parts.length != 3) {
            return false;
        }
        try {
            Integer.parseInt(parts[0]);
            Integer.parseInt(parts[1]);
            Integer.parseInt(parts[2]);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
