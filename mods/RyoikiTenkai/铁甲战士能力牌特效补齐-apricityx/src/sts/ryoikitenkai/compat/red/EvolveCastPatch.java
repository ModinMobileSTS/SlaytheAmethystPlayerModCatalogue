package sts.ryoikitenkai.compat.red;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.cards.red.Evolve;
import com.megacrit.cardcrawl.characters.AbstractPlayer;

/**
 * 打出「进化」时：绿色的卡片轮廓闪光 + 从脚底涌起的能量。
 * <p>
 * 用 Prefix（不替换原方法），只插视觉，卡牌结算照常。
 */
@SpirePatch2(
    cls = "com.megacrit.cardcrawl.cards.red.Evolve",
    method = "use",
    paramtypez = { com.megacrit.cardcrawl.characters.AbstractPlayer.class, com.megacrit.cardcrawl.monsters.AbstractMonster.class }
)
public final class EvolveCastPatch {
    private EvolveCastPatch() {
    }

    @SpirePrefixPatch
    public static void Prefix(Evolve __instance, Object[] __args) {
        RedEffectRegistry.castEvolve((AbstractPlayer) __args[0], __instance);
    }
}
