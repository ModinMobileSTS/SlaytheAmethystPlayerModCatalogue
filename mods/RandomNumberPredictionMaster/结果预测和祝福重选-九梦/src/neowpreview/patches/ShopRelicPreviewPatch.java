package neowpreview.patches;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.shop.StoreRelic;
import neowpreview.RelicUpgradePreview;

/** 商店里悬停磨刀石／战纹涂料时，把它实际会升级的那两张牌面画出来。 */
@SpirePatch2(clz = StoreRelic.class, method = "render", paramtypez = {SpriteBatch.class})
public class ShopRelicPreviewPatch {

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
