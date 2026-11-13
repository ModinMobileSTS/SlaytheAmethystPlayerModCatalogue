package tkfix.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import tkfix.Diag;
import tkfix.SkinApply;

/**
 * 角色创建完成（进入游戏 / 读档 / 选人界面预览）后，确认骨架已经是所选皮肤。
 * 母模组只在构造函数 Postfix 里换一次骨架，失败就再也回不来；
 * 这里在角色真正创建完成后补一次。
 */
@SpirePatch2(clz = CardCrawlGame.class, method = "createCharacter",
        paramtypez = {AbstractPlayer.PlayerClass.class})
public class InGameSkinPatch {

    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __result) {
        try {
            SkinApply.ensureOnce(__result, "game");
        } catch (Throwable t) {
            Diag.error("game", t);
        }
    }
}
