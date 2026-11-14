package cardforge.zh;

import com.eternal0.slay.the.spire.mods.relics.CardGridRelic;
import com.eternal0.slay.the.spire.mods.relics.HammerOfThorRelic;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

/**
 * 雷神之锤改为打开重写后的铸卡界面（{@link ForgeCraftScreen}）。
 * 另外两个遗物（灼热之火、众神之砧）保持原 mod 行为不变。
 */
@SpirePatch2(
        cls = "com.eternal0.slay.the.spire.mods.relics.CardGridRelic",
        method = "onRightClick",
        paramtypez = {}
)
public final class CraftScreenPatch {
    private CraftScreenPatch() {
    }

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(CardGridRelic __instance) {
        if (!(__instance instanceof HammerOfThorRelic)) {
            return SpireReturn.Continue();
        }
        if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.MAP) {
            return SpireReturn.Return();
        }
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        if (room != null && room.phase == AbstractRoom.RoomPhase.COMBAT) {
            return SpireReturn.Return();
        }
        try {
            ForgeCraftScreen.openScreen();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return SpireReturn.Return();
    }
}
