package goldenglowpatch.cardtweaks;

import Goldenglow.cards.Attack.Telepathy;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

/**
 * 心电感应（Telepathy）调整：
 * 费用 2 -> 1（升级后仍为 1），伤害 15 -> 5，升级后伤害 20 -> 8。
 */
public final class TelepathyPatch {

    private TelepathyPatch() {
    }

    @SpirePatch2(
        cls = "Goldenglow.cards.Attack.Telepathy",
        method = "<ctor>",
        paramtypez = {}
    )
    public static final class CtorPatch {
        private CtorPatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(Telepathy __instance) {
            __instance.cost = 1;
            __instance.costForTurn = 1;
            __instance.baseDamage = 5;
            __instance.damage = 5;
        }
    }

    @SpirePatch2(
        cls = "Goldenglow.cards.Attack.Telepathy",
        method = "limitedUpgrade",
        paramtypez = {}
    )
    public static final class UpgradePatch {
        private UpgradePatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(Telepathy __instance) {
            // 原版升级 +5 伤害，这里改为升级后 8 点
            __instance.baseDamage = 8;
            __instance.damage = 8;
        }
    }
}
