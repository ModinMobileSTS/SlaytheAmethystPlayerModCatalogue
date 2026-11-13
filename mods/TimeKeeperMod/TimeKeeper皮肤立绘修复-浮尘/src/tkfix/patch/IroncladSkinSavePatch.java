package tkfix.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import timekeepermod.patch.SkinSelectScreen_red;
import tkfix.Diag;
import tkfix.SkinFixConfig;

/**
 * 铁甲战士切换皮肤（点击左右箭头）时，把当前序号保存到独立配置文件。
 */
@SpirePatch2(clz = SkinSelectScreen_red.class, method = "refresh_red", paramtypez = {})
public class IroncladSkinSavePatch {

    @SpirePostfixPatch
    public static void Postfix(SkinSelectScreen_red __instance) {
        Diag.log("red", "refresh idx=" + __instance.index + " name=" + __instance.curName
                + " next=" + __instance.nextName);
        SkinFixConfig.save(SkinFixConfig.KEY_IRONCLAD, __instance.index);
    }
}
