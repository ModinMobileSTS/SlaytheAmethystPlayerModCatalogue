package shionmetricscompat;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.github.paopaoyue.metrics.MetricsMod;
import com.github.paopaoyue.metrics.data.CardPickStatData;
import com.github.paopaoyue.metrics.patch.CardFieldPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;

/**
 * Shared helpers for the overlay: card detection, gating, and the two draw routines.
 *
 * <p>No {@code @SpirePatch2} hooks are used anywhere in this mod. An earlier revision hooked
 * {@code AbstractCard.renderEnergy}/ {@code renderType} / {@code SingleCardViewPopup.render};
 * ModTheSpire failed while injecting those ("Unknown error acquiring parameter name for
 * \"null\""), so the overlay is now drawn purely from BaseMod's post-render subscriber.</p>
 *
 * <p>Because VUPShionMod cards never reach the sts-metrics hooks, there is no double-draw risk,
 * so no "was it already drawn" bookkeeping is needed.</p>
 */
public final class ShionMarkers {

    private ShionMarkers() {
    }

    /**
     * Returns the stats sts-metrics already fetched for this card, or {@code null} when there is
     * nothing to draw (wrong mod, display disabled, or no data for that card).
     */
    public static CardPickStatData pickRateOf(AbstractCard card) {
        if (card == null) {
            return null;
        }
        if (MetricsMod.isDisplayDisabled() || MetricsMod.isPercentageDisabled()) {
            return null;
        }
        if (!ShionCards.isShionCard(card)) {
            return null;
        }
        return CardFieldPatch.pickRate.get(card);
    }

    /** In-hand / grid / reward layout, identical to sts-metrics' CardRenderPatch. */
    public static void renderOnCard(SpriteBatch sb, AbstractCard card, CardPickStatData data) {
        FontHelper.renderRotatedText(sb, FontHelper.cardEnergyFont_L, data.pickRate,
                card.current_x, card.current_y,
                132.0f * card.drawScale * Settings.scale,
                -192.0f * card.drawScale * Settings.scale,
                card.angle, false, Color.WHITE.cpy());
    }

    /** Full-card view layout, identical to sts-metrics' PopupRenderCostPatch. */
    public static void renderOnPopup(SpriteBatch sb, CardPickStatData data) {
        FontHelper.renderFont(sb, FontHelper.SCP_cardEnergyFont, data.pickRate,
                Settings.WIDTH / 2.0f + 80.0f * Settings.scale,
                Settings.HEIGHT / 2.0f - 360.0f * Settings.scale,
                Settings.CREAM_COLOR);
    }

    /** Reads {@code SingleCardViewPopup.card} without a compile-time dependency on its accessors. */
    public static AbstractCard popupCard(Object popup) {
        if (popup == null) {
            return null;
        }
        try {
            Object card = ReflectionHacks.getPrivate(popup,
                    com.megacrit.cardcrawl.screens.SingleCardViewPopup.class, "card");
            return card instanceof AbstractCard ? (AbstractCard) card : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
