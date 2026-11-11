package TheNecrobinderMod.relics;

// ============================================================
// 修复：canSpawn() 使用硬编码 "BoundPhylactery" 作为 relicId，
// 但实际 relicId 是带 mod 前缀的 "TheNecrobinder:BoundPhylactery"，
// 导致 hasRelic 永远返回 false，该遗物永远不出现在商店/精英掉落。
// 改为使用 BoundPhylactery.ID 常量。
// ============================================================

import TheNecrobinderMod.BasicMod;
import TheNecrobinderMod.actions.SummonAction;
import TheNecrobinderMod.character.MyCharacter;
import TheNecrobinderMod.relics.BaseRelic;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class PhylacteryUnbound
        extends BaseRelic {
    private static final String NAME = PhylacteryUnbound.class.getSimpleName();
    public static final String ID = BasicMod.makeID(NAME);
    private static final AbstractRelic.RelicTier RARITY = AbstractRelic.RelicTier.BOSS;
    private static final AbstractRelic.LandingSound SOUND = AbstractRelic.LandingSound.MAGICAL;
    private static final int INIT_AMOUNT = 5;
    private static final int INCREASE_AMOUNT = 2;

    public PhylacteryUnbound() {
        super(ID, NAME, MyCharacter.Meta.CARD_COLOR, RARITY, SOUND);
    }

    @Override
    public void atTurnStartPostDraw() {
        this.addToBot((AbstractGameAction) new SummonAction((AbstractCreature) AbstractDungeon.player, 2));
        this.addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, this));
    }

    @Override
    public void atBattleStart() {
        this.addToBot((AbstractGameAction) new SummonAction((AbstractCreature) AbstractDungeon.player, 5));
        this.addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, this));
    }

    @Override
    public boolean canSpawn() {
        return AbstractDungeon.player.hasRelic(BoundPhylactery.ID);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 5 + this.DESCRIPTIONS[1] + 2 + this.DESCRIPTIONS[2];
    }

    @Override
    public void obtain() {
        if (AbstractDungeon.player.hasRelic(BoundPhylactery.ID)) {
            for (int i = 0; i < AbstractDungeon.player.relics.size(); ++i) {
                if (!((AbstractRelic) AbstractDungeon.player.relics.get(i)).relicId.equals((Object) BoundPhylactery.ID)) continue;
                this.instantObtain(AbstractDungeon.player, i, true);
                return;
            }
        }
        super.obtain();
    }

    @Override
    public AbstractRelic makeCopy() {
        return new PhylacteryUnbound();
    }
}
