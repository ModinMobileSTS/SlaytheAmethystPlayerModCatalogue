package spiceoverhaul.patches;

import com.evacipated.cardcrawl.mod.hubris.relics.Spice;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import spiceoverhaul.SpiceData;

/**
 * Obtaining paths.
 *
 * While a save file is being restored the vanilla loader rebuilds every relic
 * from the save, so a save containing duplicate Spice entries would increment
 * the owned Spice again on every load.  Duplicates are dropped during loading
 * and a freshly obtained Spice starts at stack 1.
 */
public class SpiceObtainPatches {

    @SpirePatch2(clz = Spice.class, method = "instantObtain",
            paramtypez = {AbstractPlayer.class, int.class, boolean.class})
    public static class InstantObtainSlot {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(Spice __instance, Object[] __args) {
            if (!SpiceData.loading) {
                return SpireReturn.Continue();
            }
            AbstractPlayer p = (AbstractPlayer) __args[0];
            if (p != null && p.hasRelic(SpiceData.ID)) {
                __instance.isDone = true;
                __instance.isObtained = true;
                __instance.discarded = true;
                return SpireReturn.Return();
            }
            return SpireReturn.Continue();
        }

        @SpirePostfixPatch
        public static void Postfix() {
            SpiceData.ensureFirstLayer();
        }
    }

    @SpirePatch2(clz = Spice.class, method = "instantObtain", paramtypez = {})
    public static class InstantObtain {
        @SpirePostfixPatch
        public static void Postfix() {
            SpiceData.ensureFirstLayer();
        }
    }

    @SpirePatch2(clz = Spice.class, method = "obtain", paramtypez = {})
    public static class Obtain {
        @SpirePostfixPatch
        public static void Postfix() {
            SpiceData.ensureFirstLayer();
        }
    }
}
