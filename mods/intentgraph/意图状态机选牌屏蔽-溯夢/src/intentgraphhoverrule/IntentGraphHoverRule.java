package intentgraphhoverrule;

import com.megacrit.cardcrawl.monsters.AbstractMonster;

/**
 * 意图状态机的悬停触发规则（唯一决策点）。
 *
 * <p>规则一：没有选中任何卡牌时，鼠标悬停敌人 → 触发（由 Intent Graph 正常绘制）。
 * <br>规则二：处于卡牌选中状态时，鼠标悬停敌人 → 屏蔽，不绘制意图状态机。
 * <br>规则三：一次悬停必须是从「未选牌状态」进入的才算有效。
 * 指针停在敌人身上不动（例如触屏上把牌丢过去后指针残留）不会触发，
 * 必须先把指针移出敌人再重新移入。
 *
 * <p>规则每帧重新计算，因此悬停过程中如果玩家选中 / 取消选中卡牌，
 * 状态机会在同一帧内立即隐藏或恢复，不需要额外的计时器或缓存。
 */
public final class IntentGraphHoverRule {

    private IntentGraphHoverRule() {
    }

    /**
     * @return true 表示本帧允许为这个敌人显示意图状态机
     */
    public static boolean isGraphAllowed(AbstractMonster monster) {
        if (CardSelectionState.isSelectingCard()) {
            return false;
        }
        return HoverEntryTracker.isHoveredSinceIdle(monster);
    }
}
