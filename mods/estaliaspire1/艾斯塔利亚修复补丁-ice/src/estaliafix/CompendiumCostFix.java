package estaliafix;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.helpers.CardLibrary;

/**
 * Estalia 卡牌没有对应的解锁记录，CardLibrary.add 会将其标记为未见过。
 * AbstractCard.renderEnergy 会因此跳过百科中的能量球和费用文字。
 */
@SpirePatch2(
        clz = CardLibrary.class,
        method = "add",
        paramtypez = {AbstractCard.class}
)
public class CompendiumCostFix {
    @SpirePostfixPatch
    public static void postfix(AbstractCard card) {
        if (card instanceof estalia.Card) {
            card.isSeen = true;
        }
    }
}
