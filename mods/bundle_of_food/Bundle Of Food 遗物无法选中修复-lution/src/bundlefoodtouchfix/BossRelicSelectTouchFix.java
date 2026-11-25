package bundlefoodtouchfix;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.screens.select.BossRelicSelectScreen;

/**
 * boss 宝箱的三选一遗物界面（BossRelicSelectScreen）。
 *
 * 问题：AbstractRelic.spawn(x, y) 只把 isObtained 复位为 false，并不会复位 isDone。
 * bundlecore 的 QuestRelicChestPatch 会在这个界面里额外追加第四个（任务）遗物，
 * 并且会复用同一个遗物实例（BundleCoreMain.CurrentRunsFourthBossRelicStorage /
 * ...Act2Storage）。一旦该实例之前被获得过（instantObtain 会把 isDone 置为 true），
 * 再次显示时 isDone 仍为 true，AbstractRelic.update() 就会走“已获得”分支，
 * 只调用 updateRelicPopupClick()：点按遗物只会打开遗物详情弹窗，永远无法选中/获得。
 *
 * 修复：界面打开完成（open 返回）后，把界面内所有遗物复位为“未获得”状态，
 * 使其重新走正常的可选中流程（触屏下选中 + 确认按钮）。
 */
@SpirePatch2(
    cls = "com.megacrit.cardcrawl.screens.select.BossRelicSelectScreen",
    method = "open",
    paramtypez = { java.util.ArrayList.class }
)
public final class BossRelicSelectTouchFix {
    private BossRelicSelectTouchFix() {
    }

    @SpirePostfixPatch
    public static void Postfix(BossRelicSelectScreen __instance) {
        if (__instance == null || __instance.relics == null) {
            return;
        }
        for (int i = 0; i < __instance.relics.size(); i++) {
            AbstractRelic relic = __instance.relics.get(i);
            if (relic == null) {
                continue;
            }
            // spawn() 不会复位 isDone；被复用且已获得过的实例必须在这里复位，
            // 否则点击只会打开遗物详情弹窗。
            relic.isDone = false;
            relic.isObtained = false;
        }
    }
}
