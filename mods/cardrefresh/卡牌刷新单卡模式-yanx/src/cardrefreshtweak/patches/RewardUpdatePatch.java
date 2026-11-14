package cardrefreshtweak.patches;

import cardrefreshtweak.RefreshUi;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.screens.CardRewardScreen;

@SpirePatch2(
    cls = "com.megacrit.cardcrawl.screens.CardRewardScreen",
    method = "update",
    paramtypez = {}
)
public final class RewardUpdatePatch {

    private RewardUpdatePatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(CardRewardScreen __instance) {
        RefreshUi.inst().onUpdate(__instance);
    }
}
