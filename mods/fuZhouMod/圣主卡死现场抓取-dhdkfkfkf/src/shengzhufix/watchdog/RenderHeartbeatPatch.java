package shengzhufix.watchdog;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

/**
 * 心跳点二：主循环 render()。render 每帧必然调用一次，即使卡死发生在 update 内部，
 * 这一帧的 render 也不会执行，看门狗同样能发现停顿。
 */
@SpirePatch2(
    cls = "com.megacrit.cardcrawl.core.CardCrawlGame",
    method = "render",
    paramtypez = {}
)
public final class RenderHeartbeatPatch {
    private RenderHeartbeatPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(com.megacrit.cardcrawl.core.CardCrawlGame __instance) {
        Watchdog.tick();
    }
}
