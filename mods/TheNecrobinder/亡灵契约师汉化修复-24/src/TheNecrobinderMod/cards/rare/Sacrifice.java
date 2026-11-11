package TheNecrobinderMod.cards.rare;

// ============================================================
// 修复：use() 方法在 Osty 未存活时调用 OstyManager.get().maxHealth
// 导致 NPE 崩溃。加入 isAlive() 判断，Osty 不存活时打出无效果。
// ============================================================

import TheNecrobinderMod.OstyManager;
import TheNecrobinderMod.actions.KillOstyAction;
import TheNecrobinderMod.cards.BaseCard;
import TheNecrobinderMod.character.MyCharacter;
import TheNecrobinderMod.util.CardStats;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Sacrifice
        extends BaseCard {
    public static final String ID = Sacrifice.makeID(Sacrifice.class.getSimpleName());
    private static final CardStats info = new CardStats(MyCharacter.Meta.CARD_COLOR, AbstractCard.CardType.SKILL, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.SELF, 1);
    private String appended = "";

    public Sacrifice() {
        super(ID, info);
        this.setSelfRetain(true);
        this.setCostUpgrade(0);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (OstyManager.isAlive()) {
            int ostyHealth = OstyManager.get().maxHealth;
            this.addToBot(new KillOstyAction());
            this.addToBot(new GainBlockAction(p, ostyHealth * 3));
        }
    }

    private void refreshDescription() {
        this.refreshDescription(false);
    }

    private void refreshDescription(boolean forceHide) {
        String base = this.rawDescription;
        if (!this.appended.isEmpty() && base.endsWith(this.appended)) {
            base = base.substring(0, base.length() - this.appended.length());
        }
        boolean show = !forceHide && AbstractDungeon.player != null && AbstractDungeon.player.hand.contains(this) && OstyManager.isAlive();
        this.appended = show ? this.cardStrings.EXTENDED_DESCRIPTION[0] + OstyManager.get().maxHealth * 3 + this.cardStrings.EXTENDED_DESCRIPTION[1] : "";
        this.rawDescription = base + this.appended;
        this.initializeDescription();
    }

    @Override
    public void applyPowers() {
        super.applyPowers();
        this.refreshDescription();
    }

    @Override
    public void onMoveToDiscard() {
        this.refreshDescription(true);
    }

    @Override
    public void triggerOnExhaust() {
        this.refreshDescription(true);
    }

    @Override
    public AbstractCard makeCopy() {
        return new Sacrifice();
    }
}
