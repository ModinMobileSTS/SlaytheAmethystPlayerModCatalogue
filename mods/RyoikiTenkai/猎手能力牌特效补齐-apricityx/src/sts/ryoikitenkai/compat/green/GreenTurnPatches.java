package sts.ryoikitenkai.compat.green;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.InfiniteBladesPower;
import com.megacrit.cardcrawl.powers.RetainCardPower;
import com.megacrit.cardcrawl.powers.ToolsOfTheTradePower;

/**
 * 三个能力的回合触发视觉。
 * <p>
 * 只挂能力自己的钩子（回合开始/回合末），而不是每帧的 update：
 * 钩子本身就和实际效果同一个拍子，画面上「动作发生」与「特效出现」是同步的。
 */
public final class GreenTurnPatches {
    private GreenTurnPatches() {
    }

    /** 无限刀刃：回合开始往手里塞小刀。 */
    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.powers.InfiniteBladesPower",
        method = "atStartOfTurn",
        paramtypez = {}
    )
    public static class InfiniteBladesTurn {
        @SpirePostfixPatch
        public static void Postfix(InfiniteBladesPower __instance) {
            if (__instance.owner == null || !__instance.owner.isPlayer) {
                return;
            }
            // 原版这段逻辑在敌人已全灭时不会给刀，画面也别抢戏
            if (AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
                return;
            }
            GreenEffectRegistry.bladeGained(__instance.owner);
        }
    }

    /** 工具贸易：回合开始抽一张、弃一张。 */
    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.powers.ToolsOfTheTradePower",
        method = "atStartOfTurnPostDraw",
        paramtypez = {}
    )
    public static class ToolsTradeTurn {
        @SpirePostfixPatch
        public static void Postfix(ToolsOfTheTradePower __instance) {
            if (__instance.owner == null || !__instance.owner.isPlayer) {
                return;
            }
            GreenEffectRegistry.toolsUpkeep(__instance.owner);
        }
    }

    /** 蓄势待发：回合末打开保留手牌的选择界面。 */
    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.powers.RetainCardPower",
        method = "atEndOfTurn",
        paramtypez = { boolean.class }
    )
    public static class RetainTurn {
        @SpirePostfixPatch
        public static void Postfix(RetainCardPower __instance, Object[] __args) {
            if (__instance.owner == null || !__instance.owner.isPlayer) {
                return;
            }
            if (!Boolean.TRUE.equals(__args[0])) {
                return;
            }
            // 复刻原版条件：手牌为空、已有符文金字塔/平衡时不弹选择界面，也就不播特效
            if (AbstractDungeon.player == null || AbstractDungeon.player.hand.isEmpty()) {
                return;
            }
            if (AbstractDungeon.player.hasRelic("Runic Pyramid") || AbstractDungeon.player.hasPower("Equilibrium")) {
                return;
            }
            GreenEffectRegistry.retainTurn(__instance.owner);
        }
    }
}
