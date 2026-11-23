package gensokyo.togethercompat;

import Gensokyo.powers.act3.KasenMinon;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

/**
 * Extra safety for Gensokyo's special monster fights in a Together in Spire session.
 *
 * "Spell card rule" battles (Reimu, Eternal Rivals, Scarlet Devils, Shinki event)
 * use the Position family of powers, whose lane/orb bookkeeping dereferences
 * AbstractDungeon.player and the current monster group. Kasen's "minion" fight
 * uses KasenMinon, which dereferences the dying minion's id to clear Kasen's
 * two minion slots. Remote players are represented by monsters and their hooks
 * can run before the local room/player is ready, so those dereferences need a
 * guard. Every guard is a no-op in single player and keeps the mechanic intact
 * whenever the combat context is valid.
 */
public final class GensokyoSpecialFightPatches {
    private GensokyoSpecialFightPatches() {
    }

    @SpirePatch2(cls = "Gensokyo.powers.act1.Position", method = "fixOrbPositioning", paramtypez = {})
    public static class SafeSpellCardOrbs {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix() {
            return MultiplayerGuard.hasUnsafeCombatContext() ? SpireReturn.Return() : SpireReturn.<Void>Continue();
        }
    }

    @SpirePatch2(cls = "Gensokyo.powers.act2.RivalPlayerPosition", method = "setUpNewRound", paramtypez = {})
    public static class SafeRivalLaneSetup {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix() {
            return MultiplayerGuard.hasUnsafeCombatContext() ? SpireReturn.Return() : SpireReturn.<Void>Continue();
        }
    }

    @SpirePatch2(cls = "Gensokyo.powers.act3.SistersPlayerPosition", method = "setUpNewRound", paramtypez = {})
    public static class SafeSistersLaneSetup {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix() {
            return MultiplayerGuard.hasUnsafeCombatContext() ? SpireReturn.Return() : SpireReturn.<Void>Continue();
        }
    }

    @SpirePatch2(cls = "Gensokyo.powers.act2.RivalPlayerPosition", method = "fixOrbPositioning", paramtypez = {})
    public static class SafeRivalOrbs {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix() {
            return MultiplayerGuard.hasUnsafeCombatContext() ? SpireReturn.Return() : SpireReturn.<Void>Continue();
        }
    }

    @SpirePatch2(cls = "Gensokyo.powers.act3.SistersPlayerPosition", method = "fixOrbPositioning", paramtypez = {})
    public static class SafeSistersOrbs {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix() {
            return MultiplayerGuard.hasUnsafeCombatContext() ? SpireReturn.Return() : SpireReturn.<Void>Continue();
        }
    }

    /** onRemove also shifts the player horizontally; skip it if the player is already gone. */
    @SpirePatch2(cls = "Gensokyo.powers.act2.RivalPlayerPosition", method = "onRemove", paramtypez = {})
    public static class SafeRivalRemove {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix() {
            boolean unsafe = MultiplayerGuard.isMultiplayer() && AbstractDungeon.player == null;
            return unsafe ? SpireReturn.Return() : SpireReturn.<Void>Continue();
        }
    }

    /** A network-reconstructed minion may have no id, which KasenMinon.onDeath dereferences. */
    @SpirePatch2(cls = "Gensokyo.powers.act3.KasenMinon", method = "onDeath", paramtypez = {})
    public static class SafeKasenMinionDeath {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(KasenMinon __instance) {
            boolean unsafe = MultiplayerGuard.isMultiplayer()
                    && (__instance == null || __instance.owner == null || __instance.owner.id == null);
            return unsafe ? SpireReturn.Return() : SpireReturn.<Void>Continue();
        }
    }

    /** Global block hook walks the monster group for every player that gains block. */
    @SpirePatch2(cls = "Gensokyo.patches.onGainedBlockPatch", method = "TriggerOnGainedBlock",
            paramtypez = {AbstractCreature.class, int.class, float[].class})
    public static class SafeOnGainedBlock {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix() {
            return MultiplayerGuard.hasUnsafeCombatContext() ? SpireReturn.Return() : SpireReturn.<Void>Continue();
        }
    }
}
