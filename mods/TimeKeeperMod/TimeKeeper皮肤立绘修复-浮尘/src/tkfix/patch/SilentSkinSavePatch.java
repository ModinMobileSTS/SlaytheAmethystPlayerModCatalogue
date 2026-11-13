package tkfix.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import timekeepermod.patch.SkinSelectScreen_green;
import tkfix.Diag;
import tkfix.SkinFixConfig;

/**
 * 静默猎手切换皮肤（点击左右箭头）时，把当前序号保存到独立配置文件。
 * 母模组的三个皮肤界面共用同一个存档键「TimeCutter:skin」，实际只有一个能生效，
 * 因此这里改用单独的配置文件持久化。
 */
@SpirePatch2(clz = SkinSelectScreen_green.class, method = "refresh_green", paramtypez = {})
public class SilentSkinSavePatch {

    @SpirePostfixPatch
    public static void Postfix(SkinSelectScreen_green __instance) {
        Diag.log("green", "refresh idx=" + __instance.index + " name=" + __instance.curName);
        SkinFixConfig.save(SkinFixConfig.KEY_SILENT, __instance.index);
    }
}
