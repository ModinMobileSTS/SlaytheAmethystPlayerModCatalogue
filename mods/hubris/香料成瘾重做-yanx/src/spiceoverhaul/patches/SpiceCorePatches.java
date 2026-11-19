package spiceoverhaul.patches;

import com.evacipated.cardcrawl.mod.hubris.relics.Spice;
import com.evacipated.cardcrawl.mod.stslib.actions.tempHp.AddTemporaryHPAction;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.curses.Necronomicurse;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.DexterityPower;
import com.megacrit.cardcrawl.powers.MetallicizePower;
import com.megacrit.cardcrawl.powers.RegenPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import spiceoverhaul.SpiceData;

/** Replaces the core behaviour of the parent mod's Spice relic. */
public class SpiceCorePatches {

    @SpirePatch2(clz = Spice.class, method = "increment", paramtypez = {})
    public static class Increment {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix() {
            SpiceData.gainLayer();
            return SpireReturn.Return();
        }
    }

    /** The rework no longer changes max HP or hand size when Spice is equipped. */
    @SpirePatch2(clz = Spice.class, method = "onEquip", paramtypez = {})
    public static class NoEquip {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix() {
            return SpireReturn.Return();
        }
    }

    @SpirePatch2(clz = Spice.class, method = "onUnequip", paramtypez = {})
    public static class NoUnequip {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix() {
            return SpireReturn.Return();
        }
    }

    @SpirePatch2(clz = Spice.class, method = "atPreBattle", paramtypez = {})
    public static class PreBattle {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(Spice __instance) {
            AbstractPlayer p = AbstractDungeon.player;
            int stacks = __instance.counter;
            if (p == null || stacks < 1) {
                return SpireReturn.Return();
            }
            SpiceData.BattleStats stats = SpiceData.battleStats(stacks);
            __instance.flash();
            if (stats.strength > 0) {
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(
                        p, p, new StrengthPower(p, stats.strength), stats.strength));
            }
            if (stats.dexterity > 0) {
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(
                        p, p, new DexterityPower(p, stats.dexterity), stats.dexterity));
            }
            if (stats.metallicize > 0) {
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(
                        p, p, new MetallicizePower(p, stats.metallicize), stats.metallicize));
            }
            if (stats.regen > 0) {
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(
                        p, p, new RegenPower(p, stats.regen), stats.regen));
            }
            if (stats.tempHp > 0) {
                AbstractDungeon.actionManager.addToBottom(new AddTemporaryHPAction(p, p, stats.tempHp));
            }
            if (SpiceData.curseActive()) {
                AbstractCard curse = new Necronomicurse();
                curse.isInnate = true;
                curse.selfRetain = true;
                AbstractDungeon.actionManager.addToBottom(
                        new MakeTempCardInDrawPileAction(curse, 1, true, true));
            }
            return SpireReturn.Return();
        }
    }

    @SpirePatch2(clz = Spice.class, method = "getUpdatedDescription", paramtypez = {})
    public static class Description {
        @SpirePrefixPatch
        public static SpireReturn<String> Prefix(Spice __instance) {
            return SpireReturn.Return(SpiceData.description(__instance));
        }
    }

    /**
     * Once max HP has bottomed out the parent mod must not turn relic rewards
     * into Spice any more; only campfire crafting can add stacks then.
     */
    @SpirePatch2(clz = Spice.class, method = "replaceWithSpice", paramtypez = {})
    public static class NoSpiceRewardsAtFloor {
        @SpirePostfixPatch
        public static boolean Postfix(boolean __result) {
            return SpiceData.normalSpiceBlocked() ? false : __result;
        }
    }
}
