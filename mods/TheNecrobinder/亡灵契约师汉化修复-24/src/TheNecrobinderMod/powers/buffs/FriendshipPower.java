package TheNecrobinderMod.powers.buffs;

// ============================================================
// 修复点：能量图标 [R]（红色能量，仅铁甲战士能量条渲染）
// 改为 [E]（通用能量图标），纯显示修复。
// ============================================================

import TheNecrobinderMod.BasicMod;
import TheNecrobinderMod.powers.BasePower;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.localization.LocalizedStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class FriendshipPower
        extends BasePower {
    public static final String POWER_ID = BasicMod.makeID(FriendshipPower.class.getSimpleName());
    private static final AbstractPower.PowerType TYPE = AbstractPower.PowerType.BUFF;
    private static final boolean TURN_BASED = false;

    public FriendshipPower(AbstractCreature owner, int amount) {
        super(POWER_ID, TYPE, false, owner, amount);
    }

    public void updateDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.DESCRIPTIONS[0]);
        for (int i = 0; i < this.amount; ++i) {
            sb.append("[E] ");
        }
        sb.append(LocalizedStrings.PERIOD);
        this.description = sb.toString();
    }

    public void atStartOfTurn() {
        this.addToBot((AbstractGameAction) new GainEnergyAction(this.amount));
        this.flash();
    }
}
