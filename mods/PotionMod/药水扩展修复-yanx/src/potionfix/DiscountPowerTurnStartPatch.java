package potionfix;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import potionmod.powers.DiscountPower;

/**
 * 原版 DiscountPower.onUseCard 只会 flash()，卡牌价格没有任何变化（描述写着“所有卡牌消费-1”）。
 * 这里在玩家回合开始、抽牌结束之后，把手上所有牌的本次费用各降低 1 点；
 * 降费目标直接按基础费用计算，重复触发也不会叠加，费用不会被降成负数。
 */
@SpirePatch2(
    cls = "com.megacrit.cardcrawl.core.AbstractCreature",
    method = "applyStartOfTurnPostDrawPowers",
    paramtypez = {}
)
public final class DiscountPowerTurnStartPatch {
    private DiscountPowerTurnStartPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(AbstractCreature __instance) {
        if (__instance == null || AbstractDungeon.player == null || __instance != AbstractDungeon.player) {
            return;
        }
        if (!AbstractDungeon.player.hasPower(DiscountPower.POWER_ID)) {
            return;
        }
        for (AbstractCard card : AbstractDungeon.player.hand.group) {
            if (card == null || card.cost < 0) {
                continue;
            }
            int targetCost = Math.max(0, card.cost - 1);
            if (card.costForTurn > targetCost) {
                card.costForTurn = targetCost;
                card.isCostModifiedForTurn = true;
            }
        }
    }
}
