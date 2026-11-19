package mpdownfallmap;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;

/**
 * Downfall 的崩坠地图就是在 AbstractDungeon.generateMap 里被翻转的
 * （FlipMap.StandardMapFlipper）。生成地图之前必须让 evilMode 已经正确，
 * 于是这里再核对一次（第一幕、第二幕、第三幕重新生成地图时都会走这里）。
 */
@SpirePatch2(
        cls = "com.megacrit.cardcrawl.dungeons.AbstractDungeon",
        method = "generateMap",
        paramtypez = {}
)
public final class MapGenerationSyncPatch {
    private MapGenerationSyncPatch() {
    }

    @SpirePrefixPatch
    public static void Prefix() {
        LobbyDownfallMapSync.sync();
    }
}
