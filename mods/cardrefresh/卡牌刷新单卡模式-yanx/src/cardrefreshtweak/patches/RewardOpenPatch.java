package cardrefreshtweak.patches;

import cardrefreshtweak.RefreshUi;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.screens.CardRewardScreen;
import java.util.ArrayList;

@SpirePatch2(
    cls = "com.megacrit.cardcrawl.screens.CardRewardScreen",
    method = "open",
    paramtypez = { ArrayList.class, RewardItem.class, String.class }
)
public final class RewardOpenPatch {

    private RewardOpenPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(CardRewardScreen __instance) {
        RefreshUi.inst().onOpen(__instance);
    }
}
