package com.navsysfix;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dsaita.NavSys.NavSysMod;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.screens.DungeonMapScreen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Paints the two routes as a dyed-paper tint on the icons themselves.
 *
 * Earlier attempts failed for the same reason: they drew something *around* the
 * icon (a bigger silhouette, then a bigger outline ring). Anything drawn at a
 * different size than the icon can only look like a sticker hovering over the
 * map, and because the game keeps re-scaling nodes (MapRoomNode.oscillateColor
 * changes this.scale every frame) that halo breathed with the animation, which
 * is what made it feel like jelly.
 *
 * So nothing is drawn around the icon any more. The very same icon texture is
 * drawn once more, on top of itself, with the route colour at a partial alpha -
 * a sheet of tinted rice paper over the artwork. Because it is the same texture
 * at the exact same transform as MapRoomNode.render() uses, the colour follows
 * every pixel edge and every pulse, so it cannot drift, hover or lag. The icon's
 * own brightness differences survive in the red channel, so details (eyes,
 * masks, question marks) stay readable instead of being flooded out.
 *
 * Colours are used exactly as configured - no grey blending.
 */
public final class RoutePainter {

    /** Map icons are drawn from a 128px source with the origin at its centre. */
    private static final float SRC = 128.0f;
    private static final float ICON_HALF = 64.0f;

    private static Method drawEdges;
    private static Field nodeScale;

    /** MapRoomNode's private layout constants, read once. */
    private static boolean geometryTried;
    private static boolean geometryOk;
    private static float spacingX;
    private static float offsetX;
    private static float offsetY;

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
        if (!geometry()) {
            // The layout constants could not be read, so the tint could not be
            // placed on the icons exactly. Nothing is drawn rather than drawing
            // a misaligned copy over the map.
            return;
        }
        // Aggressive first so the safe tint sits on top where both routes meet.
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
        drawLine(mod, sb, path, color);
        tintNodes(sb, path, color);
    }

    /** NavSys' own edge renderer, so the line matches the mod's highlighting. */
    private static void drawLine(NavSysMod mod, SpriteBatch sb, List<MapRoomNode> path, Color color) {
        Method method = drawEdgesMethod();
        if (method == null) {
            return;
        }
        try {
            method.invoke(mod, new ArrayList<MapRoomNode>(path), sb, color, Boolean.FALSE);
        } catch (Throwable ignored) {
        }
    }

    private static void tintNodes(SpriteBatch sb, List<MapRoomNode> path, Color color) {
        float alpha = tintAlpha();
        if (alpha <= 0.01f) {
            return;
        }
        for (MapRoomNode node : path) {
            if (node == null || node.taken) {
                continue;
            }
            AbstractRoom room = node.getRoom();
            if (room == null) {
                continue;
            }
            Texture icon = room.getMapImg();
            if (icon == null) {
                continue;
            }
            tintIcon(sb, node, icon, color, alpha);
        }
        // Leave the batch in a neutral state for whatever renders next.
        sb.setColor(Color.WHITE);
    }

    /**
     * One extra draw of the node's own icon with the route colour.
     *
     * The transform is copied from MapRoomNode.render(): position from x/y plus
     * the node's own random offsets, origin in the middle of the 128px source,
     * scale = node scale * Settings.scale (*2 on mobile, as the game does).
     */
    private static void tintIcon(SpriteBatch sb, MapRoomNode node, Texture icon, Color color, float alpha) {
        float device = Settings.isMobile ? 2.0f : 1.0f;
        float scale = nodeScale(node) * Settings.scale * device;
        if (scale <= 0.01f) {
            return;
        }
        float x = node.x * spacingX + offsetX - ICON_HALF + node.offsetX;
        float y = node.y * Settings.MAP_DST_Y + offsetY + DungeonMapScreen.offsetY - ICON_HALF + node.offsetY;
        sb.setColor(color.r, color.g, color.b, alpha);
        sb.draw(icon, x, y, ICON_HALF, ICON_HALF, SRC, SRC, scale, scale,
                0.0f, 0, 0, 128, 128, false, false);
    }

    /** Config value 0-100 -> 0.0-1.0; read every frame so the slider is live. */
    private static float tintAlpha() {
        int percent = NavSysConfigPanel.tintPercent;
        if (percent <= 0) {
            return 0.0f;
        }
        return Math.min(percent, 100) / 100.0f;
    }

    private static Method drawEdgesMethod() {
        if (drawEdges != null) {
            return drawEdges;
        }
        try {
            Method method = NavSysMod.class.getDeclaredMethod("drawEdges",
                    List.class, SpriteBatch.class, Color.class, boolean.class);
            method.setAccessible(true);
            drawEdges = method;
        } catch (Throwable t) {
            drawEdges = null;
        }
        return drawEdges;
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

    private static boolean geometry() {
        if (geometryTried) {
            return geometryOk;
        }
        geometryTried = true;
        try {
            spacingX = staticFloat("SPACING_X");
            offsetX = staticFloat("OFFSET_X");
            offsetY = staticFloat("OFFSET_Y");
            geometryOk = spacingX != 0.0f;
        } catch (Throwable t) {
            geometryOk = false;
        }
        return geometryOk;
    }

    private static float staticFloat(String name) throws ReflectiveOperationException {
        Field f = MapRoomNode.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.getFloat(null);
    }
}
