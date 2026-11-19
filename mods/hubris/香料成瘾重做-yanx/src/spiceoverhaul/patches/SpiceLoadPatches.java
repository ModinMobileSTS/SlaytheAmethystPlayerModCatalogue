package spiceoverhaul.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import spiceoverhaul.SpiceData;

/** Keeps the Spice relic unique across save/load and relic reordering. */
public class SpiceLoadPatches {

    @SpirePatch2(clz = CardCrawlGame.class, method = "loadPlayerSave", paramtypez = {AbstractPlayer.class})
    public static class LoadPlayerSave {
        @SpirePrefixPatch
        public static void Prefix() {
            SpiceData.loading = true;
        }

        @SpirePostfixPatch
        public static void Postfix() {
            SpiceData.loading = false;
            SpiceData.afterLoad();
        }
    }

    @SpirePatch2(clz = AbstractPlayer.class, method = "reorganizeRelics", paramtypez = {})
    public static class Reorganize {
        @SpirePostfixPatch
        public static void Postfix(AbstractPlayer __instance) {
            SpiceData.dedupe(__instance);
        }
    }
}
