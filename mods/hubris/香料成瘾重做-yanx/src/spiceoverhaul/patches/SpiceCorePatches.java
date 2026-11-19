package spiceoverhaul.patches;

import com.evacipated.cardcrawl.mod.hubris.relics.Spice;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.DexterityPower;
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
            __instance.flash();
            int str = SpiceData.strength(stacks);
            int dex = SpiceData.dexterity(stacks);
            if (str > 0) {
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(
                        p, p, new StrengthPower(p, str), str));
            }
            if (dex > 0) {
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(
                        p, p, new DexterityPower(p, dex), dex));
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
}
