package sts.ryoikitenkai.compat.green;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.tempCards.Shiv;
import com.megacrit.cardcrawl.characters.AbstractPlayer;

/**
 * 使用小刀时的「精准」反馈。
 * <p>
 * 精准的实际效果就是让小刀打得更疼，所以视觉挂在 {@code Shiv.use} 上：
 * 每次扔出小刀，身上绿光闪一下，读作「这一下被校准过」。
 * 没有精准能力时 {@link GreenEffectRegistry#shivUsed} 会自己返回。
 */
@SpirePatch2(
    cls = "com.megacrit.cardcrawl.cards.tempCards.Shiv",
    method = "use",
    paramtypez = { com.megacrit.cardcrawl.characters.AbstractPlayer.class, com.megacrit.cardcrawl.monsters.AbstractMonster.class }
)
public final class ShivHitPatch {
    private ShivHitPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(Shiv __instance, Object[] __args) {
        GreenEffectRegistry.shivUsed((AbstractPlayer) __args[0]);
    }
}
