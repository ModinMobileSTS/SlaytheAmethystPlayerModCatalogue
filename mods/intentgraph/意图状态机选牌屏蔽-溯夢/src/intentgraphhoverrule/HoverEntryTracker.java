package intentgraphhoverrule;

import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

/**
 * 记录「当前这一次悬停是在什么前提下进入的」。
 *
 * <p>触屏模拟器上把牌丢到敌人身上后，模拟指针会停在松手位置，
 * 敌人的 {@code hb.hovered} 一直为 true，看起来就像一直在悬停。
 * 这里只在指针「进入」敌人 hitbox 的那一帧记录前提：
 * 如果进入时玩家正握着牌（选中 / 拖拽 / 瞄准 / 出牌中），
 * 那么这次悬停整体不算数，必须先把指针移开再重新移入。
 *
 * <p>每帧由 {@link FrameTickPatch} 驱动，保证指针离开时状态能及时失效，
 * 不依赖意图状态机是否真的在绘制。
 */
public final class HoverEntryTracker {

    private static AbstractMonster hovering = null;
    private static boolean enteredWhileSelectingCard = false;

    private HoverEntryTracker() {
    }

    /**
     * 每帧刷新一次当前的悬停目标；目标变化即视为一次新的进入。
     */
    public static void refresh() {
        AbstractMonster hovered = findHoveredMonster();
        if (hovered != hovering) {
            hovering = hovered;
            enteredWhileSelectingCard = hovered != null && CardSelectionState.isSelectingCard();
        }
    }

    /**
     * @return 该敌人当前这次悬停是否是从「未选牌状态」进入的
     */
    public static boolean isHoveredSinceIdle(AbstractMonster monster) {
        return monster != null && monster == hovering && !enteredWhileSelectingCard;
    }

    /**
     * 与 Intent Graph 本身一致的悬停判定，保证两边的「当前目标」不会错位。
     */
    public static AbstractMonster findHoveredMonster() {
        if (AbstractDungeon.getCurrMapNode() == null || AbstractDungeon.isScreenUp) {
            return null;
        }
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        if (room == null || room.monsters == null) {
            return null;
        }
        for (AbstractMonster monster : room.monsters.monsters) {
            if (monster.hb.hovered && (!monster.isDeadOrEscaped() || monster.halfDead)) {
                return monster;
            }
        }
        return null;
    }
}
