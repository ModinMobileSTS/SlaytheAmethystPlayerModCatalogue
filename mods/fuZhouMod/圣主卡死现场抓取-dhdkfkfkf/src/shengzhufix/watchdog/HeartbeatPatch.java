package shengzhufix.watchdog;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

/**
 * 心跳点一：主循环 update()。每帧刷新一次时间戳，供看门狗判断主线程是否还活着。
 */
@SpirePatch2(
    cls = "com.megacrit.cardcrawl.core.CardCrawlGame",
    method = "update",
    paramtypez = {}
)
public final class HeartbeatPatch {
    private HeartbeatPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(com.megacrit.cardcrawl.core.CardCrawlGame __instance) {
        Watchdog.tick();
    }
}
