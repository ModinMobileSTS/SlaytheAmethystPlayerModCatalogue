package heartfx;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;

/**
 * 保险：万一演出过程中战斗被打断、归位没能播放，下一场战斗开始时把纸片人的
 * 动画偏移清零，避免蕾忍模型停在错误位置。
 */
@SpirePatch2(
        cls = "com.megacrit.cardcrawl.characters.AbstractPlayer",
        method = "preBattlePrep",
        paramtypez = {}
)
public final class PlayerPrepResetPatch {

    private PlayerPrepResetPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance) {
        if (__instance == null) {
            return;
        }
        __instance.animX = 0.0f;
        __instance.animY = 0.0f;
    }
}
