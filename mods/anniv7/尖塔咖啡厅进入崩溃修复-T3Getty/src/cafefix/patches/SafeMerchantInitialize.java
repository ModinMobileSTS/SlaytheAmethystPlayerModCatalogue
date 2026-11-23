package cafefix.patches;

import cafefix.CafeSafety;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import spireCafe.abstracts.AbstractMerchant;

/**
 * AbstractMerchant.initialize() runs the merchant's shop roll while the cafe room is entered, with
 * no error handling: a failing roll (empty pools, other mods' odd relic or card data) aborts the
 * room entry. The roll is now guarded, and the merchant simply offers an empty shop in that case.
 */
@SpirePatch2(clz = AbstractMerchant.class, method = "initialize", paramtypez = {})
public class SafeMerchantInitialize {

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(AbstractMerchant __instance) {
        if (!__instance.wasShopRolled) {
            try {
                CafeSafety.rollShop(__instance);
            } catch (Throwable t) {
                CafeSafety.LOG.warn("[CafeFix] Shop roll of " + __instance.getClass().getName()
                        + " failed; the merchant will show an empty shop instead of aborting the room.", t);
            }
            __instance.wasShopRolled = true;
        }
        return SpireReturn.Return();
    }
}
