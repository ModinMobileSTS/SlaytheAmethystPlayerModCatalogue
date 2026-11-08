package customrelicszh.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import customrelicszh.ZhText;

/**
 * 父模组的 getUpdatedDescription 直接返回硬编码英文，会盖住 RelicStrings 中的描述，
 * 因此这里在中文环境下直接替换返回值。
 */
@SpirePatch2(
    cls = "com.example.relicsmod.relics.BagOfGreed",
    method = "getUpdatedDescription",
    paramtypez = {}
)
public final class BagOfGreedDescPatch {
    private BagOfGreedDescPatch() {
    }

    @SpirePrefixPatch
    public static SpireReturn Prefix() {
        if (!ZhText.isChinese()) {
            return SpireReturn.Continue();
        }
        return SpireReturn.Return(ZhText.BAG_OF_GREED_DESC);
    }
}
