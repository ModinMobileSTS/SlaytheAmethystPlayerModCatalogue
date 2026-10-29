package sts.ryoikitenkai.compat.green;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.tempCards.Shiv;
import com.megacrit.cardcrawl.characters.AbstractPlayer;

/**
 * 使用小刀时的「精准」反馈。
 * <h2>为什么挂在 {@code Shiv.use}</h2>
 * 精准的实际效果就是让每一把小刀打得更疼：{@code AccuracyPower.updateExistingShivs()}
 * 与 {@code onDrawOrDiscard()} 会把每张 {@code Shiv} 的 {@code baseDamage} 改成
 * {@code 4 + amount}（升级版 {@code 6 + amount}）。
 * 所以「精准真正生效」的那一瞬，就是<b>这张小刀被打出</b>的时候。
 * <p>
 * 挂 Postfix 而不是 Prefix：{@code DamageAction} 是在 {@code use()} 里才被加入行动队列的，
 * Postfix 时这张牌的状态与去向都已经确定。
 * <p>
 * 注意：这里<b>不</b>挂 {@code AccuracyPower.onDrawOrDiscard}。
 * {@code onDrawOrDiscard} 每次抽牌/弃牌都会遍历手牌改伤害，
 * 但它并没有「哪张牌被打出去」的信息，挂在它上面会把特效撒在「抽牌」这个动作上，
 * 而精确属于「小刀命中」（规范 §2.3：视觉必须来自机制，先问生效条件是什么）。
 */
@SpirePatch2(
    cls = "com.megacrit.cardcrawl.cards.tempCards.Shiv",
    method = "use",
    paramtypez = {
        com.megacrit.cardcrawl.characters.AbstractPlayer.class,
        com.megacrit.cardcrawl.monsters.AbstractMonster.class
    }
)
public final class ShivHitPatch {
    private ShivHitPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(Shiv __instance, Object[] __args) {
        GreenEffectRegistry.shivUsed((AbstractPlayer) __args[0], __instance);
    }
}
