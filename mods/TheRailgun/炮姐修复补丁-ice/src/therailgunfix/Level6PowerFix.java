package therailgunfix;

import TheRailgun.cards.Level_6;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.DexterityPower;
import com.megacrit.cardcrawl.powers.StrengthPower;

@SpirePatch2(clz = Level_6.class, method = "use", paramtypez = {AbstractPlayer.class, AbstractMonster.class})
public class Level6PowerFix {
    @SpirePrefixPatch
    public static SpireReturn prefix(Level_6 __instance, Object[] __args) {
        Object source = __args[0];
        if (!(source instanceof AbstractPlayer)) {
            return SpireReturn.Continue();
        }
        AbstractPlayer p = (AbstractPlayer) source;
        AbstractDungeon.actionManager.addToTop(new ApplyPowerAction(p, p, new StrengthPower(p, 6), 6));
        AbstractDungeon.actionManager.addToTop(new ApplyPowerAction(p, p, new DexterityPower(p, 6), 6));
        AbstractDungeon.actionManager.addToTop(new GainEnergyAction(6));
        if (__instance.upgraded) {
            p.heal(p.maxHealth / 5);
        }
        return SpireReturn.Return();
    }
}
