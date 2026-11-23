package cafefix.patches;

import cafefix.CafeSafety;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import java.util.List;
import spireCafe.interactables.patrons.spiomesmanifestation.SpiomesManifestationPatron;

/**
 * Fixes the Spire Biomes patron crash. The original rollBiomes() ends with
 * possibleBiomes.subList(0, 3); once fewer than three biomes are eligible (later acts, or when most
 * zones were already visited in this run) that throws IndexOutOfBoundsException out of the patron
 * constructor, which aborts entering the cafe room. The replacement offers however many biomes are
 * available, and turns reflection failures into an empty list instead of an exception.
 */
@SpirePatch2(clz = SpiomesManifestationPatron.class, method = "rollBiomes", paramtypez = {})
public class SafeRollBiomes {

    @SpirePrefixPatch
    public static SpireReturn<List> Prefix(SpiomesManifestationPatron __instance) {
        return SpireReturn.Return(CafeSafety.rollBiomes(__instance));
    }
}
