package com.navsysfix;

import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.neow.NeowRoom;
import com.megacrit.cardcrawl.rooms.EmptyRoom;
import com.megacrit.cardcrawl.rooms.TreasureRoomBoss;

import java.util.ArrayList;
import java.util.List;

/**
 * Depth-first enumeration of every route to the end of the map. Replaces NavSys'
 * own generator, which builds partial paths from java.util.Collections.singletonList
 * and later calls remove() on whatever survived - an immutable singleton that
 * survived makes that throw UnsupportedOperationException and takes the map
 * screen down.
 *
 * This version only ever uses mutable ArrayLists, caps recursion depth, and
 * shares one implementation between "what the panel lists" and "what the two
 * coloured routes are", so they can never disagree.
 *
 * It also always produces something usable. Before the first room of an act is
 * chosen there is no current node, and after a boss the current node belongs to
 * the previous act - in both cases the routes have to start from the act's
 * bottom floor. If even that yields nothing (a start node with no outgoing
 * edges, a save loaded mid-act), the search is retried from every node that
 * actually leads somewhere, so the map never ends up with no suggestions at all.
 */
public final class RouteEnumerator {

    private RouteEnumerator() {
    }

    /** Full routes, including the node the player is standing on. */
    public static List<List<MapRoomNode>> enumerate() {
        List<List<MapRoomNode>> paths = new ArrayList<>();
        ArrayList<ArrayList<MapRoomNode>> map = AbstractDungeon.map;
        if (map == null || map.isEmpty()) {
            return paths;
        }
        int maxDepth = map.size() + 2;

        MapRoomNode current = AbstractDungeon.getCurrMapNode();
        if (current != null && !isActStartRoom(current)) {
            collect(current, new ArrayList<MapRoomNode>(), paths, map, maxDepth);
        }
        if (!hasRoute(paths)) {
            // Act start: the player has not picked a room yet, so every route
            // begins on the bottom floor.
            paths.clear();
            ArrayList<MapRoomNode> firstRow = map.get(0);
            if (firstRow != null) {
                for (MapRoomNode start : firstRow) {
                    if (start == null) {
                        continue;
                    }
                    collect(start, new ArrayList<MapRoomNode>(), paths, map, maxDepth);
                }
            }
        }
        if (!hasRoute(paths)) {
            // Last resort: start from anything that is still connected upwards.
            paths.clear();
            for (ArrayList<MapRoomNode> row : map) {
                if (row == null) {
                    continue;
                }
                for (MapRoomNode start : row) {
                    if (start == null || childrenOf(start, map).isEmpty()) {
                        continue;
                    }
                    collect(start, new ArrayList<MapRoomNode>(), paths, map, maxDepth);
                }
            }
        }
        return paths;
    }

    /**
     * The routes NavSys used to hand to its panel: the current node stripped
     * off, and anything shorter than two nodes dropped.
     */
    public static List<List<MapRoomNode>> trimmed() {
        MapRoomNode current = AbstractDungeon.getCurrMapNode();
        List<List<MapRoomNode>> result = new ArrayList<>();
        for (List<MapRoomNode> path : enumerate()) {
            List<MapRoomNode> copy = new ArrayList<>(path);
            if (current != null && !copy.isEmpty() && copy.get(0) == current) {
                copy.remove(0);
            }
            if (copy.size() > 1) {
                result.add(copy);
            }
        }
        return result;
    }

    /** A route worth drawing or listing needs at least two nodes. */
    public static boolean hasRoute(List<List<MapRoomNode>> paths) {
        if (paths == null) {
            return false;
        }
        for (List<MapRoomNode> path : paths) {
            if (path != null && path.size() >= 2) {
                return true;
            }
        }
        return false;
    }

    private static boolean isActStartRoom(MapRoomNode node) {
        return node.getRoom() instanceof NeowRoom
                || node.getRoom() instanceof EmptyRoom
                || node.getRoom() instanceof TreasureRoomBoss;
    }

    private static void collect(MapRoomNode node, List<MapRoomNode> trail,
                                List<List<MapRoomNode>> out,
                                ArrayList<ArrayList<MapRoomNode>> map,
                                int maxDepth) {
        if (trail.size() > maxDepth) {
            return;
        }
        trail.add(node);
        List<MapRoomNode> children = childrenOf(node, map);
        if (children.isEmpty()) {
            out.add(new ArrayList<MapRoomNode>(trail));
        } else {
            for (MapRoomNode child : children) {
                collect(child, trail, out, map, maxDepth);
            }
        }
        trail.remove(trail.size() - 1);
    }

    /** Children are the next floor's nodes that list this node as a parent. */
    private static List<MapRoomNode> childrenOf(MapRoomNode node,
                                                ArrayList<ArrayList<MapRoomNode>> map) {
        List<MapRoomNode> children = new ArrayList<>();
        for (ArrayList<MapRoomNode> row : map) {
            if (row == null) {
                continue;
            }
            for (MapRoomNode candidate : row) {
                if (candidate == null) {
                    continue;
                }
                List<MapRoomNode> parents = candidate.getParents();
                if (parents != null && parents.contains(node)) {
                    children.add(candidate);
                }
            }
            if (!children.isEmpty()) {
                return children;
            }
        }
        return children;
    }
}
