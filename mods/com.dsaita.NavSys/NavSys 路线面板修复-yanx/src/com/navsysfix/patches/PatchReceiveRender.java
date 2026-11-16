package com.navsysfix.patches;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dsaita.NavSys.NavSysMod;
import com.dsaita.NavSys.ui.PathInfoPanel;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.navsysfix.PanelUi;
import com.navsysfix.Reflect;
import com.navsysfix.RoutePainter;

import java.util.List;

/**
 * NavSys' render pass, repaired and extended:
 *
 *   - the camera no longer stays at 4x after leaving the map screen;
 *   - panel hitboxes are re-derived every frame and rows the panel is not
 *     showing are parked off-screen;
 *   - the two coloured routes are painted on top of the map.
 *
 * The map itself carries no buttons: every switch lives in the mod's config page.
 */
@SpirePatch2(clz = NavSysMod.class, method = "receiveRender", paramtypez = {SpriteBatch.class})
public class PatchReceiveRender {

    @SpirePrefixPatch
    public static void Prefix(NavSysMod __instance) {
        restoreCameraWhenLeavingMap(__instance);
        alignPanelHitboxes(__instance);
    }

    @SpirePostfixPatch
    public static void Postfix(NavSysMod __instance, Object[] __args) {
        if (!CardCrawlGame.isInARun()) {
            return;
        }
        if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.MAP) {
            return;
        }
        SpriteBatch sb = (SpriteBatch) __args[0];
        handleRowTap();
        RoutePainter.paint(__instance, sb);
    }

    private static void restoreCameraWhenLeavingMap(NavSysMod mod) {
        if (!Reflect.fullMapView(mod)) {
            return;
        }
        boolean stillOnMap = CardCrawlGame.isInARun()
                && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MAP;
        if (stillOnMap) {
            return;
        }
        OrthographicCamera camera = Reflect.camera();
        if (camera != null) {
            camera.zoom = Reflect.savedZoom(mod);
            camera.update();
        }
        Reflect.setFullMapView(mod, false);
    }

    private static void alignPanelHitboxes(NavSysMod mod) {
        List<Hitbox> boxes = Reflect.hitboxes(mod);
        int limit = Math.min(boxes.size(), PanelUi.MAX_ROWS);
        int visible = PanelUi.rowCount();
        for (int i = 0; i < limit; i++) {
            Hitbox hb = boxes.get(i);
            if (hb == null) {
                continue;
            }
            if (i < visible) {
                hb.resize(PanelUi.ROW_WIDTH, PanelUi.ROW_HEIGHT);
                hb.translate(PathInfoPanel.CONTENT_X, PanelUi.rowBottom(i));
            } else {
                hb.resize(0.0f, 0.0f);
                hb.translate(-10000.0f, -10000.0f);
            }
        }
    }

    /** Tapping a route row selects it in the list. */
    private static void handleRowTap() {
        if (!InputHelper.justClickedLeft) {
            return;
        }
        int row = PanelUi.rowAt(InputHelper.mX, InputHelper.mY);
        if (row >= 0) {
            PathInfoPanel.selectedIndex = PathInfoPanel.selectedIndex == row ? -1 : row;
        }
    }
}
