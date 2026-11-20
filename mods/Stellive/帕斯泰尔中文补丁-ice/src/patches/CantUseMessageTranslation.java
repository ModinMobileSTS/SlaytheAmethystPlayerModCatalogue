package patches;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;

final class CantUseMessageTranslation {
    private static final String DIVINITY_SHORTAGE = "\ubd80\ud0a4\uc758 \uc2e0\uc131\ub825\uc774 \ucda9\ubd84\ud558\uc9c0 \uc54a\uc2b5\ub2c8\ub2e4.";
    private static final String MANA_SHORTAGE = "\ub9ac\ucf54\uc758 \ub9c8\ub825\uc774 \ucda9\ubd84\ud558\uc9c0 \uc54a\uc558\uc2b5\ub2c8\ub2e4.";
    private static final String CHEONGCHO_ATTACK_BLOCK = "\uccad\ucd08\uc758 \uae30\uc6b4\uc774 \uac15\ud574\uc838 \uacf5\uaca9\ud560 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4!";

    private CantUseMessageTranslation() {
    }

    static void translate(AbstractCard card) {
        if (Settings.language != Settings.GameLanguage.ZHS || card == null || card.cantUseMessage == null) return;
        if (DIVINITY_SHORTAGE.equals(card.cantUseMessage)) {
            card.cantUseMessage = "神性力不足，无法使用这张牌。";
        } else if (MANA_SHORTAGE.equals(card.cantUseMessage)) {
            card.cantUseMessage = "莉可的魔力不足，无法使用这张牌。";
        } else if (CHEONGCHO_ATTACK_BLOCK.equals(card.cantUseMessage)) {
            card.cantUseMessage = "青脑草的力量增强，无法进行攻击！";
        }
    }
}
