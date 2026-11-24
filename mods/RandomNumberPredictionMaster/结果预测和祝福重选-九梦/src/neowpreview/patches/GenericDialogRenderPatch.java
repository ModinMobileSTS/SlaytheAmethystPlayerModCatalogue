package neowpreview.patches;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.events.GenericEventDialog;
import neowpreview.EventPreview;
import neowpreview.Reflect;

/** 图像事件的对话框画完后，叠加事件随机结果的预测。 */
@SpirePatch2(clz = GenericEventDialog.class, method = "render", paramtypez = {SpriteBatch.class})
public class GenericDialogRenderPatch {

    @SpirePostfixPatch
    public static void Postfix(GenericEventDialog __instance, Object[] __args) {
        try {
            if (__args.length > 0 && __args[0] instanceof SpriteBatch) {
                EventPreview.render((SpriteBatch) __args[0], Reflect.get(__instance, "optionList"));
            }
        } catch (Throwable ignored) {
        }
    }
}
