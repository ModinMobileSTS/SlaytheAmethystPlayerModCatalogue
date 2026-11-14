package cardrefreshtweak;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.screens.CardRewardScreen;
import com.megacrit.cardcrawl.ui.buttons.PeekButton;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Draws and handles the refresh buttons on the card reward screen.
 *
 * Two modes:
 *  - whole group (original): one big button below the cards, powered by the accumulated charges,
 *  - single card: one button per card option, drawn exactly like the vanilla "skip" button and
 *    placed in its row below the cards, each with a fixed amount of uses.
 *
 * The original mod's button is disabled by the Parent*DisablePatch classes; everything the player
 * sees here is this class, because the pay out of a reroll has to be recorded in a way that
 * survives saving and loading the run.
 */
public final class RefreshUi {

    private static final Logger LOG = LogManager.getLogger("CardRefreshTweak");

    private static final RefreshUi INSTANCE = new RefreshUi();

    private static final float COOLDOWN = 0.45f;
    private static final float MIN_VISIBLE_TIME = 0.3f;

    /** Same drawn size as {@code SkipCardButton}: whole 512x256 texture scaled by Settings.scale. */
    private static final float BTN_TEX_W = 512.0f;
    private static final float BTN_TEX_H = 256.0f;
    /** Same clickable area as {@code SkipCardButton}. */
    private static final float SLOT_HIT_W = 260.0f;
    private static final float SLOT_HIT_H = 80.0f;
    /** Distance from the bottom edge of the card (at its laid out scale) to the button centre. */
    private static final float SLOT_Y_FROM_CARD = 44.0f;

    private static final float GROUP_HIT_W = 260.0f;
    private static final float GROUP_HIT_H = 80.0f;

    private final Hitbox groupHb = new Hitbox(GROUP_HIT_W * Settings.scale, GROUP_HIT_H * Settings.scale);

    private boolean visible = false;
    private float visibleTime = 0.0f;
    private float alpha = 0.0f;

    private boolean groupArmed = false;
    private boolean groupPrevClick = false;
    private float groupCooldown = 0.0f;

    private Hitbox[] slotHb = new Hitbox[0];
    private boolean[] slotArmed = new boolean[0];
    private boolean[] slotPrevClick = new boolean[0];
    private float[] slotCooldown = new float[0];

    private static Field touchCardField = null;
    private static boolean touchCardLookupDone = false;

    private RefreshUi() {
    }

    public static RefreshUi inst() {
        return INSTANCE;
    }

    // ---------------------------------------------------------------- hooks

    /** Called from the postfix of {@code CardRewardScreen.open}. */
    public void onOpen(CardRewardScreen screen) {
        if (screen == null) {
            return;
        }
        RewardItem rItem = screen.rItem;
        if (rItem == null || screen.rewardGroup == null || screen.rewardGroup.isEmpty()) {
            return;
        }
        RefreshState.ensureRun();
        int size = screen.rewardGroup.size();
        int floor = AbstractDungeon.floorNum;
        int remaining = remainingCardRewards();
        if (RefreshState.beginReward(floor, remaining, size)) {
            LOG.info("[CardRefresh] card reward already paid out (floor " + floor + ", remaining "
                    + remaining + "), charges=" + RefreshState.charges
                    + " slot " + usedSlots());
        } else {
            LOG.info("[CardRefresh] new card reward (floor " + floor + ", remaining " + remaining
                    + "), charges=" + RefreshState.charges);
        }
        resetWidgets(size);
    }

    /** Called from the postfix of {@code CardRewardScreen.update}. */
    public void onUpdate(CardRewardScreen screen) {
        float dt = Gdx.graphics.getDeltaTime();
        if (dt <= 0.0f || dt > 1.0f) {
            dt = 0.016f;
        }
        if (!isActive(screen)) {
            hide();
            return;
        }
        if (!visible) {
            visible = true;
            visibleTime = 0.0f;
            alpha = 0.0f;
            clearInput();
        }
        visibleTime += dt;

        if (RefreshConfig.singleModeActive()) {
            syncSlots(screen.rewardGroup.size());
            updateSlots(screen, dt);
        } else if (!RefreshConfig.singleCardMode) {
            updateGroup(screen, dt);
        }

        alpha = MathHelper.fadeLerpSnap(alpha, 1.0f);
    }

    /** Called from the postfix of {@code CardRewardScreen.render}. */
    public void onRender(CardRewardScreen screen, SpriteBatch sb) {
        if (!visible || sb == null || !isActive(screen)) {
            return;
        }
        if (RefreshConfig.singleModeActive()) {
            int count = Math.min(screen.rewardGroup.size(), slotHb.length);
            for (int i = 0; i < count; ++i) {
                Hitbox hb = slotHb[i];
                if (hb == null) {
                    continue;
                }
                int remaining = remainingFor(i);
                drawButton(sb, hb, label(remaining), remaining > 0);
            }
        } else if (!RefreshConfig.singleCardMode) {
            drawButton(sb, groupHb, label(RefreshState.charges), RefreshState.charges > 0);
        }
        sb.setColor(Color.WHITE);
    }

    /**
     * True while the player presses on one of our buttons, so the vanilla card handling of the
     * reward screen must not treat that press as "take this card".
     */
    public boolean blocksCardClick() {
        if (!visible) {
            return false;
        }
        if (!InputHelper.justClickedLeft && !InputHelper.isMouseDown) {
            return false;
        }
        if (RefreshConfig.singleModeActive()) {
            for (int i = 0; i < slotHb.length; ++i) {
                if (slotHb[i] != null && slotHb[i].hovered) {
                    return true;
                }
            }
            return false;
        }
        if (RefreshConfig.singleCardMode) {
            return false;
        }
        return groupHb.hovered;
    }

    // ---------------------------------------------------------------- whole group mode

    private void updateGroup(CardRewardScreen screen, float dt) {
        boolean enabled = RefreshState.charges > 0;
        groupHb.move(groupX(), groupY());
        groupHb.update();

        if (groupCooldown > 0.0f) {
            groupCooldown = Math.max(0.0f, groupCooldown - dt);
        }
        if (enabled && groupHb.justHovered) {
            CardCrawlGame.sound.play("UI_HOVER");
        }

        boolean freshClick = InputHelper.justClickedLeft && !groupPrevClick;
        groupPrevClick = InputHelper.justClickedLeft;
        boolean pointerDown = InputHelper.isMouseDown || InputHelper.justReleasedClickLeft;

        if (enabled && groupCooldown <= 0.0f && freshClick && pointerDown && groupHb.hovered
                && visibleTime > MIN_VISIBLE_TIME) {
            groupArmed = true;
            CardCrawlGame.sound.play("UI_CLICK_1");
        }
        if (groupArmed && !InputHelper.isMouseDown) {
            groupArmed = false;
            if (enabled && groupHb.hovered && isActive(screen)) {
                rerollWholeGroup(screen);
                groupCooldown = COOLDOWN;
            }
        }
        groupHb.clicked = false;
    }

    private void rerollWholeGroup(CardRewardScreen screen) {
        if (RefreshState.charges <= 0) {
            return;
        }
        ArrayList<AbstractCard> before = new ArrayList<AbstractCard>(screen.rewardGroup);
        ArrayList<AbstractCard> after = generateGroup(before);
        if (after == null || after.isEmpty()) {
            return;
        }
        screen.rewardGroup.clear();
        screen.rewardGroup.addAll(after);
        if (screen.rItem != null && screen.rItem.cards != screen.rewardGroup) {
            screen.rItem.cards = new ArrayList<AbstractCard>(screen.rewardGroup);
        }
        layout(screen);
        for (AbstractCard card : after) {
            UnlockTracker.markCardAsSeen(card.cardID);
        }
        RefreshState.charges = Math.max(0, RefreshState.charges - 1);
        RefreshState.save();
        clearVanillaTouchCard(screen);
        LOG.info("[CardRefresh] reroll group " + ids(before) + " -> " + ids(after)
                + ", left=" + RefreshState.charges);
        CardCrawlGame.sound.playA("CARD_OBTAIN", 0.25f);
    }

    private static ArrayList<AbstractCard> generateGroup(ArrayList<AbstractCard> old) {
        if (AbstractDungeon.player == null) {
            return null;
        }
        AbstractCard.CardColor color = AbstractDungeon.player.getCardColor();
        ArrayList<AbstractCard> fallback = null;
        for (int attempt = 0; attempt < 4; ++attempt) {
            ArrayList<AbstractCard> rolled;
            try {
                rolled = new RewardItem(color).cards;
            } catch (Throwable t) {
                LOG.warn("[CardRefresh] cannot generate reward cards: " + t);
                return fallback;
            }
            if (rolled == null || rolled.isEmpty()) {
                continue;
            }
            if (fallback == null) {
                fallback = rolled;
            }
            if (!sameIds(rolled, old)) {
                return rolled;
            }
        }
        return fallback;
    }

    // ---------------------------------------------------------------- single card mode

    private void updateSlots(CardRewardScreen screen, float dt) {
        int count = Math.min(screen.rewardGroup.size(), slotHb.length);
        int perCard = RefreshConfig.refreshesPerCard();
        for (int i = 0; i < count; ++i) {
            Hitbox hb = slotHb[i];
            AbstractCard card = screen.rewardGroup.get(i);
            if (hb == null || card == null) {
                continue;
            }
            int remaining = Math.max(0, perCard - RefreshState.usedFor(i));
            boolean enabled = remaining > 0;

            hb.move(card.current_x, slotY(card));
            hb.update();

            if (slotCooldown[i] > 0.0f) {
                slotCooldown[i] = Math.max(0.0f, slotCooldown[i] - dt);
            }
            if (enabled && hb.justHovered) {
                CardCrawlGame.sound.play("UI_HOVER");
            }

            boolean freshClick = InputHelper.justClickedLeft && !slotPrevClick[i];
            slotPrevClick[i] = InputHelper.justClickedLeft;
            boolean pointerDown = InputHelper.isMouseDown || InputHelper.justReleasedClickLeft;

            if (enabled && slotCooldown[i] <= 0.0f && freshClick && pointerDown && hb.hovered
                    && visibleTime > MIN_VISIBLE_TIME) {
                slotArmed[i] = true;
                CardCrawlGame.sound.play("UI_CLICK_1");
            }
            if (slotArmed[i] && !InputHelper.isMouseDown) {
                slotArmed[i] = false;
                if (enabled && hb.hovered && isActive(screen)) {
                    rerollSingleCard(screen, i);
                    slotCooldown[i] = COOLDOWN;
                }
            }
            hb.clicked = false;
        }
    }

    /**
     * The row of per-card buttons sits right below the cards, in the free strip between the cards
     * and the vanilla skip button. The card's laid out scale is used (not its current scale), so
     * the buttons do not jump around while a card is hovered.
     */
    private static float slotY(AbstractCard card) {
        float cardBottom = card.current_y - AbstractCard.IMG_HEIGHT * 0.5f * 0.75f;
        return cardBottom - SLOT_Y_FROM_CARD * Settings.scale;
    }

    private void rerollSingleCard(CardRewardScreen screen, int index) {
        ArrayList<AbstractCard> group = screen.rewardGroup;
        if (group == null || index < 0 || index >= group.size()) {
            return;
        }
        int remaining = RefreshConfig.refreshesPerCard() - RefreshState.usedFor(index);
        if (remaining <= 0) {
            return;
        }
        AbstractCard old = group.get(index);
        if (old == null) {
            return;
        }
        AbstractCard replacement = generateSingle(group, old);
        if (replacement == null) {
            return;
        }
        replacement.drawScale = old.drawScale > 0.0f ? old.drawScale : 0.75f;
        replacement.targetDrawScale = replacement.drawScale;
        replacement.current_x = old.current_x;
        replacement.current_y = old.current_y;
        replacement.target_x = old.current_x;
        replacement.target_y = old.current_y;
        replacement.angle = 0.0f;
        replacement.targetAngle = 0.0f;

        group.set(index, replacement);
        // The reward screen normally uses the very same list as rItem.cards, but keep the reward
        // item in sync in case it ever holds a separate copy.
        if (screen.rItem != null && screen.rItem.cards != group) {
            screen.rItem.cards = new ArrayList<AbstractCard>(group);
        }
        UnlockTracker.markCardAsSeen(replacement.cardID);
        RefreshState.markUsed(index);
        RefreshState.save();
        clearVanillaTouchCard(screen);
        LOG.info("[CardRefresh] reroll card #" + index + " " + old.cardID + " -> "
                + replacement.cardID + ", left="
                + Math.max(0, RefreshConfig.refreshesPerCard() - RefreshState.usedFor(index)));
        CardCrawlGame.sound.playA("CARD_OBTAIN", 0.25f);
    }

    private static AbstractCard generateSingle(ArrayList<AbstractCard> group, AbstractCard replaced) {
        if (AbstractDungeon.player == null) {
            return null;
        }
        AbstractCard.CardColor color = AbstractDungeon.player.getCardColor();
        ArrayList<AbstractCard> candidates = null;
        for (int attempt = 0; attempt < 4 && (candidates == null || candidates.isEmpty()); ++attempt) {
            try {
                candidates = new RewardItem(color).cards;
            } catch (Throwable t) {
                LOG.warn("[CardRefresh] cannot generate a card: " + t);
                return null;
            }
        }
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        Set<String> shown = new HashSet<String>();
        for (AbstractCard card : group) {
            if (card != null && card != replaced) {
                shown.add(card.cardID);
            }
        }
        AbstractCard sameRarity = null;
        AbstractCard different = null;
        for (AbstractCard card : candidates) {
            if (card == null || shown.contains(card.cardID)) {
                continue;
            }
            if (different == null) {
                different = card;
            }
            if (replaced.rarity != null && card.rarity == replaced.rarity) {
                sameRarity = card;
                break;
            }
        }
        if (sameRarity != null) {
            return sameRarity;
        }
        if (different != null) {
            return different;
        }
        return candidates.get(0);
    }

    // ---------------------------------------------------------------- drawing

    /** Draws a button exactly like the vanilla skip button, so both look the same size. */
    private void drawButton(SpriteBatch sb, Hitbox hb, String text, boolean enabled) {
        float x = hb.cX;
        float y = hb.cY;
        Color tint = enabled ? Color.WHITE : new Color(0.45f, 0.45f, 0.45f, 1.0f);
        sb.setColor(tint.r, tint.g, tint.b, alpha);
        drawButtonTexture(sb, x, y);
        if (enabled && hb.hovered && !hb.clickStarted) {
            sb.setBlendFunction(770, 1);
            sb.setColor(1.0f, 1.0f, 1.0f, 0.3f * alpha);
            drawButtonTexture(sb, x, y);
            sb.setBlendFunction(770, 771);
        }
        Color textColor = enabled ? Color.WHITE.cpy() : new Color(0.62f, 0.62f, 0.62f, 1.0f);
        textColor.a = alpha;
        FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, text, x, y, textColor);
    }

    private static void drawButtonTexture(SpriteBatch sb, float x, float y) {
        sb.draw(ImageMaster.REWARD_SCREEN_TAKE_BUTTON,
                x - BTN_TEX_W / 2.0f, y - BTN_TEX_H / 2.0f,
                BTN_TEX_W / 2.0f, BTN_TEX_H / 2.0f,
                BTN_TEX_W, BTN_TEX_H,
                Settings.scale, Settings.scale, 0.0f,
                0, 0, 512, 256, false, false);
    }

    private int remainingFor(int index) {
        return Math.max(0, RefreshConfig.refreshesPerCard() - RefreshState.usedFor(index));
    }

    // ---------------------------------------------------------------- helpers

    /**
     * How many card rewards are still unclaimed in the current room, this one included.
     *
     * Together with the floor this identifies the reward across a save / load: the reward list is
     * rebuilt from the save in the same order and the item is only removed when the player takes
     * or skips it, so a reward that was open when the game was saved reports the same number
     * again. A second card reward of the same room (Prayer Wheel, Colosseum) reports one less.
     *
     * @return the count, or -1 when it cannot be determined
     */
    private static int remainingCardRewards() {
        try {
            if (AbstractDungeon.combatRewardScreen == null) {
                return -1;
            }
            ArrayList<?> rewards = AbstractDungeon.combatRewardScreen.rewards;
            if (rewards == null) {
                return -1;
            }
            int count = 0;
            for (int i = 0; i < rewards.size(); ++i) {
                Object entry = rewards.get(i);
                if (entry instanceof RewardItem && ((RewardItem) entry).type == RewardItem.RewardType.CARD) {
                    ++count;
                }
            }
            return count;
        } catch (Throwable t) {
            LOG.warn("[CardRefresh] cannot count card rewards: " + t);
            return -1;
        }
    }

    private static String usedSlots() {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < RefreshState.slotUsed.length; ++i) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(RefreshState.usedFor(i));
        }
        return builder.append(']').toString();
    }

    private static boolean isActive(CardRewardScreen screen) {
        return screen != null
                && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.CARD_REWARD
                && screen.rItem != null
                && screen.rewardGroup != null
                && !screen.rewardGroup.isEmpty()
                && !PeekButton.isPeeking;
    }

    private void hide() {
        visible = false;
        visibleTime = 0.0f;
        alpha = 0.0f;
        clearInput();
    }

    private void clearInput() {
        groupArmed = false;
        groupPrevClick = false;
        for (int i = 0; i < slotArmed.length; ++i) {
            slotArmed[i] = false;
            slotPrevClick[i] = false;
        }
    }

    private void resetWidgets(int size) {
        groupArmed = false;
        groupPrevClick = false;
        groupCooldown = 0.0f;
        visible = false;
        visibleTime = 0.0f;
        alpha = 0.0f;
        syncSlots(size);
        for (int i = 0; i < slotCooldown.length; ++i) {
            slotCooldown[i] = 0.0f;
        }
    }

    private void syncSlots(int size) {
        if (slotHb.length == size && slotArmed.length == size && slotPrevClick.length == size
                && slotCooldown.length == size) {
            return;
        }
        int fixed = Math.max(0, size);
        Hitbox[] newHb = new Hitbox[fixed];
        boolean[] newArmed = new boolean[fixed];
        boolean[] newPrev = new boolean[fixed];
        float[] newCooldown = new float[fixed];
        for (int i = 0; i < fixed; ++i) {
            newHb[i] = i < slotHb.length && slotHb[i] != null
                    ? slotHb[i]
                    : new Hitbox(SLOT_HIT_W * Settings.scale, SLOT_HIT_H * Settings.scale);
            newArmed[i] = i < slotArmed.length && slotArmed[i];
            newPrev[i] = i < slotPrevClick.length && slotPrevClick[i];
            newCooldown[i] = i < slotCooldown.length ? slotCooldown[i] : 0.0f;
        }
        slotHb = newHb;
        slotArmed = newArmed;
        slotPrevClick = newPrev;
        slotCooldown = newCooldown;
    }

    private static String label(int value) {
        String base = "REFRESH";
        if (Settings.language == Settings.GameLanguage.ZHS || Settings.language == Settings.GameLanguage.ZHT) {
            base = "刷新";
        }
        return base + " " + Math.max(0, value);
    }

    private static boolean hasSingingBowl() {
        return AbstractDungeon.player != null && AbstractDungeon.player.hasRelic("Singing Bowl");
    }

    private static float groupX() {
        float offset = hasSingingBowl() ? 435.0f : 330.0f;
        return (float) Settings.WIDTH / 2.0f - offset * Settings.scale;
    }

    private static float groupY() {
        return (float) Settings.HEIGHT / 2.0f - 340.0f * Settings.scale;
    }

    /**
     * The vanilla screen remembers the card the player tapped on touch devices. That reference
     * would point at a card that no longer is one of the options after a reroll, so it is cleared.
     */
    private static void clearVanillaTouchCard(CardRewardScreen screen) {
        if (screen == null || !Settings.isTouchScreen) {
            return;
        }
        if (!touchCardLookupDone) {
            touchCardLookupDone = true;
            try {
                Field field = CardRewardScreen.class.getDeclaredField("touchCard");
                field.setAccessible(true);
                touchCardField = field;
            } catch (Throwable t) {
                touchCardField = null;
            }
        }
        if (touchCardField == null) {
            return;
        }
        try {
            if (touchCardField.get(screen) != null) {
                touchCardField.set(screen, null);
                LOG.info("[CardRefresh] cleared the pending card selection");
            }
        } catch (Throwable t) {
            touchCardField = null;
        }
    }

    private static boolean sameIds(ArrayList<AbstractCard> a, ArrayList<AbstractCard> b) {
        if (a == null || b == null || a.size() != b.size()) {
            return false;
        }
        ArrayList<String> left = new ArrayList<String>();
        ArrayList<String> right = new ArrayList<String>();
        for (AbstractCard card : a) {
            left.add(card == null ? "" : String.valueOf(card.cardID));
        }
        for (AbstractCard card : b) {
            right.add(card == null ? "" : String.valueOf(card.cardID));
        }
        Collections.sort(left);
        Collections.sort(right);
        return left.equals(right);
    }

    private static String ids(ArrayList<AbstractCard> cards) {
        StringBuilder builder = new StringBuilder("[");
        if (cards != null) {
            for (int i = 0; i < cards.size(); ++i) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(cards.get(i) == null ? "null" : cards.get(i).cardID);
            }
        }
        return builder.append(']').toString();
    }

    private static void layout(CardRewardScreen screen) {
        ArrayList<AbstractCard> group = screen.rewardGroup;
        if (group == null) {
            return;
        }
        int size = group.size();
        float pad = 40.0f * Settings.xScale;
        float y = (float) Settings.HEIGHT * 0.45f;
        for (int i = 0; i < size; ++i) {
            AbstractCard card = group.get(i);
            if (card == null) {
                continue;
            }
            card.drawScale = 0.75f;
            card.targetDrawScale = 0.75f;
            card.current_y = y;
            card.current_x = size <= 5
                    ? (float) Settings.WIDTH / 2.0f + ((float) i - (float) (size - 1) / 2.0f) * (AbstractCard.IMG_WIDTH + pad)
                    : (float) Settings.WIDTH / 2.0f;
            card.target_x = card.current_x;
            card.target_y = y;
            card.angle = 0.0f;
            card.targetAngle = 0.0f;
        }
    }
}
