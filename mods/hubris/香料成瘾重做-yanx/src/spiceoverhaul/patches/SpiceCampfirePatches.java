package spiceoverhaul.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.CampfireUI;
import com.megacrit.cardcrawl.rooms.RestRoom;
import spiceoverhaul.SpiceData;

/** Keeps the campfire usable after crafting Spice from a potion. */
public class SpiceCampfirePatches {

    @SpirePatch2(clz = CampfireUI.class, method = "update", paramtypez = {})
    public static class KeepCampfireOpen {
        @SpirePostfixPatch
        public static void Postfix() {
            if (!SpiceData.reopenCampfire) {
                return;
            }
            SpiceData.reopenCampfire = false;
            AbstractRoom room = AbstractDungeon.getCurrRoom();
            if (room instanceof RestRoom) {
                RestRoom rest = (RestRoom) room;
                if (rest.campfireUI != null) {
                    rest.campfireUI.reopen();
                }
            }
        }
    }
}
