package generousfix;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.screens.select.BossRelicSelectScreen;
import generousrelics.Config;

/**
 * Repairs the two outcomes the parent mod loses when it keeps the boss relic screen open:
 *
 * 1. Vanilla BossRelicSelectScreen.relicObtainLogic gives the player Black Blood /
 *    Ring of the Serpent / Frozen Core / Holy Water (they are excluded from
 *    AbstractRelic.bossObtainLogic and only added by that method). The parent mod
 *    suppresses the whole body, so those relics were never obtained at all.
 * 2. Relics whose onEquip() opens another screen (Empty Cage, Astrolabe, Pandora's Box,
 *    Tiny House, Orrery) only behave correctly once the boss relic screen is closed.
 *    Ending the session here lets onEquip() run against a clean screen stack.
 */
@SpirePatch2(clz = BossRelicSelectScreen.class, method = "update", paramtypez = {})
public class BossScreenUpdatePatch {

    @SpirePostfixPatch
    public static void Postfix(BossRelicSelectScreen __instance) {
        AbstractRelic relic = FixSupport.justClaimed;
        FixSupport.justClaimed = null;
        if (relic == null || !Config.isMultiBossRelicEnabled()) {
            return;
        }
        if (FixSupport.isStarterReplacement(relic.relicId) && !hasRelic(relic)) {
            relic.instantObtain(AbstractDungeon.player, 0, true);
            if (AbstractDungeon.getCurrRoom() != null) {
                AbstractDungeon.getCurrRoom().rewardPopOutTimer = 0.25f;
            }
            FixSupport.finishSession(__instance);
            return;
        }
        if (FixSupport.opensScreen(relic.relicId)) {
            FixSupport.finishSession(__instance);
        }
    }

    private static boolean hasRelic(AbstractRelic relic) {
        return AbstractDungeon.player != null && AbstractDungeon.player.relics.contains(relic);
    }
}
