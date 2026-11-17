package com.navsysfix;

import basemod.BaseMod;
import basemod.interfaces.PostInitializeSubscriber;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;

/**
 * Entry point: restores the saved settings, then registers the settings page the
 * same way other patches do it.
 *
 * The page is NavSysConfigPanel (a BaseMod EasyConfigPanel). Handing it to
 * BaseMod.registerModBadge() lets BaseMod's ModBadge own the whole lifecycle -
 * the badge is drawn on the main menu, clicking it sets modSettingsUp, calls
 * panel.onCreate() to build the widgets, and renders/updates the panel until it
 * is closed. No custom click handling is needed here.
 */
@SpireInitializer
public class NavSysFixInit implements PostInitializeSubscriber {

    private static final String BADGE_NAME = "NavSys 优选路线";
    private static final String BADGE_AUTHOR = "AI 补丁";
    private static final String BADGE_DESC = "金线=安全路线，红线=激进路线。点击徽章打开设置页。";

    public NavSysFixInit() {
        ConfigRuntime.load();
        clearLeftoverMarkers();
        BaseMod.subscribe(this);
    }

    /**
     * NavSys' node markers are no longer used, but older sessions may have left
     * entries behind; those were drawn as solid blue icons on the map.
     */
    private static void clearLeftoverMarkers() {
        try {
            com.dsaita.NavSys.NavSysMarkers.getMarkedNodes().clear();
            com.navsysfix.Reflect.saveMarkers();
        } catch (Throwable ignored) {
        }
    }

    public static void initialize() {
        new NavSysFixInit();
    }

    @Override
    public void receivePostInitialize() {
        try {
            NavSysConfigPanel panel = new NavSysConfigPanel();
            Texture badge = buildBadge();
            if (badge != null) {
                BaseMod.registerModBadge(badge, BADGE_NAME, BADGE_AUTHOR, BADGE_DESC, panel);
            }
        } catch (Throwable ignored) {
            // A broken badge must never stop the rest of the patch from loading;
            // the saved settings are already applied by ConfigRuntime.
        }
    }

    /**
     * 40x40 badge drawn at runtime: a gold ring (safe route) around a red ring
     * (aggressive route). Built from a Pixmap so the patch ships no image files.
     */
    private static Texture buildBadge() {
        Pixmap pixmap = null;
        try {
            pixmap = new Pixmap(40, 40, Pixmap.Format.RGBA8888);
            pixmap.setColor(0.0f, 0.0f, 0.0f, 0.0f);
            pixmap.fill();

            pixmap.setColor(0.80f, 0.68f, 0.32f, 1.0f);
            pixmap.drawCircle(20, 20, 17);
            pixmap.drawCircle(20, 20, 16);

            pixmap.setColor(0.72f, 0.30f, 0.26f, 1.0f);
            pixmap.drawCircle(20, 20, 10);
            pixmap.drawCircle(20, 20, 9);

            return new Texture(pixmap);
        } catch (Throwable t) {
            return null;
        } finally {
            if (pixmap != null) {
                pixmap.dispose();
            }
        }
    }
}
