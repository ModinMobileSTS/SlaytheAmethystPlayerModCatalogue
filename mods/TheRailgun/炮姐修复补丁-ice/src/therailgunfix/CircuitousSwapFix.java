package therailgunfix;

import TheRailgun.cards.Circuitous;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.DexterityPower;

@SpirePatch2(clz = Circuitous.class, method = "use", paramtypez = {AbstractPlayer.class, AbstractMonster.class})
public class CircuitousSwapFix {
    @SpirePrefixPatch
    public static SpireReturn prefix(Object[] __args) {
        Object source = __args[0];
        Object target = __args[1];
        if (!(source instanceof AbstractPlayer) || !(target instanceof AbstractMonster)) {
            return SpireReturn.Continue();
        }
        AbstractPlayer p = (AbstractPlayer) source;
        AbstractMonster m = (AbstractMonster) target;
        int playerDex = p.hasPower("Dexterity") ? p.getPower("Dexterity").amount : 0;
        int monsterDex = m.hasPower("Dexterity") ? m.getPower("Dexterity").amount : 0;
        if (playerDex == monsterDex) {
            return SpireReturn.Return();
        }
        if (monsterDex > playerDex) {
            int gain = monsterDex - playerDex;
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(p, p, new DexterityPower(p, gain), gain));
        } else {
            AbstractDungeon.actionManager.addToBottom(new ReducePowerAction(p, p, "Dexterity", playerDex - monsterDex));
        }
        if (playerDex > monsterDex) {
            int gain = playerDex - monsterDex;
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(m, p, new DexterityPower(m, gain), gain));
        } else {
            AbstractDungeon.actionManager.addToBottom(new ReducePowerAction(m, p, "Dexterity", monsterDex - playerDex));
        }
        return SpireReturn.Return();
    }
}
