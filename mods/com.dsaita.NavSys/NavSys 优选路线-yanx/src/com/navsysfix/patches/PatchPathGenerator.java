package com.navsysfix.patches;

import com.dsaita.NavSys.pathing.PathGenerator;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.navsysfix.RouteEnumerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Replaces PathGenerator.generateAllPossiblePaths().
 *
 * The original builds every partial path from java.util.Collections.singletonList
 * and later calls remove() on whatever survived the traversal. A start node with
 * no outgoing edges leaves an immutable singleton behind, so that removal throws
 * UnsupportedOperationException and takes the whole map screen down. It also
 * pruned its own intermediate paths with List.equals based removeAll.
 *
 * The replacement lives in RouteEnumerator: plain depth-first search, mutable
 * lists only, capped recursion, and an empty result instead of an escaping
 * exception.
 */
@SpirePatch2(clz = PathGenerator.class, method = "generateAllPossiblePaths", paramtypez = {})
public class PatchPathGenerator {

    @SpirePrefixPatch
    public static SpireReturn Prefix(PathGenerator __instance) {
        try {
            return SpireReturn.Return(RouteEnumerator.trimmed());
        } catch (Throwable t) {
            return SpireReturn.Return(new ArrayList<List<MapRoomNode>>());
        }
    }
}
