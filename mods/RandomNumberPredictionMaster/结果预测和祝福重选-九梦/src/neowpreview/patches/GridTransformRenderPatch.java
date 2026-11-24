package neowpreview.patches;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.screens.select.GridCardSelectScreen;
import neowpreview.TransformPredictor;

/** 多选变形（如涅奥“变形两张牌”、阿斯特罗莱布）没有确认界面，这里在右下角补上结果预览。 */
@SpirePatch2(clz = GridCardSelectScreen.class, method = "render", paramtypez = {SpriteBatch.class})
public class GridTransformRenderPatch {

    @SpirePostfixPatch
    public static void Postfix(Object[] __args) {
        try {
            if (__args.length > 0 && __args[0] instanceof SpriteBatch) {
                TransformPredictor.renderBeside((SpriteBatch) __args[0]);
                neowpreview.EventPreviewMore.renderGridCard((SpriteBatch) __args[0]);
            }
        } catch (Throwable ignored) {
        }
    }
}
