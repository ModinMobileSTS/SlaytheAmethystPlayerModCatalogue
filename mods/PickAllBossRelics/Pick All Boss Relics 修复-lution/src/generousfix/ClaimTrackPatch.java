package generousfix;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

/**
 * Remembers which relic was just clicked in the boss relic selection screen so
 * BossScreenUpdatePatch can repair the outcome on the same frame.
 */
@SpirePatch2(clz = AbstractRelic.class, method = "bossObtainLogic", paramtypez = {})
public class ClaimTrackPatch {

    @SpirePostfixPatch
    public static void Postfix(AbstractRelic __instance) {
        if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.BOSS_REWARD) {
            return;
        }
        FixSupport.justClaimed = __instance;
        FixSupport.rememberClaim(__instance.relicId);
    }
}
