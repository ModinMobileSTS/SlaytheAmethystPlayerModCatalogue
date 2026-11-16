package com.navsysfix.patches;

import com.dsaita.NavSys.pathing.PathInfo;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.navsysfix.Reflect;

import java.util.List;

/**
 * PathInfo.compareTo() ranks routes by elite+rest, elite and rest only, so two
 * routes that differ solely in monster/event/shop counts compare as 0 while
 * being different objects. That breaks the Comparable contract (0 must mean
 * equal) and left the panel order to whatever the sort happened to do.
 *
 * Keep the original ranking, then break ties on the node sequence so the order
 * is stable and consistent with equals().
 */
@SpirePatch2(clz = PathInfo.class, method = "compareTo", paramtypez = {PathInfo.class})
public class PatchPathInfoCompareTo {

    @SpirePrefixPatch
    public static SpireReturn Prefix(PathInfo __instance, Object[] __args) {
        PathInfo other = (PathInfo) __args[0];
        if (other == null) {
            return SpireReturn.Return(-1);
        }
        int primary = compareRanking(__instance, other);
        if (primary != 0) {
            return SpireReturn.Return(primary);
        }
        return SpireReturn.Return(compareNodes(__instance, other));
    }

    /** Same ordering as the original reversed comparator: best route first. */
    private static int compareRanking(PathInfo a, PathInfo b) {
        int totalA = a.getEliteNodeCount() + a.getRestSiteNodeCount();
        int totalB = b.getEliteNodeCount() + b.getRestSiteNodeCount();
        if (totalA != totalB) {
            return totalB - totalA;
        }
        if (a.getEliteNodeCount() != b.getEliteNodeCount()) {
            return b.getEliteNodeCount() - a.getEliteNodeCount();
        }
        return b.getRestSiteNodeCount() - a.getRestSiteNodeCount();
    }

    private static int compareNodes(PathInfo a, PathInfo b) {
        List<MapRoomNode> left = Reflect.nodesOf(a);
        List<MapRoomNode> right = Reflect.nodesOf(b);
        int shared = Math.min(left.size(), right.size());
        for (int i = 0; i < shared; i++) {
            MapRoomNode x = left.get(i);
            MapRoomNode y = right.get(i);
            if (x == null || y == null) {
                if (x == y) {
                    continue;
                }
                return x == null ? -1 : 1;
            }
            if (x.y != y.y) {
                return x.y < y.y ? -1 : 1;
            }
            if (x.x != y.x) {
                return x.x < y.x ? -1 : 1;
            }
        }
        return Integer.compare(left.size(), right.size());
    }
}
