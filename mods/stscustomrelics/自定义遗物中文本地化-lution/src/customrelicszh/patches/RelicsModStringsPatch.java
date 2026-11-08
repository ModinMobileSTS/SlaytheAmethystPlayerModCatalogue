package customrelicszh.patches;

import basemod.BaseMod;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.localization.RelicStrings;
import customrelicszh.ZhText;

/**
 * 父模组在 receiveEditStrings 中直接注册英文 RelicStrings。
 * 在该方法之后追加注册中文文本，覆盖顺序稳定（不依赖两个模组的订阅顺序）。
 * 遗物对象在随后的 receiveEditRelics 阶段才创建，因此名称与风味文本会取到中文。
 */
@SpirePatch2(
    cls = "com.example.relicsmod.RelicsMod",
    method = "receiveEditStrings",
    paramtypez = {}
)
public final class RelicsModStringsPatch {
    private RelicsModStringsPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix() {
        if (ZhText.isChinese()) {
            BaseMod.loadCustomStrings(RelicStrings.class, ZhText.relicStringsJson());
        }
    }
}
