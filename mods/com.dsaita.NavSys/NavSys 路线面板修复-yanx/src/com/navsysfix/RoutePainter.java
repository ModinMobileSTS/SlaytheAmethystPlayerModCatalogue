package com.navsysfix;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dsaita.NavSys.NavSysMod;
import com.dsaita.NavSys.pathing.PathInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.map.MapRoomNode;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws the two coloured routes by reusing NavSys' own highlightPath(), so the
 * lines look exactly like the mod's native highlighting instead of a second,
 * slightly different renderer.
 *
 * Routes are only recomputed when the player moves to another node, the act
 * changes, or a setting changes - never per frame.
 */
public final class RoutePainter {

    private static Method highlightPath;
    private static List<MapRoomNode> safePath;
    private static List<MapRoomNode> aggroPath;
    private static MapRoomNode lastNode;
    private static int lastAct = -1;
    private static boolean dirty = true;

    private RoutePainter() {
    }

    public static void invalidate() {
        dirty = true;
    }

    public static void paint(NavSysMod mod, SpriteBatch sb) {
        MapRoomNode current = AbstractDungeon.getCurrMapNode();
        int act = AbstractDungeon.actNum;
        if (dirty || current != lastNode || act != lastAct) {
            recompute();
            lastNode = current;
            lastAct = act;
            dirty = false;
        }
        // Aggressive first so the safe line sits on top where they overlap.
        if (Config.showAggroRoute && aggroPath != null) {
            apply(mod, sb, aggroPath, Config.aggroColor());
        }
        if (Config.showSafeRoute && safePath != null) {
            apply(mod, sb, safePath, Config.safeColor());
        }
    }

    private static void recompute() {
        safePath = RouteScoring.bestRoute(RouteScoring.Kind.SAFE);
        aggroPath = RouteScoring.bestRoute(RouteScoring.Kind.AGGRESSIVE);
    }

    private static void apply(NavSysMod mod, SpriteBatch sb, List<MapRoomNode> path, Color color) {
        if (path == null || path.size() < 2) {
            return;
        }
        Method method = highlightMethod();
        if (method == null) {
            return;
        }
        try {
            // NavSys' highlighter consumes a PathInfo, which is a thin wrapper here.
            method.invoke(mod, new PathInfo(new ArrayList<MapRoomNode>(path)), sb, color);
        } catch (Throwable ignored) {
        }
    }

    private static Method highlightMethod() {
        if (highlightPath != null) {
            return highlightPath;
        }
        try {
            Method method = NavSysMod.class.getDeclaredMethod("highlightPath",
                    PathInfo.class, SpriteBatch.class, Color.class);
            method.setAccessible(true);
            highlightPath = method;
        } catch (Throwable t) {
            highlightPath = null;
        }
        return highlightPath;
    }
}
