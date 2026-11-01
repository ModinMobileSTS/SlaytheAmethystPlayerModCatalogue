package dnkmodl10n;

import basemod.BaseMod;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoom;
import java.lang.reflect.Method;

/**
 * Applies the configured multiplier to the gold rewards dropped by battles.
 * The reward amount is scaled before the reward card is shown, so the displayed
 * number and the gold actually granted always match.
 */
public class GoldRewardPatch {

    @SpirePatch2(clz = RewardItem.class, method = "<ctor>", paramtypez = {int.class})
    public static class GoldRewardCtorPatch {
        @SpirePostfixPatch
        public static void Postfix(RewardItem __instance) {
            scaleNewGoldReward(__instance);
        }
    }

    @SpirePatch2(clz = RewardItem.class, method = "incrementGold", paramtypez = {int.class})
    public static class GoldRewardIncrementPatch {
        @SpirePostfixPatch
        public static void Postfix(RewardItem __instance, Object[] __args) {
            if (__instance == null || __args == null || __args.length < 1) {
                return;
            }
            if (!(__args[0] instanceof Integer)) {
                return;
            }
            int gold = (Integer) __args[0];
            if (gold <= 0) {
                return;
            }
            int multiplier = BattleGoldMultiplier.get();
            if (multiplier <= 1 || !isBattleGoldReward(__instance)) {
                return;
            }
            try {
                int before = __instance.goldAmt;
                __instance.goldAmt += gold * (multiplier - 1);
                refreshGoldText(__instance);
                BaseMod.logger.info("[DNKmodL10n] battle gold " + before + " -> " + __instance.goldAmt
                        + " (x" + multiplier + ")");
            } catch (Throwable ignored) {
                // Cosmetic/economy tweak only: never interrupt the reward flow.
            }
        }
    }

    private static void scaleNewGoldReward(RewardItem item) {
        if (item == null || item.goldAmt <= 0) {
            return;
        }
        int multiplier = BattleGoldMultiplier.get();
        if (multiplier <= 1 || !isBattleGoldReward(item)) {
            return;
        }
        try {
            int before = item.goldAmt;
            item.goldAmt *= multiplier;
            refreshGoldText(item);
            BaseMod.logger.info("[DNKmodL10n] battle gold " + before + " -> " + item.goldAmt
                    + " (x" + multiplier + ")");
        } catch (Throwable ignored) {
            // Cosmetic/economy tweak only: never interrupt the reward flow.
        }
    }

    private static boolean isBattleGoldReward(RewardItem item) {
        if (item.type != RewardItem.RewardType.GOLD) {
            return false;
        }
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        if (room == null) {
            return false;
        }
        // Normal, elite and boss fights all live in MonsterRoom; combat events are flagged.
        return room instanceof MonsterRoom || room.combatEvent;
    }

    /**
     * Rebuilds the reward text and the Golden Idol style bonus from the new gold amount.
     */
    private static void refreshGoldText(RewardItem item) {
        try {
            Method applyGoldBonus = RewardItem.class.getDeclaredMethod("applyGoldBonus", boolean.class);
            applyGoldBonus.setAccessible(true);
            applyGoldBonus.invoke(item, false);
        } catch (Throwable ignored) {
            // The reward still grants the scaled gold even if the text cannot be refreshed.
        }
    }
}
