package com.navsysfix.patches;

import com.dsaita.NavSys.pathing.PathInfo;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.navsysfix.Reflect;

import java.util.List;

/**
 * PathInfo.equals() only compares the five room-type counters, so two clearly
 * different routes with the same makeup are "equal". PathInfoPanel.draw() then
 * runs .distinct() over them and silently drops real candidates.
 *
 * Identity of a route is its node sequence, so compare that instead.
 */
@SpirePatch2(clz = PathInfo.class, method = "equals", paramtypez = {Object.class})
public class PatchPathInfoEquals {

    @SpirePrefixPatch
    public static SpireReturn Prefix(PathInfo __instance, Object[] __args) {
        Object other = __args[0];
        if (__instance == other) {
            return SpireReturn.Return(true);
        }
        if (other == null || __instance.getClass() != other.getClass()) {
            return SpireReturn.Return(false);
        }
        return SpireReturn.Return(sameNodes(Reflect.nodesOf(__instance), Reflect.nodesOf((PathInfo) other)));
    }

    static boolean sameNodes(List<MapRoomNode> left, List<MapRoomNode> right) {
        if (left.size() != right.size()) {
            return false;
        }
        for (int i = 0; i < left.size(); i++) {
            MapRoomNode a = left.get(i);
            MapRoomNode b = right.get(i);
            if (a == null || b == null) {
                if (a != b) {
                    return false;
                }
                continue;
            }
            if (a.x != b.x || a.y != b.y) {
                return false;
            }
        }
        return true;
    }
}
