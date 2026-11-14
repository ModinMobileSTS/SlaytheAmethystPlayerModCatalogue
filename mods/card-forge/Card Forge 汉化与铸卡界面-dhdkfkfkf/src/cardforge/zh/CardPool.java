package cardforge.zh;

import com.megacrit.cardcrawl.cards.AbstractCard;

/** 一个可选的卡池：角色颜色卡池，或 color 为 null 的“全部卡牌”。 */
final class CardPool {
    final String label;
    final AbstractCard.CardColor color;

    CardPool(String label, AbstractCard.CardColor color) {
        this.label = label;
        this.color = color;
    }

    boolean accepts(AbstractCard card) {
        return color == null || card.color == color;
    }
}
