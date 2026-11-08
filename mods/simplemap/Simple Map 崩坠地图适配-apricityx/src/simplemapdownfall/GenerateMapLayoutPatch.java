package simplemapdownfall;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

/**
 * 普通地图：generateMap 内部会完成建图、崩坠翻图、分配房间和翡翠精英，
 * 在方法返回后统一按 Simple Map 的收缩网格重新排布。
 */
@SpirePatch2(
    cls = "com.megacrit.cardcrawl.dungeons.AbstractDungeon",
    method = "generateMap",
    paramtypez = {}
)
public final class GenerateMapLayoutPatch {
    private GenerateMapLayoutPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix() {
        DownfallMapAdapter.redress();
    }
}
