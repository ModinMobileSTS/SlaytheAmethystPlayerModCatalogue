package intentgraphhoverrule;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import io.chaofan.sts.intentgraph.IntentGraphMod;

/**
 * Intent Graph 每帧的 PostRender 入口，用来刷新悬停进入记录。
 * 它在同一次调用里接着绘制状态机，因此这里刷新后，
 * 本帧的绘制判定拿到的就是「本帧」的悬停状态。
 */
@SpirePatch2(clz = IntentGraphMod.class, method = "receivePostRender", paramtypez = {SpriteBatch.class})
public class FrameTickPatch {

    @SpirePrefixPatch
    public static void Prefix() {
        HoverEntryTracker.refresh();
    }
}
