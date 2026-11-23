package gensokyo.togethercompat;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.unique.VampireDamageAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.relics.DeadBranch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;

/**
 * Compatibility layer for Gensokyo + Together in Spire.
 *
 * Together in Spire turns remote players into AbstractMonster-derived
 * CharacterEntity objects and runs combat hooks while rooms/monster groups may
 * still be null. Several Gensokyo hooks assume a single local player and a fully
 * initialised monster group, and throw NullPointerException in those situations.
 * During an active multiplayer session these hooks are neutralised; single
 * player behaviour is completely unchanged.
 */
public final class GensokyoMultiplayerPatches {
    private GensokyoMultiplayerPatches() {
    }

    /** Monster group iteration with an unchecked monsters field and id dereference. */
    @SpirePatch2(cls = "Gensokyo.patches.MakeAlliesHalfDead", method = "MakeAlliesHalfDeadReee", paramtypez = GameActionManager.class)
    public static class SafeMakeAlliesHalfDead {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix() {
            return MultiplayerGuard.isMultiplayer() ? SpireReturn.Return() : SpireReturn.<Void>Continue();
        }
    }

    /** Reads AbstractDungeon.getMonsters().monsters and blindly takes element 0. */
    @SpirePatch2(cls = "Gensokyo.patches.ReversalEventPatches$getStartOfBattleStats", method = "getStuff", paramtypez = AbstractPlayer.class)
    public static class SafeReversalStats {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix() {
            return MultiplayerGuard.isMultiplayer() ? SpireReturn.Return() : SpireReturn.<Void>Continue();
        }
    }

    /** Retargets damage onto AbstractDungeon.getMonsters().getRandomMonster(...). */
    @SpirePatch2(cls = "Gensokyo.patches.LunacyRetargetPatch$VampireDamageActionRetarget", method = "ChangeTarget", paramtypez = {VampireDamageAction.class, DamageInfo[].class})
    public static class SafeVampireRetarget {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix() {
            return MultiplayerGuard.isMultiplayer() ? SpireReturn.Return() : SpireReturn.<Void>Continue();
        }
    }

    @SpirePatch2(cls = "Gensokyo.patches.LunacyRetargetPatch$DamageActionRetarget", method = "ChangeTarget", paramtypez = {DamageAction.class, DamageInfo[].class})
    public static class SafeDamageRetarget {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix() {
            return MultiplayerGuard.isMultiplayer() ? SpireReturn.Return() : SpireReturn.<Void>Continue();
        }
    }

    /** Force getRandomMonster to keep its vanilla result during multiplayer. */
    @SpirePatch2(cls = "Gensokyo.patches.RandomHitCertainEnemy$HitThisGuyPatch1", method = "HitThisGuy", paramtypez = {AbstractMonster.class, MonsterGroup.class, AbstractMonster.class, boolean.class, Random.class})
    public static class SafeRandomHit1 {
        @SpirePrefixPatch
        public static SpireReturn<AbstractMonster> Prefix(Object[] __args) {
            if (MultiplayerGuard.isMultiplayer()) {
                return SpireReturn.Return((AbstractMonster) __args[0]);
            }
            return SpireReturn.<AbstractMonster>Continue();
        }
    }

    @SpirePatch2(cls = "Gensokyo.patches.RandomHitCertainEnemy$HitThisGuyPatch2", method = "HitThisGuy", paramtypez = {AbstractMonster.class, MonsterGroup.class, AbstractMonster.class, boolean.class})
    public static class SafeRandomHit2 {
        @SpirePrefixPatch
        public static SpireReturn<AbstractMonster> Prefix(Object[] __args) {
            if (MultiplayerGuard.isMultiplayer()) {
                return SpireReturn.Return((AbstractMonster) __args[0]);
            }
            return SpireReturn.<AbstractMonster>Continue();
        }
    }

    /** Hovered-monster patch touches every player's single-target input. */
    @SpirePatch2(cls = "Gensokyo.patches.MakeAlliesUntargetable", method = "MakeHoveredMonsterNull", paramtypez = {AbstractPlayer.class, AbstractMonster[].class})
    public static class SafeUntargetable {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix() {
            return MultiplayerGuard.isMultiplayer() ? SpireReturn.Return() : SpireReturn.<Void>Continue();
        }
    }

    /** Render hooks dereference AbstractDungeon.player instead of the rendered player. */
    @SpirePatch2(cls = "Gensokyo.patches.InvisiblePlayerPatch$RenderPatch", method = "Prefix", paramtypez = {AbstractPlayer.class, SpriteBatch.class})
    public static class SafeInvisibleRender {
        @SpirePrefixPatch
        public static SpireReturn Prefix() {
            // Make Gensokyo's prefix return Continue so the normal render runs.
            return MultiplayerGuard.isMultiplayer() ? SpireReturn.Return(SpireReturn.Continue()) : SpireReturn.Continue();
        }
    }

    @SpirePatch2(cls = "Gensokyo.patches.DisableDeadBranchPatch$DisablePatch", method = "Prefix", paramtypez = {DeadBranch.class, AbstractCard.class})
    public static class SafeDeadBranch {
        @SpirePrefixPatch
        public static SpireReturn Prefix() {
            return MultiplayerGuard.isMultiplayer() ? SpireReturn.Return(SpireReturn.Continue()) : SpireReturn.Continue();
        }
    }
}
