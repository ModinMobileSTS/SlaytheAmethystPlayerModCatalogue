package neowpreview.patches;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.screens.CombatRewardScreen;
import neowpreview.RelicUpgradePreview;

/** 奖励界面悬停磨刀石／战纹涂料这类遗物奖励时，同样画出会被升级的牌面。 */
@SpirePatch2(clz = CombatRewardScreen.class, method = "render", paramtypez = {SpriteBatch.class})
public class RewardRelicPreviewPatch {

    @SpirePostfixPatch
    public static void Postfix(CombatRewardScreen __instance, Object[] __args) {
        try {
            if (__instance == null || __instance.rewards == null) {
                return;
            }
            if (__args.length == 0 || !(__args[0] instanceof SpriteBatch)) {
                return;
            }
            RewardItem hovered = null;
            for (RewardItem item : __instance.rewards) {
                if (item == null || item.relic == null) {
                    continue;
                }
                if (item.type == RewardItem.RewardType.RELIC && item.hb.hovered) {
                    hovered = item;
                    break;
                }
            }
            if (hovered == null) {
                return;
            }
            SpriteBatch sb = (SpriteBatch) __args[0];
            float top = hovered.hb.cY + hovered.hb.height * 0.5f;
            float bottom = hovered.hb.cY - hovered.hb.height * 0.5f;
            RelicUpgradePreview.render(sb, hovered.relic, hovered.hb.cX, top, bottom);
        } catch (Throwable ignored) {
        }
    }
}
