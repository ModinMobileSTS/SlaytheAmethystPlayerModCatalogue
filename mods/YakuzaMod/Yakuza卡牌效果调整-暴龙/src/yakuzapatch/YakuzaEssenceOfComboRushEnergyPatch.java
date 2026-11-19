package yakuzapatch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import theYakuza.cards.CommonAttacks.YakuzaEssenceOfComboRush;
import theYakuza.powers.HeatLevelPower;

/**
 * 「冲刺连击」/「冲刺连击+」：有热血值时获得的费用由 1 点改为 2 点，并同步卡牌描述。
 */
public final class YakuzaEssenceOfComboRushEnergyPatch {
    private YakuzaEssenceOfComboRushEnergyPatch() {
    }

    @SpirePatch2(cls = "theYakuza.cards.CommonAttacks.YakuzaEssenceOfComboRush", method = "<ctor>", paramtypez = {})
    public static final class CtorPatch {
        private CtorPatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(YakuzaEssenceOfComboRush __instance) {
            String desc = __instance.rawDescription;
            desc = desc.replace("获得 [E]", "获得 2 [E]");
            desc = desc.replace("Gain [E]", "Gain 2 [E]");
            __instance.rawDescription = desc;
            __instance.initializeDescription();
        }
    }

    @SpirePatch2(cls = "theYakuza.cards.CommonAttacks.YakuzaEssenceOfComboRush", method = "use",
        paramtypez = {AbstractPlayer.class, AbstractMonster.class})
    public static final class UsePatch {
        private UsePatch() {
        }

        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(YakuzaEssenceOfComboRush __instance, Object[] __args) {
            AbstractPlayer p = (AbstractPlayer) __args[0];
            AbstractMonster m = (AbstractMonster) __args[1];
            AbstractDungeon.actionManager.addToBottom(new DamageAction(m,
                new DamageInfo(p, __instance.damage, __instance.damageTypeForTurn),
                AbstractGameAction.AttackEffect.BLUNT_HEAVY));
            if (p.hasPower(HeatLevelPower.POWER_ID)) {
                AbstractDungeon.actionManager.addToBottom(new GainEnergyAction(2));
            }
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(p, p, new HeatLevelPower(p, p, 1)));
            return SpireReturn.Return();
        }
    }
}
