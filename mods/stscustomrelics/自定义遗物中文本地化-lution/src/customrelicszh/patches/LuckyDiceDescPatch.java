package customrelicszh.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import customrelicszh.ZhText;

@SpirePatch2(
    cls = "com.example.relicsmod.relics.LuckyDice",
    method = "getUpdatedDescription",
    paramtypez = {}
)
public final class LuckyDiceDescPatch {
    private LuckyDiceDescPatch() {
    }

    @SpirePrefixPatch
    public static SpireReturn Prefix() {
        if (!ZhText.isChinese()) {
            return SpireReturn.Continue();
        }
        return SpireReturn.Return(ZhText.LUCKY_DICE_DESC);
    }
}
