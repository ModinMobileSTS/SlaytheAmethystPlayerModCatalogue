package ironcladperks.mobilefix.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import ironcladperks.mobilefix.PerkMobileConfig;
import ironcladperks.perks.PerkEffectApplier;
import ironcladperks.perks.PerkSaveData;

/**
 * The single place every perk bonus flows through.
 *
 * <p>When the player turns the bonus switch off, these prefixes drop the perk effects before they
 * are applied: run start bonuses (HP / gold / potions), combat start powers and the reward
 * extras (card choices, gold scaling, kill heal, extra relic choice). XP and skill points keep
 * working, only the bonuses are paused.</p>
 */
public final class BonusTogglePatches {
    private BonusTogglePatches() {
    }

    @SpirePatch2(
            cls = "ironcladperks.perks.PerkEffectApplier",
            method = "applyRunStartPerks",
            paramtypez = {com.megacrit.cardcrawl.characters.AbstractPlayer.class, PerkSaveData.class}
    )
    public static final class RunStartPerksPatch {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix() {
            return PerkMobileConfig.bonusesEnabled() ? SpireReturn.Continue() : SpireReturn.Return();
        }
    }

    @SpirePatch2(
            cls = "ironcladperks.perks.PerkEffectApplier",
            method = "applyCombatStartPerks",
            paramtypez = {com.megacrit.cardcrawl.characters.AbstractPlayer.class, PerkSaveData.class}
    )
    public static final class CombatStartPerksPatch {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix() {
            return PerkMobileConfig.bonusesEnabled() ? SpireReturn.Continue() : SpireReturn.Return();
        }
    }

    @SpirePatch2(
            cls = "ironcladperks.perks.PerkEffectApplier",
            method = "bonusCardRewardCount",
            paramtypez = {PerkSaveData.class}
    )
    public static final class BonusCardRewardPatch {
        @SpirePrefixPatch
        public static SpireReturn<Integer> Prefix() {
            return PerkMobileConfig.bonusesEnabled() ? SpireReturn.Continue() : SpireReturn.Return(0);
        }
    }

    @SpirePatch2(
            cls = "ironcladperks.perks.PerkEffectApplier",
            method = "killHealAmount",
            paramtypez = {PerkSaveData.class}
    )
    public static final class KillHealPatch {
        @SpirePrefixPatch
        public static SpireReturn<Integer> Prefix() {
            return PerkMobileConfig.bonusesEnabled() ? SpireReturn.Continue() : SpireReturn.Return(0);
        }
    }

    @SpirePatch2(
            cls = "ironcladperks.perks.PerkEffectApplier",
            method = "bonusGoldDropPercent",
            paramtypez = {PerkSaveData.class}
    )
    public static final class BonusGoldDropPatch {
        @SpirePrefixPatch
        public static SpireReturn<Integer> Prefix() {
            return PerkMobileConfig.bonusesEnabled() ? SpireReturn.Continue() : SpireReturn.Return(0);
        }
    }

    @SpirePatch2(
            cls = "ironcladperks.perks.PerkEffectApplier",
            method = "bonusRelicChoiceCount",
            paramtypez = {PerkSaveData.class}
    )
    public static final class BonusRelicChoicePatch {
        @SpirePrefixPatch
        public static SpireReturn<Integer> Prefix() {
            return PerkMobileConfig.bonusesEnabled() ? SpireReturn.Continue() : SpireReturn.Return(0);
        }
    }
}
