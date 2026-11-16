package com.navsysfix;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dsaita.NavSys.NavSysMod;
import com.dsaita.NavSys.pathing.PathInfo;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.map.MapRoomNode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws the two coloured routes by reusing NavSys' own highlightPath(), so the
 * lines look exactly like the mod's native highlighting instead of a second,
 * slightly different renderer.
 *
 * Each highlighted icon also gets a ring of small dark dots drawn behind it, so
 * the coloured map icons stop looking like flat stickers on the parchment.
 *
 * Routes are only recomputed when the player moves to another node, the act
 * changes, or a setting changes - never per frame.
 */
public final class RoutePainter {

    /** Dots in the ring around every highlighted icon. */
    private static final int HALO_DOTS = 14;
    /** Map icons are drawn from a 128px source; their half width is 64 * scale. */
    private static final float ICON_HALF_SOURCE = 64.0f;

    private static Method highlightPath;
    private static Field nodeScale;
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
        if (NavSysConfigPanel.showAggroRoute && aggroPath != null) {
            apply(mod, sb, aggroPath, aggroColor());
        }
        if (NavSysConfigPanel.showSafeRoute && safePath != null) {
            apply(mod, sb, safePath, safeColor());
        }
    }

    public static Color safeColor() {
        return new Color(NavSysConfigPanel.safeR / 255.0f,
                NavSysConfigPanel.safeG / 255.0f,
                NavSysConfigPanel.safeB / 255.0f, 1.0f);
    }

    public static Color aggroColor() {
        return new Color(NavSysConfigPanel.aggroR / 255.0f,
                NavSysConfigPanel.aggroG / 255.0f,
                NavSysConfigPanel.aggroB / 255.0f, 1.0f);
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
            // Drawn before the highlight so the dots form a ring behind the icon.
            drawHalo(sb, path);
            // NavSys' highlighter consumes a PathInfo, which is a thin wrapper here.
            method.invoke(mod, new PathInfo(new ArrayList<MapRoomNode>(path)), sb, color);
        } catch (Throwable ignored) {
        }
    }

    /** A ring of small dark dots around each route icon, to give it depth. */
    private static void drawHalo(SpriteBatch sb, List<MapRoomNode> path) {
        float device = Settings.isMobile ? 2.0f : 1.0f;
        for (MapRoomNode node : path) {
            if (node == null || node.taken || node.hb == null) {
                continue;
            }
            float radius = ICON_HALF_SOURCE * nodeScale(node) * Settings.scale * device * 0.92f;
            if (radius < 4.0f) {
                continue;
            }
            float dot = Math.max(2.0f, radius * 0.14f);
            float half = dot * 0.5f;
            sb.setColor(new Color(0.03f, 0.02f, 0.02f, 0.55f));
            for (int i = 0; i < HALO_DOTS; i++) {
                double angle = i * Math.PI * 2.0 / HALO_DOTS;
                float dotX = node.hb.cX + (float) Math.cos(angle) * radius;
                float dotY = node.hb.cY + (float) Math.sin(angle) * radius;
                sb.draw(ImageMaster.WHITE_SQUARE_IMG, dotX - half, dotY - half, dot, dot);
            }
        }
        sb.setColor(Color.WHITE);
    }

    /** MapRoomNode.scale is private; it drives the drawn icon size. */
    private static float nodeScale(MapRoomNode node) {
        try {
            if (nodeScale == null) {
                nodeScale = MapRoomNode.class.getDeclaredField("scale");
                nodeScale.setAccessible(true);
            }
            float value = nodeScale.getFloat(node);
            return value > 0.05f ? value : 1.0f;
        } catch (Throwable t) {
            return 1.0f;
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
