package simplemapdownfall;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.TheEnding;

/**
 * 终幕（心脏）专用地图：generateSpecialMap 里崩坠同样会翻转并重连 Boss 边，
 * 同样在方法返回后按收缩网格重新排布。
 */
@SpirePatch2(
    cls = "com.megacrit.cardcrawl.dungeons.TheEnding",
    method = "generateSpecialMap",
    paramtypez = {}
)
public final class FinalActMapLayoutPatch {
    private FinalActMapLayoutPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(TheEnding __instance) {
        DownfallMapAdapter.redress();
    }
}
