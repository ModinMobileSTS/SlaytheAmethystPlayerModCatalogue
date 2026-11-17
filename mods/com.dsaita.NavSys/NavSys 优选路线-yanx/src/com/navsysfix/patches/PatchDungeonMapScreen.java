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
import com.navsysfix.NavSysConfigPanel;

import java.util.ArrayList;

/**
 * Turns a tap into a marker toggle while mark mode is on.
 *
 * Map travel is decided by MapRoomNode.update() reading DungeonMapScreen.clicked,
 * and DungeonMapScreen.updateMouse() only sets that flag at the very end of
 * DungeonMapScreen.update(). A postfix therefore sees the click before any node
 * does - the window needed to turn a tap into a marker toggle and swallow it.
 *
 * Outside mark mode this patch touches nothing at all. In particular it does not
 * intercept taps that land on the left-side route list: the list is a read-only
 * report now, and eating those taps could make a map node under the list
 * unreachable.
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
        if (!NavSysConfigPanel.markMode) {
            return;
        }
        // Never swallow the first room choice, or the act cannot be started.
        if (!AbstractDungeon.firstRoomChosen) {
            return;
        }

        MapRoomNode target = nodeUnderPointer(InputHelper.mX, InputHelper.mY);
        if (target == null) {
            return;
        }

        NavSysMarkers.toggle(target.x, target.y);
        __instance.clicked = false;
        __instance.clickTimer = 0.0f;
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
