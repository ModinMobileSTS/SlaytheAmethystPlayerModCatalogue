package com.navsysfix.patches;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dsaita.NavSys.NavSysMod;
import com.dsaita.NavSys.ui.PathInfoPanel;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.navsysfix.Config;
import com.navsysfix.MarkMode;
import com.navsysfix.PanelUi;
import com.navsysfix.Reflect;
import com.navsysfix.RoutePainter;
import com.navsysfix.SettingsOverlay;

import java.util.List;

/**
 * NavSys' render pass, repaired and extended:
 *
 *   - the camera no longer stays at 4x after leaving the map screen;
 *   - panel hitboxes are re-derived every frame and rows the panel is not
 *     showing are parked off-screen;
 *   - the two coloured routes are painted on top of the map;
 *   - [M] mark mode, [S]/[H] panel toggle and [C] settings work on touch.
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
        handlePanelInput();
        RoutePainter.paint(__instance, sb);
        drawButtons(sb);
        SettingsOverlay.draw(sb);
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

    private static void handlePanelInput() {
        if (!InputHelper.justClickedLeft) {
            return;
        }
        float x = InputHelper.mX;
        float y = InputHelper.mY;

        if (SettingsOverlay.handleTap(x, y)) {
            return;
        }
        if (PanelUi.insideSettingsButton(x, y)) {
            SettingsOverlay.open = !SettingsOverlay.open;
            return;
        }
        if (PanelUi.insideToggleButton(x, y)) {
            Config.showPanel = !Config.showPanel;
            Config.save();
            return;
        }
        if (PanelUi.insideMarkButton(x, y)) {
            MarkMode.on = !MarkMode.on;
            return;
        }
        int row = PanelUi.rowAt(x, y);
        if (row >= 0) {
            PathInfoPanel.selectedIndex = PathInfoPanel.selectedIndex == row ? -1 : row;
        }
    }

    private static void drawButtons(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        if (PanelUi.hidden()) {
            FontHelper.renderFontLeftTopAligned(sb, FontHelper.blockInfoFont, "NavSys",
                    PanelUi.markButtonX(), PanelUi.buttonY(), Settings.CREAM_COLOR);
            FontHelper.renderFontLeftTopAligned(sb, FontHelper.blockInfoFont, "[S]",
                    PanelUi.toggleButtonX(), PanelUi.buttonY(), Settings.GOLD_COLOR);
            FontHelper.renderFontLeftTopAligned(sb, FontHelper.blockInfoFont, "[C]",
                    PanelUi.settingsButtonX(), PanelUi.buttonY(), Settings.CREAM_COLOR);
            return;
        }
        Color markColor = MarkMode.on ? Settings.GOLD_COLOR : Settings.CREAM_COLOR;
        FontHelper.renderFontLeftTopAligned(sb, FontHelper.blockInfoFont,
                MarkMode.on ? "[M ON]" : "[M]", PanelUi.markButtonX(), PanelUi.buttonY(), markColor);
        FontHelper.renderFontLeftTopAligned(sb, FontHelper.blockInfoFont, "[H]",
                PanelUi.toggleButtonX(), PanelUi.buttonY(), Settings.CREAM_COLOR);
        FontHelper.renderFontLeftTopAligned(sb, FontHelper.blockInfoFont, "[C]",
                PanelUi.settingsButtonX(), PanelUi.buttonY(), Settings.CREAM_COLOR);
    }
}
