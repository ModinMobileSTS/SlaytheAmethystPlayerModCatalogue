package customrelicszh.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import customrelicszh.ZhText;

@SpirePatch2(
    cls = "com.example.relicsmod.relics.HourglassOfTime",
    method = "getUpdatedDescription",
    paramtypez = {}
)
public final class HourglassOfTimeDescPatch {
    private HourglassOfTimeDescPatch() {
    }

    @SpirePrefixPatch
    public static SpireReturn Prefix() {
        if (!ZhText.isChinese()) {
            return SpireReturn.Continue();
        }
        return SpireReturn.Return(ZhText.HOURGLASS_OF_TIME_DESC);
    }
}
