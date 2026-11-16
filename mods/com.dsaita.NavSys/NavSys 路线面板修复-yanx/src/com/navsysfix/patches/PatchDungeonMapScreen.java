package com.navsysfix.patches;

import com.dsaita.NavSys.NavSysMarkers;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.screens.DungeonMapScreen;
import com.navsysfix.MarkMode;
import com.navsysfix.PanelUi;
import com.navsysfix.SettingsOverlay;

import java.util.ArrayList;

/**
 * Makes mark mode work with a finger, and keeps every tap on our own UI from
 * being read as a map step.
 *
 * Map travel is decided by MapRoomNode.update() reading DungeonMapScreen.clicked,
 * and DungeonMapScreen.updateMouse() only sets that flag at the very end of
 * DungeonMapScreen.update(). A postfix therefore sees the click before any node
 * does - the window needed to turn a tap into a marker toggle and swallow it.
 */
@SpirePatch2(clz = DungeonMapScreen.class, method = "update", paramtypez = {})
public class PatchDungeonMapScreen {

    @SpirePostfixPatch
    public static void Postfix(DungeonMapScreen __instance) {
        if (!__instance.clicked) {
            return;
        }
        if (!CardCrawlGame.isInARun()) {
            return;
        }
        if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.MAP) {
            return;
        }

        float x = InputHelper.mX;
        float y = InputHelper.mY;

        // A tap on the NavSys UI is never a map step.
        if (SettingsOverlay.covers(x, y)
                || PanelUi.insideSettingsButton(x, y)
                || PanelUi.insideToggleButton(x, y)
                || PanelUi.insideMarkButton(x, y)
                || PanelUi.rowAt(x, y) >= 0) {
            consume(__instance);
            return;
        }

        if (!MarkMode.on || PanelUi.hidden()) {
            return;
        }
        // Never swallow the first room choice, or the act cannot be started.
        if (!AbstractDungeon.firstRoomChosen) {
            return;
        }

        MapRoomNode target = nodeUnderPointer(x, y);
        if (target == null) {
            return;
        }

        NavSysMarkers.toggle(target.x, target.y);
        consume(__instance);
    }

    private static void consume(DungeonMapScreen screen) {
        screen.clicked = false;
        screen.clickTimer = 0.0f;
    }

    private static MapRoomNode nodeUnderPointer(float x, float y) {
        ArrayList<ArrayList<MapRoomNode>> map = AbstractDungeon.map;
        if (map == null) {
            return null;
        }
        for (ArrayList<MapRoomNode> row : map) {
            if (row == null) {
                continue;
            }
            for (MapRoomNode node : row) {
                if (node == null || node.taken || node.hb == null || node.getRoom() == null) {
                    continue;
                }
                if (inside(node.hb, x, y)) {
                    return node;
                }
            }
        }
        return null;
    }

    private static boolean inside(Hitbox hb, float x, float y) {
        return x > hb.x && x < hb.x + hb.width && y > hb.y && y < hb.y + hb.height;
    }
}
