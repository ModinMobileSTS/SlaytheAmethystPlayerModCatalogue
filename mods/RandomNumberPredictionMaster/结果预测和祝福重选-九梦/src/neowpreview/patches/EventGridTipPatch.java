package neowpreview.patches;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.screens.select.GridCardSelectScreen;
import neowpreview.EventPreviewMore;

/**
 * 网格选牌界面的悬停预测（篝火精灵献祭、尖端设计师全套服务）。
 *
 * <p>放在 render 之前：原版每帧只接受一次提示排队，先排的才显示，
 * 这样提示不会被原版的卡牌提示顶掉，也就不会一闪就没了。</p>
 */
@SpirePatch2(clz = GridCardSelectScreen.class, method = "render", paramtypez = {SpriteBatch.class})
public class EventGridTipPatch {

    @SpirePrefixPatch
    public static void Prefix(Object[] __args) {
        try {
            if (__args.length > 0 && __args[0] instanceof SpriteBatch) {
                EventPreviewMore.renderGrid((SpriteBatch) __args[0]);
            }
        } catch (Throwable ignored) {
        }
    }
}
