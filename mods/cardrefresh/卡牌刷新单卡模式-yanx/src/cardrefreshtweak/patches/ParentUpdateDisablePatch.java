package cardrefreshtweak.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.screens.CardRewardScreen;

/**
 * The parent mod's own button is hidden; this patch mod draws and handles it instead so that the
 * spent rerolls survive save / load.
 */
@SpirePatch2(
    cls = "cardrefresh.RefreshButton",
    method = "onScreenUpdate",
    paramtypez = { CardRewardScreen.class }
)
public final class ParentUpdateDisablePatch {

    private ParentUpdateDisablePatch() {
    }

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix() {
        return SpireReturn.Return();
    }
}
