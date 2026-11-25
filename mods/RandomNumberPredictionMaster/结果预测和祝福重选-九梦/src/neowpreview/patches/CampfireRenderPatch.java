package neowpreview.patches;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.rooms.CampfireUI;
import neowpreview.CampfirePreview;

/** 篝火选项（挖、休息）悬停时的随机结果预览，排在所有按钮之后绘制，避免被后面的按钮盖住。 */
@SpirePatch2(clz = CampfireUI.class, method = "render", paramtypez = {SpriteBatch.class})
public class CampfireRenderPatch {

    @SpirePostfixPatch
    public static void Postfix(CampfireUI __instance, Object[] __args) {
        try {
            if (__args.length == 0 || !(__args[0] instanceof SpriteBatch)) {
                return;
            }
            CampfirePreview.render((SpriteBatch) __args[0], __instance);
        } catch (Throwable ignored) {
        }
    }
}
