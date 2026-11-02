package TheNecrobinderMod.powers.buffs;

// ============================================================
// 修复点：能量图标 [R]（红色能量，仅铁甲战士能量条渲染）
// 改为 [E]（通用能量图标），与卡面文本一致，纯显示修复。
// ============================================================

import TheNecrobinderMod.BasicMod;
import TheNecrobinderMod.powers.BasePower;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.localization.LocalizedStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class DemesnePower
        extends BasePower {
    public static final String POWER_ID = BasicMod.makeID(DemesnePower.class.getSimpleName());
    private static final AbstractPower.PowerType TYPE = AbstractPower.PowerType.BUFF;
    private static final boolean TURN_BASED = false;

    public DemesnePower(AbstractCreature owner, int amount) {
        super(POWER_ID, TYPE, false, owner, amount);
    }

    public void updateDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.DESCRIPTIONS[0]);
        sb.append(this.amount);
        if (this.amount == 1) {
            sb.append(this.DESCRIPTIONS[1]);
        } else if (this.amount > 1) {
            sb.append(this.DESCRIPTIONS[2]);
        }
        for (int i = 0; i < this.amount; ++i) {
            sb.append("[E] ");
        }
        sb.append(LocalizedStrings.PERIOD);
        this.description = sb.toString();
    }

    public void atStartOfTurn() {
        this.addToBot((AbstractGameAction) new GainEnergyAction(this.amount));
        this.addToBot((AbstractGameAction) new DrawCardAction(this.amount));
        this.flash();
    }
}
