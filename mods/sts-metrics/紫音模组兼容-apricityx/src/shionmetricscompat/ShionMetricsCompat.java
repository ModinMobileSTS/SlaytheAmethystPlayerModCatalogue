package shionmetricscompat;

import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entry point.
 *
 * <p>This mod registers no ModTheSpire patches: everything is drawn through BaseMod's
 * {@code PostRenderSubscriber}. sts-metrics keeps ownership of the card data
 * ({@code CardFieldPatch.pickRate}); this mod only repaints the overlay for cards whose
 * {@code renderEnergy} / {@code SingleCardViewPopup.renderCost} path VUPShionMod replaced.</p>
 */
@SpireInitializer
public class ShionMetricsCompat {

    public static final Logger logger = LogManager.getLogger(ShionMetricsCompat.class.getName());

    public static void initialize() {
        logger.info("[ShionMetricsCompat] registering pick-rate overlay for VUPShionMod cards");
        if (ShionCards.isVUPShionLoaded()) {
            logger.info("[ShionMetricsCompat] VUPShionMod detected, overlay active for its cards");
        } else {
            logger.info("[ShionMetricsCompat] VUPShionMod not present, overlay stays idle");
        }
        new PickRateOverlayRenderer();
    }
}
