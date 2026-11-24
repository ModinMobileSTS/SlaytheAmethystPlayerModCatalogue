package neowpreview.patches;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.events.RoomEventDialog;
import neowpreview.EventPreview;
import neowpreview.NeowState;

/** 对话框画完后叠加结果预览图。 */
@SpirePatch2(clz = RoomEventDialog.class, method = "render", paramtypez = {SpriteBatch.class})
public class RoomDialogRenderPatch {

    @SpirePostfixPatch
    public static void Postfix(Object[] __args) {
        try {
            if (__args.length > 0 && __args[0] instanceof SpriteBatch) {
                SpriteBatch sb = (SpriteBatch) __args[0];
                NeowState.renderPreviews(sb);
                EventPreview.render(sb, RoomEventDialog.optionList);
            }
        } catch (Throwable ignored) {
        }
    }
}
