package TheNecrobinderMod.actions;

// ============================================================
// 修复：原代码 duration = Settings.ACTION_DUR_MED (0.5f)，
// tickDuration 每次减 0.25s，动作仅存活 2 帧。第 0 帧打开
// gridSelectScreen 后，第 1 帧玩家尚未选择就 isDone=true，
// 选牌界面无人处理 → 游戏死锁（卡死）。
// 修复：duration 改为 10.0f，且处理完选择后立即 isDone=true，
// 不再依赖 duration 自然耗尽。
// ============================================================

import TheNecrobinderMod.cards.created.NSoul;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class SeanceAction
        extends AbstractGameAction {
    private AbstractPlayer p = AbstractDungeon.player;
    private AbstractCard c;

    public SeanceAction(int amount) {
        this.setValues(this.p, AbstractDungeon.player, amount);
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.duration = this.startDuration = 10.0f;
        c = new NSoul();
    }

    @Override
    public void update() {
        if (this.duration == this.startDuration) {
            CardGroup tmp = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
            for (AbstractCard card : this.p.drawPile.group) {
                tmp.addToRandomSpot(card);
            }
            tmp.sortAlphabetically(true);
            if (tmp.size() == 0) {
                this.isDone = true;
            } else if (tmp.size() == 1) {
                AbstractCard card = tmp.getTopCard();
                card.unhover();
                card.lighten(true);
                card.setAngle(0.0f);
                card.drawScale = 0.12f;
                card.targetDrawScale = 0.75f;
                card.current_x = CardGroup.DRAW_PILE_X;
                card.current_y = CardGroup.DRAW_PILE_Y;
                int idx = this.p.drawPile.group.indexOf((Object) card);
                if (idx >= 0) {
                    this.p.drawPile.group.set(idx, c.makeStatEquivalentCopy());
                }
                this.isDone = true;
            } else if (tmp.size() <= this.amount) {
                for (int i = 0; i < tmp.size(); ++i) {
                    AbstractCard card;
                    card = tmp.getNCardFromTop(i);
                    card.unhover();
                    card.lighten(true);
                    card.setAngle(0.0f);
                    card.drawScale = 0.12f;
                    card.targetDrawScale = 0.75f;
                    card.current_x = CardGroup.DRAW_PILE_X;
                    card.current_y = CardGroup.DRAW_PILE_Y;
                    int idx = this.p.drawPile.group.indexOf((Object) card);
                    if (idx < 0) continue;
                    this.p.drawPile.group.set(idx, c.makeStatEquivalentCopy());
                }
                this.isDone = true;
            } else {
                if (this.amount == 1) {
                    AbstractDungeon.gridSelectScreen.open(tmp, this.amount, "Choose a card to transform.", false);
                } else {
                    AbstractDungeon.gridSelectScreen.open(tmp, this.amount, "Choose cards to exhaust.", false);
                }
                this.tickDuration();
            }
        } else {
            // 等待玩家完成 grid 选牌
            if (AbstractDungeon.gridSelectScreen.selectedCards.size() != 0) {
                for (AbstractCard card : AbstractDungeon.gridSelectScreen.selectedCards) {
                    card.unhover();
                    int idx = this.p.drawPile.group.indexOf((Object) card);
                    if (idx < 0) continue;
                    this.p.drawPile.group.set(idx, c.makeStatEquivalentCopy());
                }
                AbstractDungeon.gridSelectScreen.selectedCards.clear();
                this.p.hand.refreshHandLayout();
                this.isDone = true;
            } else {
                this.tickDuration();
            }
        }
    }
}
