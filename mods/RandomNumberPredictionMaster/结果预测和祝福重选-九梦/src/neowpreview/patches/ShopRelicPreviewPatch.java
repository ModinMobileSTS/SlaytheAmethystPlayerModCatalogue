package neowpreview.patches;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.shop.StoreRelic;
import neowpreview.RelicUpgradePreview;

/**
 * 商店里悬停会预测的遗物（磨刀石、战纹涂料、潘多拉之盒、召唤铃、坩埚、天球仪、小房子）：
 * 前缀先把「遗物说明 + 预测出的遗物／药水详情」排给原版提示框（原版每帧只接受一次排队，所以要抢在前面），
 * 后缀再把预测出的卡面画出来。
 */
@SpirePatch2(clz = StoreRelic.class, method = "render", paramtypez = {SpriteBatch.class})
public class ShopRelicPreviewPatch {

    @SpirePrefixPatch
    public static void Prefix(StoreRelic __instance) {
        try {
            if (__instance == null || __instance.relic == null || __instance.isPurchased) {
                return;
            }
            if (!__instance.relic.hb.hovered) {
                return;
            }
            RelicUpgradePreview.queueTips(__instance.relic);
        } catch (Throwable ignored) {
        }
    }

    @SpirePostfixPatch
    public static void Postfix(StoreRelic __instance, Object[] __args) {
        try {
            if (__instance == null || __instance.relic == null || __instance.isPurchased) {
                return;
            }
            if (!__instance.relic.hb.hovered) {
                return;
            }
            if (__args.length == 0 || !(__args[0] instanceof SpriteBatch)) {
                return;
            }
            SpriteBatch sb = (SpriteBatch) __args[0];
            float top = __instance.relic.hb.cY + __instance.relic.hb.height * 0.5f;
            float bottom = __instance.relic.hb.cY - __instance.relic.hb.height * 0.5f;
            RelicUpgradePreview.render(sb, __instance.relic, __instance.relic.hb.cX, top, bottom);
        } catch (Throwable ignored) {
        }
    }
}
