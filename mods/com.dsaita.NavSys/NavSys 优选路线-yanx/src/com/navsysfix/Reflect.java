package com.navsysfix;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.dsaita.NavSys.NavSysMarkers;
import com.dsaita.NavSys.NavSysMod;
import com.dsaita.NavSys.pathing.PathInfo;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.map.MapRoomNode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Reflection helpers for the fields NavSys keeps private.
 * Every accessor degrades to a safe default instead of throwing, so a small
 * NavSys update cannot take the whole map screen down.
 */
public final class Reflect {

    private static Field fNodes;
    private static Field fHitboxes;
    private static Field fFullMapView;
    private static Field fSavedZoom;
    private static Field fShape;
    private static Field fMarkedNodes;
    private static Field fCurrentAct;
    private static Field fCamera;
    private static Method mSaveMarkers;

    private Reflect() {
    }

    private static Field field(Field cached, Class<?> owner, String name) {
        if (cached != null) {
            return cached;
        }
        try {
            Field f = owner.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (Throwable t) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static List<MapRoomNode> nodesOf(PathInfo info) {
        if (info == null) {
            return Collections.emptyList();
        }
        fNodes = field(fNodes, PathInfo.class, "nodes");
        if (fNodes == null) {
            return Collections.emptyList();
        }
        try {
            Object value = fNodes.get(info);
            if (value instanceof List) {
                return (List<MapRoomNode>) value;
            }
        } catch (Throwable ignored) {
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public static List<Hitbox> hitboxes(NavSysMod mod) {
        if (mod == null) {
            return Collections.emptyList();
        }
        fHitboxes = field(fHitboxes, NavSysMod.class, "hitboxes");
        if (fHitboxes == null) {
            return Collections.emptyList();
        }
        try {
            Object value = fHitboxes.get(mod);
            if (value instanceof List) {
                return (List<Hitbox>) value;
            }
        } catch (Throwable ignored) {
        }
        return Collections.emptyList();
    }

    public static boolean fullMapView(NavSysMod mod) {
        fFullMapView = field(fFullMapView, NavSysMod.class, "fullMapView");
        if (fFullMapView == null) {
            return false;
        }
        try {
            return fFullMapView.getBoolean(mod);
        } catch (Throwable t) {
            return false;
        }
    }

    public static void setFullMapView(NavSysMod mod, boolean value) {
        fFullMapView = field(fFullMapView, NavSysMod.class, "fullMapView");
        if (fFullMapView == null) {
            return;
        }
        try {
            fFullMapView.setBoolean(mod, value);
        } catch (Throwable ignored) {
        }
    }

    public static float savedZoom(NavSysMod mod) {
        fSavedZoom = field(fSavedZoom, NavSysMod.class, "savedZoom");
        if (fSavedZoom == null) {
            return 1.0f;
        }
        try {
            float zoom = fSavedZoom.getFloat(mod);
            return zoom > 0.01f ? zoom : 1.0f;
        } catch (Throwable t) {
            return 1.0f;
        }
    }

    public static ShapeRenderer shapeRenderer(NavSysMod mod) {
        fShape = field(fShape, NavSysMod.class, "shapeRenderer");
        if (fShape == null) {
            return null;
        }
        try {
            Object value = fShape.get(mod);
            return value instanceof ShapeRenderer ? (ShapeRenderer) value : null;
        } catch (Throwable t) {
            return null;
        }
    }

    public static void setShapeRenderer(NavSysMod mod, ShapeRenderer renderer) {
        fShape = field(fShape, NavSysMod.class, "shapeRenderer");
        if (fShape == null) {
            return;
        }
        try {
            fShape.set(mod, renderer);
        } catch (Throwable ignored) {
        }
    }

    public static OrthographicCamera camera() {
        fCamera = field(fCamera, CardCrawlGame.class, "camera");
        if (fCamera == null) {
            return null;
        }
        try {
            Object value = fCamera.get(null);
            return value instanceof OrthographicCamera ? (OrthographicCamera) value : null;
        } catch (Throwable t) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static Set<String> markedNodes() {
        fMarkedNodes = field(fMarkedNodes, NavSysMarkers.class, "markedNodes");
        if (fMarkedNodes == null) {
            return null;
        }
        try {
            Object value = fMarkedNodes.get(null);
            return value instanceof Set ? (Set<String>) value : null;
        } catch (Throwable t) {
            return null;
        }
    }

    public static int currentAct() {
        fCurrentAct = field(fCurrentAct, NavSysMarkers.class, "currentAct");
        if (fCurrentAct == null) {
            return 1;
        }
        try {
            return fCurrentAct.getInt(null);
        } catch (Throwable t) {
            return 1;
        }
    }

    public static void saveMarkers() {
        if (mSaveMarkers == null) {
            try {
                Method m = NavSysMarkers.class.getDeclaredMethod("save");
                m.setAccessible(true);
                mSaveMarkers = m;
            } catch (Throwable t) {
                mSaveMarkers = null;
                return;
            }
        }
        try {
            mSaveMarkers.invoke(null);
        } catch (Throwable ignored) {
        }
    }
}
