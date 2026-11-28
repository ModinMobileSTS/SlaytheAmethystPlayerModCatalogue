package intentgraphhoverrule;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

/**
 * 只负责读取一件事：玩家此刻是否处于「选中卡牌」的战斗状态。
 *
 * <p>判定的依据来自原版 {@link AbstractPlayer} 的出牌流程：
 * <ul>
 *     <li>{@code inSingleTargetMode}：卡牌已选中并进入指向敌人的瞄准状态
 *         （鼠标拖到敌人身上、或键盘/手柄确认单体目标卡时置位）。</li>
 *     <li>{@code isDraggingCard}：手牌被按住拖出。</li>
 *     <li>{@code cardInUse}：卡牌已经进入打出流程。</li>
 *     <li>手牌自身的 {@code isSelected}：原版选牌界面 / 特殊选牌效果标记的卡牌。</li>
 * </ul>
 *
 * <p>这里刻意不读取 {@code hoveredCard}：原版在手牌被普通悬停时也会把它置位，
 * 不能代表玩家已经选了牌，容易造成误屏蔽。
 */
public final class CardSelectionState {

    private CardSelectionState() {
    }

    /**
     * @return 是否存在正在被选中 / 拖拽 / 瞄准 / 打出的卡牌
     */
    public static boolean isSelectingCard() {
        AbstractPlayer player = AbstractDungeon.player;
        if (player == null) {
            return false;
        }
        if (player.inSingleTargetMode) {
            return true;
        }
        if (player.isDraggingCard) {
            return true;
        }
        if (player.cardInUse != null) {
            return true;
        }
        return hasMarkedCardInHand(player);
    }

    private static boolean hasMarkedCardInHand(AbstractPlayer player) {
        if (player.hand == null) {
            return false;
        }
        // CardGroup.group 在游戏字节码里是裸 ArrayList，这里按 Object 取用再判断类型。
        for (Object entry : player.hand.group) {
            if (entry instanceof AbstractCard && ((AbstractCard) entry).isSelected) {
                return true;
            }
        }
        return false;
    }
}
