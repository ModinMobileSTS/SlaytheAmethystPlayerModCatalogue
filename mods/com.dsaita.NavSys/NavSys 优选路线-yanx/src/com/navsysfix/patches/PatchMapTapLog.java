package com.navsysfix.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.screens.DungeonMapScreen;

import java.util.ArrayList;

/**
 * One log line per tap on the map screen, and nothing else.
 *
 * It exists because "the node does not react" can come from several unrelated
 * places (pointer coordinate mismatch, a node that is not connected to the
 * current one, a room phase that is not COMPLETE, a swallowed click). Travel
 * itself is decided inside MapRoomNode.update() with these exact conditions, so
 * printing them at the moment of the tap makes the failing one obvious instead
 * of guessing.
 *
 * DungeonMapScreen.updateMouse() sets clicked at the very end of update(), so
 * this postfix sees the tap of every frame that produced one.
 */
@SpirePatch2(clz = DungeonMapScreen.class, method = "update", paramtypez = {})
public class PatchMapTapLog {

    @SpirePostfixPatch
    public static void Postfix(DungeonMapScreen __instance) {
        if (!__instance.clicked) {
            return;
        }
        if (!CardCrawlGame.isInARun() || AbstractDungeon.screen != AbstractDungeon.CurrentScreen.MAP) {
            return;
        }
        try {
            basemod.BaseMod.logger.info("NavSysFix tap: " + describe());
        } catch (Throwable ignored) {
        }
    }

    private static String describe() {
        MapRoomNode current = AbstractDungeon.getCurrMapNode();
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        StringBuilder sb = new StringBuilder();
        sb.append("pointer=").append(InputHelper.mX).append(',').append(InputHelper.mY);
        sb.append(" firstRoomChosen=").append(AbstractDungeon.firstRoomChosen);
        sb.append(" phase=").append(room == null ? "null" : room.phase);
        sb.append(" current=").append(current == null ? "null" : coords(current));
        sb.append(" edges=").append(current == null ? "-" : current.getEdges().size());
        sb.append(" hovered=[");
        ArrayList<ArrayList<MapRoomNode>> map = AbstractDungeon.map;
        int found = 0;
        if (map != null) {
            for (ArrayList<MapRoomNode> row : map) {
                if (row == null) {
                    continue;
                }
                for (MapRoomNode node : row) {
                    if (node == null || node.hb == null || !inside(node)) {
                        continue;
                    }
                    if (found > 0) {
                        sb.append(' ');
                    }
                    sb.append(coords(node));
                    sb.append(node.taken ? "(taken)" : "");
                    if (current != null) {
                        sb.append(current.isConnectedTo(node) ? "(connected)" : "(NOT connected)");
                    }
                    found++;
                    if (found >= 4) {
                        break;
                    }
                }
            }
        }
        sb.append(']');
        return sb.toString();
    }

    private static String coords(MapRoomNode node) {
        return node.x + "," + node.y;
    }

    private static boolean inside(MapRoomNode node) {
        return InputHelper.mX > node.hb.x && InputHelper.mX < node.hb.x + node.hb.width
                && InputHelper.mY > node.hb.y && InputHelper.mY < node.hb.y + node.hb.height;
    }
}
