package tkfix.patch;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import java.util.IdentityHashMap;
import tkfix.Diag;
import tkfix.SkinApply;

/**
 * 诊断：记录每个玩家角色实例最初几帧实际绘制用的状态。
 */
@SpirePatch2(clz = AbstractPlayer.class, method = "renderPlayerImage", paramtypez = {SpriteBatch.class})
public class PlayerRenderProbePatch {

    private static final IdentityHashMap<Object, Integer> FRAMES = new IdentityHashMap<Object, Integer>();

    @SpirePrefixPatch
    public static void Prefix(AbstractPlayer __instance) {
        try {
            if (__instance == null) {
                return;
            }
            Integer n = FRAMES.get(__instance);
            int count = n == null ? 0 : n;
            if (count >= 2) {
                return;
            }
            FRAMES.put(__instance, count + 1);
            Diag.log("player", "cls=" + __instance.getClass().getSimpleName()
                    + " " + SkinApply.describe(__instance)
                    + " " + SkinApply.describeDraw(__instance));
        } catch (Throwable t) {
            Diag.error("player", t);
        }
    }
}
