package shionmetricscompat;

import com.megacrit.cardcrawl.cards.AbstractCard;

/**
 * Detects cards added by VUPShionMod (ShionMod / Anastasia Project) without a compile
 * time dependency, so this patch mod keeps working whether or not that mod is installed.
 */
public final class ShionCards {

    private static final String SHION_CARD_BASE = "VUPShionMod.cards.AbstractVUPShionCard";
    private static final String SHION_ADDON_BRIDGE = "VUPShionMod.cards.ShionCard.anastasia.FinFunnelUpgrade";

    private static boolean baseLookedUp = false;
    private static Class<?> baseClass = null;

    private ShionCards() {
    }

    public static boolean isVUPShionLoaded() {
        return getBaseClass() != null;
    }

    /**
     * True when the card belongs to the VUPShionMod family, i.e. it (transitively) extends
     * AbstractVUPShionCard and therefore carries the mod's custom renderEnergy override.
     */
    public static boolean isShionCard(AbstractCard card) {
        if (card == null) {
            return false;
        }
        Class<?> base = getBaseClass();
        return base != null && base.isInstance(card);
    }

    private static Class<?> getBaseClass() {
        if (!baseLookedUp) {
            baseLookedUp = true;
            baseClass = loadOrNull(SHION_CARD_BASE);
            if (baseClass == null) {
                // Fallback: a public subclass keeps the same base, but this guards against a
                // future refactor that changes the abstract class name.
                baseClass = loadOrNull(SHION_ADDON_BRIDGE);
            }
        }
        return baseClass;
    }

    private static Class<?> loadOrNull(String binaryName) {
        try {
            return Class.forName(binaryName, false, ShionCards.class.getClassLoader());
        } catch (Throwable ignored) {
            return null;
        }
    }
}
