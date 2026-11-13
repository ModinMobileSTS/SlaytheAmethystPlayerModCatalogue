package tkfix.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import timekeepermod.patch.SkinSelectScreen_red;
import tkfix.Diag;
import tkfix.SkinFixConfig;

/**
 * 母模组把三个界面的皮肤共用同一个存档键 "TimeCutter:skin"，
 * 读档时会把铁甲战士的皮肤序号改写成存档里的值（通常是 0 = 原版外观），
 * 于是「选人界面立绘正常、进入游戏却变回默认」。
 * 这里在读档后立刻把独立配置里记录的序号写回去。
 */
@SpirePatch2(clz = SkinSelectScreen_red.class, method = "onLoad", paramtypez = {Integer.class})
public class RedSaveLoadPatch {

    @SpirePostfixPatch
    public static void Postfix(SkinSelectScreen_red __instance) {
        try {
            Diag.log("save", "onLoad red idx=" + __instance.index + " cfg=" + SkinFixConfig.savedIndex(true));
            SkinFixConfig.restoreAfterLoad(true);
        } catch (Throwable t) {
            Diag.error("save.red", t);
        }
    }
}
