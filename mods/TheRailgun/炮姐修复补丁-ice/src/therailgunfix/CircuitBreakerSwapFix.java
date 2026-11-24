package therailgunfix;

import TheRailgun.cards.Circuit_Breaker;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.DexterityPower;
import com.megacrit.cardcrawl.powers.StrengthPower;

@SpirePatch2(clz = Circuit_Breaker.class, method = "use", paramtypez = {AbstractPlayer.class, AbstractMonster.class})
public class CircuitBreakerSwapFix {
    @SpirePrefixPatch
    public static SpireReturn prefix(Object[] __args) {
        Object source = __args[0];
        if (!(source instanceof AbstractPlayer)) {
            return SpireReturn.Continue();
        }
        AbstractPlayer p = (AbstractPlayer) source;
        int strength = p.hasPower("Strength") ? p.getPower("Strength").amount : 0;
        int dexterity = p.hasPower("Dexterity") ? p.getPower("Dexterity").amount : 0;
        if (strength == dexterity) {
            return SpireReturn.Return();
        }
        if (dexterity > strength) {
            int gain = dexterity - strength;
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(p, p, new StrengthPower(p, gain), gain));
        } else {
            AbstractDungeon.actionManager.addToBottom(new ReducePowerAction(p, p, "Strength", strength - dexterity));
        }
        if (strength > dexterity) {
            int gain = strength - dexterity;
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(p, p, new DexterityPower(p, gain), gain));
        } else {
            AbstractDungeon.actionManager.addToBottom(new ReducePowerAction(p, p, "Dexterity", dexterity - strength));
        }
        return SpireReturn.Return();
    }
}
