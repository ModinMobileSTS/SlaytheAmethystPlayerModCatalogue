package generousfix;

import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.TreasureRoomBoss;
import com.megacrit.cardcrawl.screens.select.BossRelicSelectScreen;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared state and helpers for the Pick All Boss Relics fix patch.
 *
 * The parent mod keeps the boss relic selection screen open (its
 * BossRelicPatch.KeepScreenOpenPatch suppresses BossRelicSelectScreen.relicObtainLogic),
 * which breaks the vanilla sequence for relics that change the screen. Everything here
 * works from outside the parent mod: the only thing touched directly is its private
 * "selection still active" bookkeeping, which is cleared so it cannot reopen a stale
 * boss relic screen later.
 */
public final class FixSupport {

    /** Relics whose onEquip() opens another screen (grid / confirmation grid / reward screen). */
    private static final Set<String> SCREEN_RELICS = new HashSet<String>(Arrays.asList(
            "Empty Cage",
            "Astrolabe",
            "Pandora's Box",
            "Tiny House",
            "Orrery"
    ));

    /** Relics that vanilla handles by replacing the slot 0 starting relic. */
    private static final Set<String> STARTER_REPLACEMENTS = new HashSet<String>(Arrays.asList(
            "Black Blood",
            "Ring of the Serpent",
            "Frozen Core",
            "Holy Water"
    ));

    /** Relic ids obtained during the current boss relic selection (protected from cancellation). */
    private static final Set<String> CLAIMED = new HashSet<String>();

    /** Relic clicked in the boss relic screen during the frame currently being updated. */
    public static AbstractRelic justClaimed = null;

    private FixSupport() {
    }

    public static boolean opensScreen(String relicId) {
        return relicId != null && SCREEN_RELICS.contains(relicId);
    }

    public static boolean isStarterReplacement(String relicId) {
        return relicId != null && STARTER_REPLACEMENTS.contains(relicId);
    }

    public static void rememberClaim(String relicId) {
        if (relicId != null) {
            CLAIMED.add(relicId);
        }
    }

    /** Returns true once for a relic that was obtained during this selection. */
    public static boolean consumeClaim(String relicId) {
        return relicId != null && CLAIMED.remove(relicId);
    }

    public static void clearClaims() {
        CLAIMED.clear();
    }

    /**
     * Clears the parent mod's "boss selection is active" state, so its
     * AbstractDungeon.closeCurrentScreen postfix cannot reopen a stale selection screen.
     */
    public static void resetParentState() {
        try {
            Class<?> clazz = Class.forName("generousrelics.patches.BossRelicPatch");
            Field active = clazz.getDeclaredField("bossSelectionActive");
            active.setAccessible(true);
            active.setBoolean(null, false);
            Field claimed = clazz.getDeclaredField("claimedRelics");
            claimed.setAccessible(true);
            Object list = claimed.get(null);
            if (list instanceof List) {
                ((List<?>) list).clear();
            }
        } catch (Throwable ignored) {
            // The parent mod is a hard dependency; if its internals ever change, the fix
            // for the remaining issues must keep working instead of crashing the game.
        }
    }

    /**
     * Ends the multi-pick session while keeping every relic the player already obtained.
     * Closing the screen here (instead of leaving it open under a nested screen) lets
     * the relic's onEquip() run against a clean screen stack, exactly like vanilla.
     */
    public static boolean finishSession(BossRelicSelectScreen screen) {
        clearClaims();
        resetParentState();
        justClaimed = null;
        if (screen == null) {
            return false;
        }
        if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.BOSS_REWARD) {
            return false;
        }
        if (AbstractDungeon.getCurrRoom() instanceof TreasureRoomBoss) {
            ((TreasureRoomBoss) AbstractDungeon.getCurrRoom()).choseRelic = true;
        }
        screen.touchRelic = null;
        screen.touchBlight = null;
        if (screen.relics != null) {
            screen.relics.clear();
        }
        if (screen.confirmButton != null) {
            screen.confirmButton.hide();
        }
        AbstractDungeon.previousScreen = null;
        AbstractDungeon.closeCurrentScreen();
        if (AbstractDungeon.getCurrRoom() != null) {
            AbstractDungeon.getCurrRoom().rewardPopOutTimer = 0.25f;
        }
        AbstractDungeon.overlayMenu.proceedButton.hide();
        return true;
    }
}
