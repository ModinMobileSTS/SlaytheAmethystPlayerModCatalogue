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
import com.navsysfix.MarkMode;
import com.navsysfix.PanelUi;
import com.navsysfix.Reflect;

import java.util.List;

/**
 * Repairs three problems around NavSys' own render pass:
 *
 * 1. Leaving the map screen while the eye button was active left the camera
 *    stuck at 4x, because the zoom is only restored inside receiveStartAct.
 * 2. The panel hitboxes were built once, with a 12px vertical offset and the
 *    scale of that frame, so they drifted off the painted rows and were
 *    completely wrong after a resolution or orientation change.
 * 3. There was no touch path: rows could only be selected and nodes only
 *    marked with the right mouse button, which a phone never produces.
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
        handlePanelInput();
        drawMarkModeButton((SpriteBatch) __args[0]);
    }

    /**
     * The eye button forces the camera to 4x while the map is open, but nothing
     * undoes that when the player leaves the map without pressing it again.
     */
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

    /**
     * Re-derives every row hitbox from the live panel geometry instead of the
     * "CONTENT_Y - (12 + 30 * i)" formula used when the boxes were created.
     * Runs before the mod's own hitbox loop, so hover state is already correct
     * in the same frame, and nothing is cached across resolution changes.
     */
    private static void alignPanelHitboxes(NavSysMod mod) {
        List<Hitbox> boxes = Reflect.hitboxes(mod);
        int limit = Math.min(boxes.size(), PanelUi.MAX_ROWS);
        for (int i = 0; i < limit; i++) {
            Hitbox hb = boxes.get(i);
            if (hb == null) {
                continue;
            }
            hb.resize(PanelUi.ROW_WIDTH, PanelUi.ROW_HEIGHT);
            hb.translate(PathInfoPanel.CONTENT_X, PanelUi.rowBottom(i));
        }
    }

    /**
     * Touch parity for the two actions NavSys only exposed through the right
     * mouse button: selecting a result row and toggling mark mode.
     */
    private static void handlePanelInput() {
        if (!InputHelper.justClickedLeft) {
            return;
        }
        float x = InputHelper.mX;
        float y = InputHelper.mY;

        if (PanelUi.insideMarkButton(x, y)) {
            MarkMode.on = !MarkMode.on;
            return;
        }

        int row = PanelUi.rowAt(x, y);
        if (row >= 0) {
            PathInfoPanel.selectedIndex = PathInfoPanel.selectedIndex == row ? -1 : row;
        }
    }

    private static void drawMarkModeButton(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        String label = MarkMode.on ? "[M ON]" : "[M]";
        Color color = MarkMode.on ? Settings.GOLD_COLOR : Settings.CREAM_COLOR;
        FontHelper.renderFontLeftTopAligned(sb, FontHelper.blockInfoFont, label,
                PanelUi.markButtonX(), PanelUi.markButtonY(), color);
    }
}
