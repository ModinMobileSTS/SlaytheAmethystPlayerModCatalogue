package generousfix;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.screens.select.BossRelicSelectScreen;

/**
 * Vanilla BossRelicSelectScreen.updateControllerInput() assumes the screen always holds
 * exactly three relics (it calls relics.get(1) / relics.get(2), and blights.get(1) when the
 * list is empty). The parent mod removes every relic the player already took, so after the
 * first pick pressing left/right on a controller threw IndexOutOfBoundsException.
 */
@SpirePatch2(clz = BossRelicSelectScreen.class, method = "updateControllerInput", paramtypez = {})
public class ControllerInputPatch {

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(BossRelicSelectScreen __instance) {
        boolean safe;
        if (__instance.relics == null || __instance.relics.isEmpty()) {
            safe = __instance.blights != null && __instance.blights.size() >= 2;
        } else {
            safe = __instance.relics.size() >= 3;
        }
        if (safe) {
            return SpireReturn.Continue();
        }
        return SpireReturn.Return();
    }
}
