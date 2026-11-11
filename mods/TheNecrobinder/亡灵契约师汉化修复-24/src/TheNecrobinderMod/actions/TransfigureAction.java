package TheNecrobinderMod.actions;

// ============================================================
// 修复：原代码 startDuration = Settings.ACTION_DUR_FAST (0.25f)，
// 导致动作只存活一帧：第0帧打开选牌界面后 tickDuration() 立即
// 将 duration 减到 0 并标记 isDone=true。动作结束后选牌界面
// 无人处理，游戏死锁（卡死）。
// 修复：startDuration 改为 1.0f，留出足够帧数等待玩家选牌。
// 与 StS 原生 SelectCardAction 一致。
// ============================================================

import TheNecrobinderMod.Patches.ReplayGrantPatches;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class TransfigureAction
        extends AbstractGameAction {
    private final AbstractPlayer p = AbstractDungeon.player;

    public TransfigureAction() {
        this.duration = this.startDuration = 1.0f;
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
    }

    @Override
    public void update() {
        if (this.duration == this.startDuration) {
            if (this.p.hand.isEmpty()) {
                this.isDone = true;
                return;
            }
            if (this.p.hand.size() == 1) {
                AbstractCard c = this.p.hand.group.get(0);
                ReplayGrantPatches.grant(c, 1);
                if (c.cost >= 0) {
                    c.updateCost(1);
                }
                this.isDone = true;
                return;
            }
            AbstractDungeon.handCardSelectScreen.open("give Replay", 1, false, false);
            this.tickDuration();
            return;
        }
        if (!AbstractDungeon.handCardSelectScreen.wereCardsRetrieved) {
            this.tickDuration();
            return;
        }
        for (AbstractCard c : AbstractDungeon.handCardSelectScreen.selectedCards.group) {
            ReplayGrantPatches.grant(c, 1);
            if (c.cost >= 0) {
                c.updateCost(1);
            }
            c.superFlash();
            this.p.hand.addToTop(c);
        }
        AbstractDungeon.handCardSelectScreen.selectedCards.clear();
        this.p.hand.refreshHandLayout();
        this.tickDuration();
    }
}
