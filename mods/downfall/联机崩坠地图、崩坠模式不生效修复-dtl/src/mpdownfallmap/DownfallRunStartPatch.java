package mpdownfallmap;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import downfall.downfallMod;

/**
 * 一局游戏刚开始时，Downfall 就在 receiveStartGame 里按 evilMode 决定
 * “金币还是灵魂”（GoldToSoulPatches.changeGoldToSouls），
 * receiveStartAct 里又按它决定 Boss 池、事件替换。
 * 所以这里在 Downfall 自己的逻辑执行之前，先把联机房间的意图同步进 evilMode。
 *
 * 联机中文补丁复制的那份 Embark 跳过了联机模组挂在大厅 Embark 上的补丁，
 * 这一步就是把漏掉的赋值补回来，不依赖任何大厅 UI 实现。
 */
@SpirePatch2(
        clz = downfallMod.class,
        method = "receiveStartGame",
        paramtypez = {}
)
public final class DownfallRunStartPatch {
    private DownfallRunStartPatch() {
    }

    @SpirePrefixPatch
    public static void Prefix() {
        LobbyDownfallMapSync.sync();
    }
}
