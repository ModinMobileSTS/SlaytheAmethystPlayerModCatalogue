package challengethespire.presentation;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.rooms.VictoryRoom;
import com.megacrit.cardcrawl.screens.CombatRewardScreen;
import com.megacrit.cardcrawl.ui.buttons.ProceedButton;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Restores Challenge The Spire's own reward and room-progression flow for the Boss Rush map while
 * keeping the real {@code MonsterRoomBoss} presentation installed by {@link RushPresentationPatch}.
 *
 * <p>The parent mod recognises its rush rooms with {@code instanceof MonsterRoomBossRush} /
 * {@code MonsterRoomEliteHunting}. Because {@link RushBossRoom} is a {@code MonsterRoomBoss} and no
 * longer a {@code MonsterRoomBossRush}, those checks stop matching. Consequences:</p>
 * <ul>
 *   <li>the vanilla card reward is shown instead of the mod's {@code LargeCardReward};</li>
 *   <li>the vanilla Proceed button sends a boss room to a {@code TreasureRoomBoss}, where picking
 *       the boss relic ends the run instead of returning to the rush map;</li>
 *   <li>the last boss node never triggers the mod's VictoryRoom transition.</li>
 * </ul>
 *
 * <p>The patches below cover those cases without changing the room type, so the boss music,
 * background and presentation cues remain intact.</p>
 */
public final class RushModFlowPatch {

    private static final String ELITE_ROOM = "challengeTheSpire.MonsterRoomEliteHunting";
    private static final String BOSS_ROOM = "challengeTheSpire.MonsterRoomBossRush";
    private static final String LARGE_REWARD = "challengeTheSpire.LargeCardReward";

    private RushModFlowPatch() {
    }

    private static boolean isModActive(String id) {
        return (CardCrawlGame.trial != null && CardCrawlGame.trial.dailyModIDs().contains(id))
                || ModHelper.isModEnabled(id);
    }

    private static boolean isRush() {
        return isModActive("challengethespire:Elite Rush")
                || isModActive("challengethespire:Modded Elite Rush")
                || isModActive("challengethespire:Boss Rush")
                || isModActive("challengethespire:Modded Boss Rush");
    }

    public static boolean isRushMonsterRoom() {
        return AbstractDungeon.getCurrRoom() != null
                && (AbstractDungeon.getCurrRoom() instanceof RushBossRoom
                || BOSS_ROOM.equals(AbstractDungeon.getCurrRoom().getClass().getName())
                || ELITE_ROOM.equals(AbstractDungeon.getCurrRoom().getClass().getName()));
    }

    public static boolean isFinalRushNode() {
        return AbstractDungeon.getCurrMapNode() != null && AbstractDungeon.map != null
                && AbstractDungeon.getCurrMapNode().y == AbstractDungeon.map.size() - 2;
    }

    /** Opens the rush map the way the parent mod's Proceed-button hook does. */
    static void openRushMap(ProceedButton button) {
        AbstractDungeon.closeCurrentScreen();
        AbstractDungeon.dungeonMapScreen.open(false);
        AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.COMBAT_REWARD;
        button.hide();
    }

    /** The parent mod finishes a rush at a normal Heart VictoryRoom, not at the true victory. */
    public static void goToRushVictory(ProceedButton button) {
        CardCrawlGame.music.fadeOutBGM();
        CardCrawlGame.music.fadeOutTempBGM();
        MapRoomNode node = new MapRoomNode(-1, 15);
        node.room = new VictoryRoom(VictoryRoom.EventType.HEART);
        AbstractDungeon.nextRoom = node;
        AbstractDungeon.closeCurrentScreen();
        AbstractDungeon.nextRoomTransitionStart();
        button.hide();
    }

    private static RewardItem createLargeCardReward() {
        try {
            Class<?> main = Class.forName("challengeTheSpire.ChallengeTheSpire");
            Method challenge = main.getDeclaredMethod("getActiveChallenge");
            Method difficulty = main.getDeclaredMethod("getActiveDifficulty");
            challenge.setAccessible(true);
            difficulty.setAccessible(true);
            Constructor<?> ctor = Class.forName(LARGE_REWARD)
                    .getConstructor(String.class, String.class);
            return (RewardItem) ctor.newInstance(challenge.invoke(null), difficulty.invoke(null));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean hasLargeCardReward(CombatRewardScreen screen) {
        for (RewardItem r : screen.rewards) {
            if (r != null && LARGE_REWARD.equals(r.getClass().getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gives boss-rush (and, as a safety net, elite-rush) rooms the mod's own large card reward.
     * The mod's rewardViewUpdate patch already knows how to claim CTS_LARGE_CARD_REWARD items.
     */
    @SpirePatch2(clz = CombatRewardScreen.class, method = "setupItemReward", paramtypez = {})
    public static class RestoreModCardReward {
        @SpirePostfixPatch
        public static void after(CombatRewardScreen __instance) {
            if (!isRush() || !isRushMonsterRoom() || hasLargeCardReward(__instance)) {
                return;
            }
            __instance.rewards.removeIf(r -> r != null && r.type == RewardItem.RewardType.CARD);
            RewardItem large = createLargeCardReward();
            if (large != null) {
                __instance.rewards.add(large);
            }
            __instance.positionRewards();
        }
    }

    /**
     * Vanilla only knows how to leave a MonsterRoomBoss by going to the boss treasure room. In a
     * rush that treasure room ends the run, so return to the map (or finish at the final node).
     */
    @SpirePatch2(clz = ProceedButton.class, method = "goToTreasureRoom", paramtypez = {})
    public static class BossRushProceed {
        @SpirePrefixPatch
        public static SpireReturn before(ProceedButton __instance) {
            if (!isRush() || !(AbstractDungeon.getCurrRoom() instanceof RushBossRoom)) {
                return SpireReturn.Continue();
            }
            if (isFinalRushNode()) {
                goToRushVictory(__instance);
            } else {
                openRushMap(__instance);
            }
            return SpireReturn.Return(null);
        }
    }

    /** Never let the rush run end early in one of the game's act-specific boss shortcuts. */
    @SpirePatch2(clz = ProceedButton.class, method = "goToVictoryRoomOrTheDoor", paramtypez = {})
    public static class VictoryDoorGuard {
        @SpirePrefixPatch
        public static SpireReturn before(ProceedButton __instance) {
            if (!isRush() || !isRushMonsterRoom() || isFinalRushNode()) {
                return SpireReturn.Continue();
            }
            openRushMap(__instance);
            return SpireReturn.Return(null);
        }
    }

    @SpirePatch2(clz = ProceedButton.class, method = "goToTrueVictoryRoom", paramtypez = {})
    public static class TrueVictoryGuard {
        @SpirePrefixPatch
        public static SpireReturn before(ProceedButton __instance) {
            if (!isRush() || !isRushMonsterRoom()) {
                return SpireReturn.Continue();
            }
            if (isFinalRushNode()) {
                goToRushVictory(__instance);
            } else {
                openRushMap(__instance);
            }
            return SpireReturn.Return(null);
        }
    }

    @SpirePatch2(clz = ProceedButton.class, method = "goToDoubleBoss", paramtypez = {})
    public static class DoubleBossGuard {
        @SpirePrefixPatch
        public static SpireReturn before(ProceedButton __instance) {
            if (!isRush() || !isRushMonsterRoom()) {
                return SpireReturn.Continue();
            }
            if (isFinalRushNode()) {
                goToRushVictory(__instance);
            } else {
                openRushMap(__instance);
            }
            return SpireReturn.Return(null);
        }
    }
}
