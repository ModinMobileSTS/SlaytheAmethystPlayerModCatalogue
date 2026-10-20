package bingooverflowfix;

import basemod.BaseMod;
import basemod.interfaces.PostUpdateSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rewards.RewardItem;
import demoMod.invincibleOne.InvincibleOneMod;
import demoMod.invincibleOne.ui.BingoItem;
import demoMod.invincibleOne.ui.BingoRewardList;
import demoMod.invincibleOne.ui.BingoSlot;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Fixes and adjusts the bingo quest panel.
 *
 * <ol>
 *   <li><b>Grid overflow.</b> The parent derives every cell's side length and row step from
 *       {@code Settings.WIDTH} alone, so the 5x5 grid is always {@code 0.5 * WIDTH} tall. On wide
 *       phone screens (19.5:9 and above) it is taller than the screen and both the first and the
 *       last row are cut off, including their text. The slots in the parent's own list are swapped
 *       for {@link BingoSlotFix}, which sizes and draws the grid from both screen axes and shrinks
 *       long descriptions so they stay inside their cell.</li>
 *   <li><b>Reward list overflow.</b> The parent stacks rewards downwards from just under the top bar
 *       with no clamp, so a long list marches off the bottom of the screen. A post-update pass pulls
 *       the block back into a safe band, keeping the relative spacing intact.</li>
 *   <li><b>Left click instead of right click.</b> See {@link BingoLeftClickRelic}; the relic
 *       description is rewritten to say "left click" so the player is not misled.</li>
 * </ol>
 *
 * Everything is done through BaseMod subscribers, so no parent method is patched and every gameplay
 * rule of the mod keeps running unchanged.
 *
 * Note: this class deliberately does <em>not</em> implement {@code EditStringsSubscriber}. That hook
 * is invoked from inside the {@code LocalizedStrings} constructor, at which point
 * {@code CardCrawlGame.languagePack} is still null, so reading relic strings there throws a
 * NullPointerException and kills the game during boot. The description is rewritten lazily instead.
 */
@SpireInitializer
public class BingoOverflowFix implements PostUpdateSubscriber {

    /**
     * Hard-coded copy of {@code TaskGoal.ID} on purpose: touching the relic class too early can
     * trigger its static initialiser while {@code AbstractRelic} is still bootstrapping.
     */
    private static final String TASK_GOAL_ID = "Invincible:TaskGoal";

    private static final float REWARD_TOP_MARGIN = 170.0f;
    private static final float REWARD_BOTTOM_MARGIN = 250.0f;
    private static final float REWARD_HALF_ROW = 49.0f;

    private static Field rewardListField;
    private static Field isRewardField;
    private static Field slotXField;
    private static Field slotYField;
    private static boolean ready;
    private static boolean descriptionSynced;

    public BingoOverflowFix() {
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        new BingoOverflowFix();
        BingoLeftClickRelic.install();
    }

    // -------------------------------------------------------------- update

    @Override
    public void receivePostUpdate() {
        if (!ensureReady() || InvincibleOneMod.bingoPanel == null) {
            return;
        }
        replaceSlotsIfNeeded();
        clampRewards();
        syncRelicDescription();
    }

    // ------------------------------------------------------------- strings

    /**
     * Rewrites "right-click" to "left-click" in the task-goal relic description, both in the live
     * relic instance (which is what the top bar tooltip renders) and, when it is available, in the
     * registered string entry.
     *
     * Runs on the first frame where the player exists. Every step is defensive: a text tweak must
     * never be able to take the game down.
     */
    private static void syncRelicDescription() {
        if (descriptionSynced) {
            return;
        }
        if (AbstractDungeon.player == null) {
            return;
        }
        if (!AbstractDungeon.player.hasRelic(TASK_GOAL_ID)) {
            return;
        }
        AbstractRelic relic = AbstractDungeon.player.getRelic(TASK_GOAL_ID);
        if (relic == null) {
            return;
        }

        translate(relic.DESCRIPTIONS);
        try {
            relic.updateDescription(AbstractDungeon.player.chosenClass);
        } catch (RuntimeException ignored) {
            // Text refresh only; the DESCRIPTIONS array above is already correct.
        }
        if (relic.DESCRIPTIONS != null && relic.DESCRIPTIONS.length > 0) {
            relic.description = relic.DESCRIPTIONS[0];
        }
        syncRegisteredStrings();
        descriptionSynced = true;
    }

    /** Best effort: also fix the shared entry so newly created copies inherit the new wording. */
    private static void syncRegisteredStrings() {
        try {
            if (CardCrawlGame.languagePack == null) {
                return;
            }
            RelicStrings strings = CardCrawlGame.languagePack.getRelicStrings(TASK_GOAL_ID);
            if (strings != null) {
                translate(strings.DESCRIPTIONS);
            }
        } catch (RuntimeException ignored) {
            // Cosmetic only; the live relic is already patched.
        }
    }

    private static void translate(String[] descriptions) {
        if (descriptions == null) {
            return;
        }
        for (int i = 0; i < descriptions.length; i++) {
            String text = descriptions[i];
            if (text == null) {
                continue;
            }
            if (Settings.language == Settings.GameLanguage.ZHS) {
                descriptions[i] = text.replace("右键", "左键");
            } else {
                descriptions[i] = text.replace("Right-click", "Left-click")
                        .replace("right-click", "left-click");
            }
        }
    }

    // --------------------------------------------------------------- panel

    /**
     * Swaps every plain {@link BingoSlot} in the parent's list for a {@link BingoSlotFix}.
     *
     * The parent rebuilds that list when a run starts and after loading a save, so this runs every
     * frame but only touches real changes. The new cell inherits the current position of the cell it
     * replaces, so nothing pops when the swap happens mid-float.
     */
    private static void replaceSlotsIfNeeded() {
        List<BingoSlot> slots;
        try {
            slots = InvincibleOneMod.bingoPanel.getSlots();
        } catch (RuntimeException e) {
            return;
        }
        if (slots == null) {
            return;
        }
        for (int i = 0; i < slots.size(); i++) {
            BingoSlot slot = slots.get(i);
            if (slot == null || slot instanceof BingoSlotFix) {
                continue;
            }
            BingoItem item = slot.getBingoItem();
            if (item == null) {
                continue;
            }
            BingoSlotFix replacement = new BingoSlotFix(item);
            // The BingoSlot constructor re-points item.currentSlot at the replacement, which is what
            // BingoItem.completeBingo() checks, so completion logic keeps working.
            try {
                if (slotXField != null && slotYField != null) {
                    replacement.snapTo(slotXField.getFloat(slot), slotYField.getFloat(slot));
                }
            } catch (IllegalAccessException ignored) {
                // Purely cosmetic; the cell simply lerps in from its default position.
            }
            slots.set(i, replacement);
        }
    }

    private static void clampRewards() {
        BingoRewardList list = InvincibleOneMod.bingoRewardList;
        if (list == null) {
            return;
        }
        List<?> rewards;
        try {
            rewards = (List<?>) rewardListField.get(list);
        } catch (IllegalAccessException e) {
            return;
        }
        if (rewards == null || rewards.isEmpty()) {
            return;
        }

        float top = Settings.HEIGHT - (REWARD_TOP_MARGIN + REWARD_HALF_ROW) * Settings.yScale;
        float bottom = (REWARD_BOTTOM_MARGIN + REWARD_HALF_ROW) * Settings.yScale;
        if (top <= bottom) {
            return;
        }

        float highest = Float.NEGATIVE_INFINITY;
        float lowest = Float.POSITIVE_INFINITY;
        for (Object o : rewards) {
            if (!(o instanceof RewardItem)) {
                continue;
            }
            float y = ((RewardItem) o).y;
            highest = Math.max(highest, y);
            lowest = Math.min(lowest, y);
        }
        if (highest == Float.NEGATIVE_INFINITY) {
            return;
        }

        float shift = 0.0f;
        if (lowest < bottom) {
            // Runs off the bottom of the screen: lift the block back up.
            shift = bottom - lowest;
        }
        if (highest + shift > top) {
            // That would poke out of the top: keep the head of the list visible instead.
            shift = top - highest;
        }
        if (Math.abs(shift) < 0.5f) {
            return;
        }
        for (Object o : rewards) {
            if (!(o instanceof RewardItem)) {
                continue;
            }
            RewardItem reward = (RewardItem) o;
            reward.move(reward.y + shift);
        }
    }

    /** {@code BingoItem#isReward} is protected; reward cells must stay unclickable. */
    public static boolean isRewardSlot(BingoItem item) {
        if (item == null || isRewardField == null) {
            return false;
        }
        try {
            return isRewardField.getBoolean(item);
        } catch (IllegalAccessException e) {
            // Fail closed: an unclickable reward cell is recoverable, a wrongly claimed one is not.
            return true;
        }
    }

    private static boolean ensureReady() {
        if (ready) {
            return true;
        }
        try {
            rewardListField = BingoRewardList.class.getDeclaredField("rewards");
            rewardListField.setAccessible(true);
            try {
                isRewardField = BingoItem.class.getDeclaredField("isReward");
                isRewardField.setAccessible(true);
            } catch (NoSuchFieldException ignored) {
                isRewardField = null;
            }
            try {
                slotXField = BingoSlot.class.getDeclaredField("x");
                slotYField = BingoSlot.class.getDeclaredField("y");
                slotXField.setAccessible(true);
                slotYField.setAccessible(true);
            } catch (NoSuchFieldException ignored) {
                slotXField = null;
                slotYField = null;
            }
            ready = true;
            return true;
        } catch (NoSuchFieldException e) {
            // Parent internals changed; leave the mod completely untouched.
            return false;
        }
    }
}
