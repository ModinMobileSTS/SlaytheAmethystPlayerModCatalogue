package bingooverflowfix;

import basemod.BaseMod;
import basemod.interfaces.PreUpdateSubscriber;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import demoMod.invincibleOne.InvincibleOneMod;

/**
 * Turns a left click on the TaskGoal relic into the "toggle the bingo board" action.
 *
 * StSLib only offers a right-click hook ({@code ClickableRelic#onRightClick}), and intercepting that
 * method from the outside is not possible with this toolchain, so the left click is detected here by
 * watching the relic's own hitbox every frame. The toggle itself is the exact logic the parent runs
 * from {@code onRightClick}, so opening/closing the board and stopping the reward pulse behave
 * identically.
 *
 * Right-click is left untouched, so the relic keeps working the old way too.
 */
public final class BingoLeftClickRelic implements PreUpdateSubscriber {

    /**
     * Same value as {@code TaskGoal.ID}, written out literally so this class never triggers the
     * relic's static initialiser (which pulls in a texture and can run too early during boot).
     */
    private static final String TASK_GOAL_ID = "Invincible:TaskGoal";

    private static boolean registered;

    public BingoLeftClickRelic() {
        BaseMod.subscribe(this);
        registered = true;
    }

    /** Subscribe exactly once; called from the mod initialiser. */
    public static void install() {
        if (!registered) {
            new BingoLeftClickRelic();
        }
    }

    @Override
    public void receivePreUpdate() {
        if (AbstractDungeon.player == null || !AbstractDungeon.player.hasRelic(TASK_GOAL_ID)) {
            return;
        }
        if (AbstractDungeon.isScreenUp) {
            // A reward screen, the deck view or an event is open: leave the click to it.
            return;
        }

        AbstractRelic relic = AbstractDungeon.player.getRelic(TASK_GOAL_ID);
        if (relic == null || relic.hb == null || !relic.hb.hovered) {
            return;
        }
        if (!InputHelper.justClickedLeft) {
            return;
        }

        // Mark the click as used so the engine does not also treat it as a plain relic click.
        relic.hb.clickStarted = true;
        toggle(relic);
    }

    /**
     * Mirrors {@code TaskGoal#onRightClick}: board and reward list are shown or hidden together, and
     * the pulse stops once nothing is left to claim.
     */
    public static void toggle(AbstractRelic relic) {
        if (InvincibleOneMod.bingoPanel == null || InvincibleOneMod.bingoRewardList == null) {
            return;
        }
        InvincibleOneMod.bingoRewardList.hide = InvincibleOneMod.bingoPanel.hide =
                !InvincibleOneMod.bingoPanel.hide;

        if (!InvincibleOneMod.bingoRewardList.getRewards().isEmpty()) {
            return;
        }
        if (relic != null) {
            relic.stopPulse();
        }
    }
}
