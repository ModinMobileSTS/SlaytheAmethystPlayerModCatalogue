package TheNecrobinderMod.cards.rare;

// ============================================================
// 修复：原代码 numEthereal 初始为 0，在卡牌首次进入手牌时
// applyPowers() 会将本战斗已打出的所有有飘牌追溯降价。
// 修复：numEthereal 初始为 -1（未初始化）。首次 applyPowers 时
// 仅记录当前基准值不降价；之后每多一张有飘牌才触发降价。
// ============================================================

import TheNecrobinderMod.cards.BaseCard;
import TheNecrobinderMod.character.MyCharacter;
import TheNecrobinderMod.util.CardStats;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class BansheesCry
        extends BaseCard {
    public static final String ID = BansheesCry.makeID(BansheesCry.class.getSimpleName());
    private static final CardStats info = new CardStats(MyCharacter.Meta.CARD_COLOR, AbstractCard.CardType.ATTACK, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.ALL_ENEMY, 9);
    private int numEthereal = -1;
    private static final int DAMAGE = 33;
    private static final int MAGIC = 2;

    public BansheesCry() {
        super(ID, info);
        this.setDamage(33);
        this.setMagic(2);
        this.isMultiDamage = true;
    }

    @Override
    public void applyPowers() {
        super.applyPowers();
        int actualEthereal = 0;
        for (AbstractCard card : AbstractDungeon.actionManager.cardsPlayedThisCombat) {
            if (!card.isEthereal) continue;
            ++actualEthereal;
        }
        if (this.numEthereal < 0) {
            // 首次进入手牌/刚创建，仅记录基准不降价
            this.numEthereal = actualEthereal;
        } else if (actualEthereal > this.numEthereal) {
            this.updateCost((this.numEthereal - actualEthereal) * 2);
            this.numEthereal = actualEthereal;
        }
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            if (this.cost < 9) {
                this.upgradeBaseCost(this.cost - 2);
                if (this.cost < 0) {
                    this.cost = 0;
                }
            } else {
                this.upgradeBaseCost(7);
            }
        }
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new DamageAllEnemiesAction((AbstractCreature) p, this.multiDamage, this.damageTypeForTurn, AbstractGameAction.AttackEffect.SLASH_HEAVY));
    }

    @Override
    public AbstractCard makeCopy() {
        return new BansheesCry();
    }
}
