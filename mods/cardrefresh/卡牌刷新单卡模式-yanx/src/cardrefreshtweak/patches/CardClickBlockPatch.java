package cardrefreshtweak.patches;

import cardrefreshtweak.RefreshUi;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;

/**
 * While the player presses one of the refresh buttons the vanilla card handling must not run,
 * otherwise that same press could also pick the card underneath the button.
 */
@SpirePatch2(
    cls = "com.megacrit.cardcrawl.screens.CardRewardScreen",
    method = "cardSelectUpdate",
    paramtypez = {}
)
public final class CardClickBlockPatch {

    private CardClickBlockPatch() {
    }

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix() {
        if (RefreshUi.inst().blocksCardClick()) {
            return SpireReturn.Return();
        }
        return SpireReturn.Continue();
    }
}
