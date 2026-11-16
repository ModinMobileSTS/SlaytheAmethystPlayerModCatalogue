package com.navsysfix;

import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.rooms.EventRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;
import com.megacrit.cardcrawl.rooms.RestRoom;
import com.megacrit.cardcrawl.rooms.ShopRoom;
import com.megacrit.cardcrawl.rooms.TreasureRoom;

import java.util.List;

/**
 * Picks the two routes that get drawn, using the scoring scheme from the
 * RouteSuggest design that was shared for this task:
 *
 *   safe (gold):      Rest +1, Treasure +1, Shop +1, Monster -1, Elite -3, Unknown 0
 *   aggressive (red): Rest +1, Treasure +1, Shop +1, Monster +2, Elite +3, Unknown +2
 *
 * Ties are resolved deterministically: the shorter route wins, then the route
 * whose coordinates come first, so the same map always yields the same lines.
 */
public final class RouteScoring {

    public enum Kind {
        SAFE,
        AGGRESSIVE
    }

    private RouteScoring() {
    }

    /** Best route for the requested kind, or null when the map has no routes. */
    public static List<MapRoomNode> bestRoute(Kind kind) {
        List<MapRoomNode> best = null;
        int bestScore = Integer.MIN_VALUE;
        for (List<MapRoomNode> path : RouteEnumerator.enumerate()) {
            int score = score(path, kind);
            if (best == null || score > bestScore
                    || (score == bestScore && isPreferred(path, best))) {
                best = path;
                bestScore = score;
            }
        }
        return best;
    }

    public static int score(List<MapRoomNode> path, Kind kind) {
        int score = 0;
        for (MapRoomNode node : path) {
            score += weight(node, kind);
        }
        return score;
    }

    private static int weight(MapRoomNode node, Kind kind) {
        Object room = node == null ? null : node.getRoom();
        boolean safe = kind == Kind.SAFE;
        if (room instanceof RestRoom || room instanceof TreasureRoom || room instanceof ShopRoom) {
            return 1;
        }
        if (room instanceof MonsterRoomElite) {
            return safe ? -3 : 3;
        }
        if (room instanceof MonsterRoom) {
            return safe ? -1 : 2;
        }
        if (room instanceof EventRoom) {
            return safe ? 0 : 2;
        }
        return 0;
    }

    /** Shorter routes first, then earlier coordinates, for stable output. */
    private static boolean isPreferred(List<MapRoomNode> candidate, List<MapRoomNode> incumbent) {
        if (candidate.size() != incumbent.size()) {
            return candidate.size() < incumbent.size();
        }
        int shared = Math.min(candidate.size(), incumbent.size());
        for (int i = 0; i < shared; i++) {
            MapRoomNode a = candidate.get(i);
            MapRoomNode b = incumbent.get(i);
            if (a == null || b == null || a == b) {
                continue;
            }
            if (a.y != b.y) {
                return a.y < b.y;
            }
            if (a.x != b.x) {
                return a.x < b.x;
            }
        }
        return false;
    }
}
