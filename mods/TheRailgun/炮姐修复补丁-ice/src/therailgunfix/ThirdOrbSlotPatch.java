package therailgunfix;

import TheRailgun.characters.TheDefault;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.screens.CharSelectInfo;

/** Gives the Railgun character three orb slots instead of two. */
@SpirePatch2(clz = TheDefault.class, method = "getLoadout", paramtypez = {})
public final class ThirdOrbSlotPatch {
    private ThirdOrbSlotPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(CharSelectInfo __result) {
        if (__result != null) {
            __result.maxOrbs = 3;
        }
    }
}
