package challengethespire.fix;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.screens.DungeonMapScreen;
import challengethespire.presentation.RushModFlowPatch;
import challengeTheSpire.MonsterRoomBossRush;
import challengeTheSpire.MonsterRoomEliteHunting;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Fixes for Challenge The Spire's Elite Rush / Boss Rush challenges.
 *
 * <p>The parent mod shuffles its own static encounter-ID lists in place while generating the
 * custom map. The game regenerates the dungeon map on every save/load, so a second shuffle of the
 * already shuffled list assigns different encounters to the map nodes: after an SL the monster
 * in a room is no longer the one you were fighting. Restoring the canonical lists before every
 * map generation makes the map, and therefore its encounters, reproducible again.</p>
 *
 * <p>The custom rooms also rebuild their monster group unconditionally in onPlayerEntry, unlike
 * the vanilla rooms which only do so when no monsters exist yet. The guards below mirror the
 * vanilla behaviour.</p>
 *
 * <p>Finally, the parent mod opens the dungeon map from the Proceed button without the vanilla
 * screen cleanup. Restoring that cleanup avoids a stale reward screen and a missing Continue
 * button after taking a card, picking a relic or loading a save.</p>
 */
public final class ChallengeRushFixes {

    private static final String CHALLENGE_CLASS = "challengeTheSpire.ChallengeTheSpire";
    private static final String GENERATE_MAP_HOOK =
            "challengeTheSpire.patches.com.megacrit.cardcrawl.dungeons.AbstractDungeon.GenerateMapHook";
    private static final String BOSS_RUSH_ROOM = "challengeTheSpire.MonsterRoomBossRush";
    private static final String ELITE_RUSH_ROOM = "challengeTheSpire.MonsterRoomEliteHunting";

    private ChallengeRushFixes() {
    }

    private static boolean isModActive(String id) {
        return (CardCrawlGame.trial != null && CardCrawlGame.trial.dailyModIDs().contains(id))
                || ModHelper.isModEnabled(id);
    }

    private static boolean isRushChallenge() {
        return isModActive("challengethespire:Elite Rush")
                || isModActive("challengethespire:Modded Elite Rush")
                || isModActive("challengethespire:Boss Rush")
                || isModActive("challengethespire:Modded Boss Rush");
    }

    private static List<String> ids(String... values) {
        return new ArrayList<String>(Arrays.asList(values));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<String>> getEncounterMap(String fieldName) {
        try {
            Class<?> clazz = Class.forName(CHALLENGE_CLASS);
            Field field = clazz.getField(fieldName);
            return (Map<String, List<String>>) field.get(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void restore(Map<String, List<String>> encounters, String key, String... values) {
        if (encounters != null) {
            encounters.put(key, ids(values));
        }
    }

    /**
     * Runs before the parent mod builds a rush map. Every generateMap() call, including the one
     * performed while loading a save, then starts from identical data.
     */
    @SpirePatch2(cls = GENERATE_MAP_HOOK, method = "Prefix", paramtypez = {})
    public static class RestoreEncounterLists {
        @SpirePrefixPatch
        public static void before() {
            Map<String, List<String>> elites = getEncounterMap("eliteIDs");
            restore(elites, "Exordium", "Gremlin Nob", "Lagavulin", "3 Sentries");
            restore(elites, "TheCity", "Gremlin Leader", "Slavers", "Book of Stabbing");
            restore(elites, "TheBeyond", "Giant Head", "Nemesis", "Reptomancer");
            restore(elites, "TheEnding", "Shield and Spear");

            Map<String, List<String>> bosses = getEncounterMap("bossIDs");
            restore(bosses, "Exordium", "The Guardian", "Hexaghost", "Slime Boss");
            restore(bosses, "TheCity", "Automaton", "Collector", "Champ");
            restore(bosses, "TheBeyond", "Awakened One", "Time Eater", "Donu and Deca");
            restore(bosses, "TheEnding", "The Heart");
        }
    }

    /** Vanilla monster rooms never rebuild an encounter that already exists. */
    @SpirePatch2(cls = BOSS_RUSH_ROOM, method = "onPlayerEntry", paramtypez = {})
    public static class BossRushEntryGuard {
        @SpirePrefixPatch
        public static SpireReturn before(MonsterRoomBossRush __instance) {
            if (hasMonsters(__instance)) {
                keepExistingEncounter(__instance);
                return SpireReturn.Return(null);
            }
            return SpireReturn.Continue();
        }
    }

    /** Vanilla elite rooms never rebuild an encounter that already exists. */
    @SpirePatch2(cls = ELITE_RUSH_ROOM, method = "onPlayerEntry", paramtypez = {})
    public static class EliteRushEntryGuard {
        @SpirePrefixPatch
        public static SpireReturn before(MonsterRoomEliteHunting __instance) {
            if (hasMonsters(__instance)) {
                keepExistingEncounter(__instance);
                return SpireReturn.Return(null);
            }
            return SpireReturn.Continue();
        }
    }

    private static boolean hasMonsters(AbstractRoom room) {
        return room != null && room.monsters != null && room.monsters.monsters != null
                && !room.monsters.monsters.isEmpty();
    }

    private static void keepExistingEncounter(AbstractRoom room) {
        room.playBGM(null);
        AbstractRoom.waitTimer = 0.1f;
    }

    /**
     * The parent mod's Proceed-button patch opens the map without closing the reward screen or
     * recording previousScreen, which can leave the game on a screen with no working Continue
     * button after taking a card/relic or loading a save. Perform the vanilla cleanup here.
     */
    @SpirePatch2(clz = DungeonMapScreen.class, method = "open", paramtypez = {boolean.class})
    public static class RushMapOpenCleanup {
        @SpirePrefixPatch
        public static SpireReturn before(DungeonMapScreen __instance, Object[] __args) {
            boolean doScrollingAnimation = __args != null && __args.length > 0
                    && Boolean.TRUE.equals(__args[0]);
            if (!isRushChallenge() || doScrollingAnimation || AbstractDungeon.screen == null) {
                return SpireReturn.Continue();
            }
            AbstractDungeon.CurrentScreen screen = AbstractDungeon.screen;
            if (screen != AbstractDungeon.CurrentScreen.COMBAT_REWARD
                    && screen != AbstractDungeon.CurrentScreen.BOSS_REWARD
                    && screen != AbstractDungeon.CurrentScreen.GRID
                    && screen != AbstractDungeon.CurrentScreen.CARD_REWARD) {
                return SpireReturn.Continue();
            }
            // After the last rush fight the map has nowhere to go: finish the challenge the way
            // the parent mod does instead of leaving the player on a dead-end map.
            if (screen == AbstractDungeon.CurrentScreen.COMBAT_REWARD
                    && RushModFlowPatch.isRushMonsterRoom()
                    && RushModFlowPatch.isFinalRushNode()) {
                RushModFlowPatch.goToRushVictory(AbstractDungeon.overlayMenu.proceedButton);
                return SpireReturn.Return(null);
            }
            AbstractDungeon.closeCurrentScreen();
            AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.COMBAT_REWARD;
            return SpireReturn.Continue();
        }
    }
}
