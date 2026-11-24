package therailgunfix;

import TheRailgun.cards.Taser;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;

import java.util.ArrayList;

@SpirePatch2(clz = Taser.class, method = "use", paramtypez = {AbstractPlayer.class, AbstractMonster.class})
public class TaserBuffFix {
    @SpirePrefixPatch
    public static SpireReturn prefix(Taser __instance, Object[] __args) {
        Object source = __args[0];
        Object target = __args[1];
        if (!(source instanceof AbstractPlayer) || !(target instanceof AbstractMonster)) {
            return SpireReturn.Continue();
        }
        AbstractPlayer p = (AbstractPlayer) source;
        AbstractMonster m = (AbstractMonster) target;
        int damage = __instance.upgraded ? 13 : 10;
        AbstractDungeon.actionManager.addToBottom(new DamageAction(m, new DamageInfo(p, damage), AbstractGameAction.AttackEffect.LIGHTNING));
        if (m.type != AbstractMonster.EnemyType.BOSS) {
            for (AbstractPower power : new ArrayList<>(m.powers)) {
                if (power != null && power.type == AbstractPower.PowerType.BUFF) {
                    AbstractDungeon.actionManager.addToBottom(new RemoveSpecificPowerAction(m, p, power));
                }
            }
        }
        return SpireReturn.Return();
    }
}
