package neowpreview.patches;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.screens.select.BossRelicSelectScreen;
import neowpreview.RelicUpgradePreview;

/** Boss 遗物三选一：前缀排提示框（遗物说明 + 详情），后缀画卡面。 */
@SpirePatch2(clz = BossRelicSelectScreen.class, method = "render", paramtypez = {SpriteBatch.class})
public class BossRelicPreviewPatch {

    @SpirePrefixPatch
    public static void Prefix(BossRelicSelectScreen __instance) {
        try {
            AbstractRelic hovered = hovered(__instance);
            if (hovered != null) {
                RelicUpgradePreview.queueTips(hovered);
            }
        } catch (Throwable ignored) {
        }
    }

    @SpirePostfixPatch
    public static void Postfix(BossRelicSelectScreen __instance, Object[] __args) {
        try {
            AbstractRelic hovered = hovered(__instance);
            if (hovered == null) {
                return;
            }
            if (__args.length == 0 || !(__args[0] instanceof SpriteBatch)) {
                return;
            }
            SpriteBatch sb = (SpriteBatch) __args[0];
            float top = hovered.hb.cY + hovered.hb.height * 0.5f;
            float bottom = hovered.hb.cY - hovered.hb.height * 0.5f;
            RelicUpgradePreview.render(sb, hovered, hovered.hb.cX, top, bottom);
        } catch (Throwable ignored) {
        }
    }

    private static AbstractRelic hovered(BossRelicSelectScreen screen) {
        if (screen == null || screen.relics == null) {
            return null;
        }
        for (AbstractRelic relic : screen.relics) {
            if (relic != null && relic.hb.hovered) {
                return relic;
            }
        }
        return null;
    }
}
