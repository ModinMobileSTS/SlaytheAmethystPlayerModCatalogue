package neowpreview.patches;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.screens.select.BossRelicSelectScreen;
import neowpreview.RelicUpgradePreview;

/** Boss 遗物三选一（潘多拉之盒、召唤铃、小房子等）悬停时，直接画出它实际会给的结果。 */
@SpirePatch2(clz = BossRelicSelectScreen.class, method = "render", paramtypez = {SpriteBatch.class})
public class BossRelicPreviewPatch {

    @SpirePostfixPatch
    public static void Postfix(BossRelicSelectScreen __instance, Object[] __args) {
        try {
            if (__instance == null || __instance.relics == null) {
                return;
            }
            if (__args.length == 0 || !(__args[0] instanceof SpriteBatch)) {
                return;
            }
            AbstractRelic hovered = null;
            for (AbstractRelic relic : __instance.relics) {
                if (relic != null && relic.hb.hovered) {
                    hovered = relic;
                    break;
                }
            }
            if (hovered == null) {
                return;
            }
            SpriteBatch sb = (SpriteBatch) __args[0];
            float top = hovered.hb.cY + hovered.hb.height * 0.5f;
            float bottom = hovered.hb.cY - hovered.hb.height * 0.5f;
            RelicUpgradePreview.render(sb, hovered, hovered.hb.cX, top, bottom);
        } catch (Throwable ignored) {
        }
    }
}
