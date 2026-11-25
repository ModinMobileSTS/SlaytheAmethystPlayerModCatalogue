package neowpreview.patches;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.screens.CombatRewardScreen;
import neowpreview.RelicUpgradePreview;

/** 奖励界面悬停会预测的遗物奖励时：前缀排提示框（遗物说明 + 详情），后缀画卡面。 */
@SpirePatch2(clz = CombatRewardScreen.class, method = "render", paramtypez = {SpriteBatch.class})
public class RewardRelicPreviewPatch {

    @SpirePrefixPatch
    public static void Prefix(CombatRewardScreen __instance) {
        try {
            RewardItem hovered = hovered(__instance);
            if (hovered != null) {
                RelicUpgradePreview.queueTips(hovered.relic);
            }
        } catch (Throwable ignored) {
        }
    }

    @SpirePostfixPatch
    public static void Postfix(CombatRewardScreen __instance, Object[] __args) {
        try {
            RewardItem hovered = hovered(__instance);
            if (hovered == null) {
                return;
            }
            if (__args.length == 0 || !(__args[0] instanceof SpriteBatch)) {
                return;
            }
            SpriteBatch sb = (SpriteBatch) __args[0];
            float top = hovered.hb.cY + hovered.hb.height * 0.5f;
            float bottom = hovered.hb.cY - hovered.hb.height * 0.5f;
            RelicUpgradePreview.render(sb, hovered.relic, hovered.hb.cX, top, bottom);
        } catch (Throwable ignored) {
        }
    }

    private static RewardItem hovered(CombatRewardScreen screen) {
        if (screen == null || screen.rewards == null) {
            return null;
        }
        for (RewardItem item : screen.rewards) {
            if (item == null || item.relic == null) {
                continue;
            }
            if (item.type == RewardItem.RewardType.RELIC && item.hb.hovered) {
                return item;
            }
        }
        return null;
    }
}
