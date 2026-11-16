package downfallvfxfix.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import downfallvfxfix.VfxGuard;
import java.lang.reflect.Field;
import slimebound.orbs.ChampSlime;

/**
 * Same defect as ScrapOozeSlime, in the Champ slime orb.
 * ChampSlime#cleanUpVFX calls crownVFX.finish() while crownVFX is only created in
 * postSpawnEffects(), so evoking the orb before its first rendered frame throws a
 * NullPointerException.
 */
@SpirePatch2(clz = ChampSlime.class, method = "cleanUpVFX", paramtypez = {})
public class ChampSlimeVfxFix {

    private static final Field CROWN_VFX = VfxGuard.field(ChampSlime.class, "crownVFX");

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(ChampSlime __instance) {
        Object crown = VfxGuard.get(CROWN_VFX, __instance);

        if (crown != null) {
            return SpireReturn.Continue();
        }

        // Crown effect was never created (orb evoked before its first render).
        return SpireReturn.Return();
    }
}
