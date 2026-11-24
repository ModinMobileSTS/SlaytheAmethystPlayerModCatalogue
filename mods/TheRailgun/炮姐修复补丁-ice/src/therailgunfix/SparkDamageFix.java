package therailgunfix;

import TheRailgun.cards.Spark;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

@SpirePatch2(clz = Spark.class, method = "use", paramtypez = {AbstractPlayer.class, AbstractMonster.class})
public class SparkDamageFix {
    @SpirePrefixPatch
    public static SpireReturn prefix(Spark __instance, Object[] __args) {
        Object source = __args[0];
        Object target = __args[1];
        if (!(source instanceof AbstractPlayer) || !(target instanceof AbstractCreature)) {
            return SpireReturn.Continue();
        }
        AbstractPlayer p = (AbstractPlayer) source;
        int orbs = p.filledOrbCount();
        int damage = __instance.damage + orbs * (__instance.upgraded ? 2 : 1);
        AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature) target, new DamageInfo(p, damage, __instance.damageTypeForTurn), AbstractGameAction.AttackEffect.NONE));
        return SpireReturn.Return();
    }
}
