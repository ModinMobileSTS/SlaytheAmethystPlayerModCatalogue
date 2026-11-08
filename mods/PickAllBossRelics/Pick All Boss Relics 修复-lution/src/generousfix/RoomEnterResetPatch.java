package generousfix;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.rooms.TreasureRoomBoss;

/**
 * The parent mod clears its "selection active" flag only from its own confirm/cancel
 * handlers. Pressing the proceed button (which vanilla shows once a boss relic lands)
 * leaves the flag set, and the next time any screen is closed inside a boss chest room
 * the parent reopened that stale selection - with the previous act's relic list.
 * Resetting on room entry keeps every boss chest selection self-contained.
 */
@SpirePatch2(clz = TreasureRoomBoss.class, method = "onPlayerEntry", paramtypez = {})
public class RoomEnterResetPatch {

    @SpirePostfixPatch
    public static void Postfix() {
        FixSupport.justClaimed = null;
        FixSupport.clearClaims();
        FixSupport.resetParentState();
    }
}
