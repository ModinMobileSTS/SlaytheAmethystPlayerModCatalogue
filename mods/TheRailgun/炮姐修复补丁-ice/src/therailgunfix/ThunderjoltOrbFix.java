package therailgunfix;

import TheRailgun.cards.Thunderjolt;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.Lightning;

@SpirePatch2(clz = Thunderjolt.class, method = "use", paramtypez = {AbstractPlayer.class, AbstractMonster.class})
public class ThunderjoltOrbFix {
    @SpirePrefixPatch
    public static SpireReturn prefix(Thunderjolt __instance, Object[] __args) {
        Object source = __args[0];
        Object target = __args[1];
        if (!(source instanceof AbstractPlayer) || !(target instanceof AbstractCreature)) {
            return SpireReturn.Continue();
        }
        AbstractPlayer p = (AbstractPlayer) source;
        int orbs = p.filledOrbCount();
        for (int i = 0; i < orbs; ++i) {
            AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature) target, new DamageInfo(p, __instance.energyOnUse, __instance.damageTypeForTurn), AbstractGameAction.AttackEffect.LIGHTNING));
        }
        if (__instance.upgraded) {
            AbstractDungeon.actionManager.addToBottom(new ChannelAction(new Lightning()));
        }
        return SpireReturn.Return();
    }
}
