package cardrefreshtweak.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.screens.CardRewardScreen;

/**
 * The parent mod hands out one reroll per card reward and remembers only the RewardItem instance.
 * After a save / load the reward is rebuilt, so it paid out a second time and the rerolls the
 * player had already spent came back. The accounting is done by this patch mod now.
 */
@SpirePatch2(
    cls = "cardrefresh.RefreshButton",
    method = "onRewardOpened",
    paramtypez = { CardRewardScreen.class }
)
public final class ParentGrantDisablePatch {

    private ParentGrantDisablePatch() {
    }

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix() {
        return SpireReturn.Return();
    }
}
