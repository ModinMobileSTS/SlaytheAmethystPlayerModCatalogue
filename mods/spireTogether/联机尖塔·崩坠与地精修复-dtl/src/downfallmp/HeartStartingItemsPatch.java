package downfallmp;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.neow.NeowRoom;
import spireTogether.SpireTogetherMod;
import spireTogether.network.P2P.P2PManager;
import spireTogether.patches.StartingItemsPatches;

@SpirePatch2(clz = NeowRoom.class, method = "update", paramtypez = {},
    requiredModId = "downfall", optional = true)
public final class HeartStartingItemsPatch {
    private static final String HEART_EVENT = "downfall.events.HeartEvent";

    private HeartStartingItemsPatch() {}

    @SpirePostfixPatch
    public static void Postfix(NeowRoom __instance) {
        if (!SpireTogetherMod.isConnected || !StartingItemsPatches.giveRewards
            || AbstractDungeon.isScreenUp || AbstractDungeon.player == null
            || P2PManager.data == null || P2PManager.data.settings == null
            || __instance.event == null || !HEART_EVENT.equals(__instance.event.getClass().getName())) {
            return;
        }
        MapRoomNode current = AbstractDungeon.getCurrMapNode();
        if (current == null || current.room != __instance) return;

        // 涅奥构造时照常登记待发放；换成心脏后，由同一个房间触发。
        // GiveRewards 会消耗联机原有标记，下一帧不会再发一遍。
        StartingItemsPatches.GiveRewards();
    }
}
