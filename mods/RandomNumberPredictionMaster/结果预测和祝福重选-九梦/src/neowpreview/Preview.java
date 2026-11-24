package neowpreview;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.relics.AbstractRelic;

import java.util.ArrayList;
import java.util.List;

/** 某个涅奥祝福选项的具体结果预告。 */
public class Preview {

    /** 会得到的卡牌（多张时按抽到的顺序，用于三选一）。 */
    public final List<AbstractCard> cards = new ArrayList<>();
    /** 会得到的遗物。 */
    public AbstractRelic relic;
    /** 会得到的药水。 */
    public final List<AbstractPotion> potions = new ArrayList<>();
    /** 这个选项的代价是随机诅咒时，具体是哪张诅咒。 */
    public AbstractCard curse;
}
