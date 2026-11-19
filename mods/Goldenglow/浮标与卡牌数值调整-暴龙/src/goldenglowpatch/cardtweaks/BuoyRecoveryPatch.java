package goldenglowpatch.cardtweaks;

import Goldenglow.cards.Power.BuoyRecovery;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

/**
 * 浮标回收（BuoyRecovery）调整：
 * 费用 2 -> 1，升级后费用 1 -> 0。
 */
public final class BuoyRecoveryPatch {

    private BuoyRecoveryPatch() {
    }

    @SpirePatch2(
        cls = "Goldenglow.cards.Power.BuoyRecovery",
        method = "<ctor>",
        paramtypez = {}
    )
    public static final class CtorPatch {
        private CtorPatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(BuoyRecovery __instance) {
            __instance.cost = 1;
            __instance.costForTurn = 1;
        }
    }

    @SpirePatch2(
        cls = "Goldenglow.cards.Power.BuoyRecovery",
        method = "limitedUpgrade",
        paramtypez = {}
    )
    public static final class UpgradePatch {
        private UpgradePatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(BuoyRecovery __instance) {
            // 原版 limitedUpgrade 调用 upgradeBaseCost(1)，这里再降到 0，
            // 并保留本回合手牌上已存在的费用增减
            int diff = __instance.costForTurn - __instance.cost;
            __instance.cost = 0;
            if (__instance.costForTurn > 0) {
                __instance.costForTurn = Math.max(0, diff);
            }
        }
    }
}
