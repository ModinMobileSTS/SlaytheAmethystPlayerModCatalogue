package downfallvfxfix.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import downfallvfxfix.VfxGuard;
import java.lang.reflect.Field;
import slimebound.orbs.ScrapOozeSlime;

/**
 * Fixes the reported crash:
 * java.lang.NullPointerException at slimebound.orbs.ScrapOozeSlime.cleanUpVFX(ScrapOozeSlime.java:72)
 *
 * scrapVFX / scrapGlowVFX are only created in postSpawnEffects(), which is called from
 * the first rendered frame of the orb. When the orb is evoked before that frame (channel
 * overflow on battle start, co-op room / battle sync, ...) cleanUpVFX() dereferences null.
 *
 * The prefix below finishes whatever effect object exists and skips the original method
 * when one of them is missing, so the evoke no longer throws.
 */
@SpirePatch2(clz = ScrapOozeSlime.class, method = "cleanUpVFX", paramtypez = {})
public class ScrapOozeSlimeVfxFix {

    private static final Field SCRAP_VFX = VfxGuard.field(ScrapOozeSlime.class, "scrapVFX");
    private static final Field SCRAP_GLOW_VFX = VfxGuard.field(ScrapOozeSlime.class, "scrapGlowVFX");

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(ScrapOozeSlime __instance) {
        Object scrap = VfxGuard.get(SCRAP_VFX, __instance);
        Object glow = VfxGuard.get(SCRAP_GLOW_VFX, __instance);

        if (scrap != null && glow != null) {
            // Both particle effects were created: run the vanilla clean up.
            return SpireReturn.Continue();
        }

        // The orb never rendered, so its effects do not exist yet. Finish what is
        // there (nothing, normally) and skip the crashing original method.
        VfxGuard.finish(scrap);
        VfxGuard.finish(glow);
        __instance.evoked = true;
        return SpireReturn.Return();
    }
}
