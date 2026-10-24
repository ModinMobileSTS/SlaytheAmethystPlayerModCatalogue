package shionmetricscompat;

import basemod.interfaces.PostRenderSubscriber;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.github.paopaoyue.metrics.data.CardPickStatData;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.screens.CardRewardScreen;
import com.megacrit.cardcrawl.screens.SingleCardViewPopup;
import com.megacrit.cardcrawl.shop.ShopScreen;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Draws the sts-metrics pick-rate percentage for VUPShionMod cards from BaseMod's global
 * post-render hook, so no ModTheSpire patch is involved.
 *
 * <p>sts-metrics draws from {@code AbstractCard.renderEnergy}, so a number only ever appears
 * while the card itself is being rendered. A global hook has no such implicit gate, so every
 * branch below first checks that its card list is the one currently on screen; otherwise the
 * overlay would keep painting hidden cards (for example reward cards after the reward screen
 * has been dismissed).</p>
 */
public class PickRateOverlayRenderer implements PostRenderSubscriber {

    private static final Logger logger = LogManager.getLogger(PickRateOverlayRenderer.class.getName());

    /** Cards smaller than this are hidden or mid-animation and not worth annotating. */
    private static final float MIN_DRAW_SCALE = 0.45f;

    public PickRateOverlayRenderer() {
        basemod.BaseMod.subscribe(this);
    }

    @Override
    public void receivePostRender(SpriteBatch sb) {
        try {
            renderVisibleCards(sb);
        } catch (Throwable t) {
            // A cosmetic overlay must never break rendering.
            logger.error("failed to render pick-rate overlay", t);
        }
    }

    private void renderVisibleCards(SpriteBatch sb) {
        // The full-card popup draws over everything else.
        SingleCardViewPopup popup = CardCrawlGame.cardPopup;
        if (popup != null && popup.isOpen) {
            AbstractCard card = ShionMarkers.popupCard(popup);
            CardPickStatData data = ShionMarkers.pickRateOf(card);
            if (data != null) {
                ShionMarkers.renderOnPopup(sb, data);
            }
            return;
        }

        AbstractDungeon.CurrentScreen screen = AbstractDungeon.screen;
        if (screen == null) {
            return;
        }

        switch (screen) {
            case CARD_REWARD:
                renderCardReward(sb);
                return;
            case SHOP:
                renderShop(sb);
                return;
            case GRID:
                renderGrid(sb);
                return;
            case HAND_SELECT:
            case NONE:
                // Cards in the player's hand are only drawn during combat.
                renderHand(sb);
                return;
            default:
                // Any other screen (map, deck view, settings, rewards list, ...) either hides
                // the hand or shows card lists sts-metrics never annotated either.
                return;
        }
    }

    private void renderCardReward(SpriteBatch sb) {
        CardRewardScreen screen = AbstractDungeon.cardRewardScreen;
        if (screen == null) {
            return;
        }
        renderList(sb, screen.rewardGroup);
    }

    private void renderShop(SpriteBatch sb) {
        ShopScreen screen = AbstractDungeon.shopScreen;
        if (screen == null) {
            return;
        }
        renderList(sb, screen.coloredCards);
    }

    private void renderHand(SpriteBatch sb) {
        if (AbstractDungeon.player == null || AbstractDungeon.player.hand == null) {
            return;
        }
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        if (room == null || room.phase != AbstractRoom.RoomPhase.COMBAT) {
            return;
        }
        renderList(sb, AbstractDungeon.player.hand.group);
    }

    private void renderGrid(SpriteBatch sb) {
        if (AbstractDungeon.gridSelectScreen == null) {
            return;
        }
        CardGroup group = AbstractDungeon.gridSelectScreen.targetGroup;
        if (group != null) {
            renderList(sb, group.group);
        }
    }

    private void renderList(SpriteBatch sb, List<?> cards) {
        if (cards == null || cards.isEmpty()) {
            return;
        }
        // Snapshot: other mods may mutate these lists from their own render hooks.
        for (Object entry : new ArrayList<Object>(cards)) {
            if (!(entry instanceof AbstractCard)) {
                continue;
            }
            AbstractCard card = (AbstractCard) entry;
            if (card.drawScale < MIN_DRAW_SCALE) {
                continue;
            }
            CardPickStatData data = ShionMarkers.pickRateOf(card);
            if (data == null) {
                continue;
            }
            ShionMarkers.renderOnCard(sb, card, data);
        }
    }
}
