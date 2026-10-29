package sts.ryoikitenkai.compat.red;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.powers.EvolvePower;

/**
 * 「进化」真正生效的位置：{@code EvolvePower.onCardDraw(AbstractCard)}。
 * <p>
 * <h2>为什么必须把 card 传下去</h2>
 * 原版在这一刻做的事是「抽到的这张如果是状态牌，就额外抽几张」。
 * 所以<b>那张牌本身就是这次触发的全部信息</b>：
 * <ul>
 *   <li>是不是状态牌（原版判据 {@code card.type == STATUS}）</li>
 *   <li>它现在在屏幕哪里 —— 决定了「标记」与「吸收弧」的起点</li>
 * </ul>
 * 旧实现只传了 {@code owner}，于是演出只能做成「从脚下往上冲的光点」，
 * 与「抽到哪一张牌」没有任何对应关系。
 * <p>
 * <h2>为什么 card 的位置是准确的</h2>
 * {@code AbstractPlayer.draw(int)} 的顺序是：
 * <pre>
 * c.current_x = CardGroup.DRAW_PILE_X;   // 先摆到抽牌堆位置
 * c.current_y = CardGroup.DRAW_PILE_Y;
 * c.drawScale = 0.12f;                   // 此时还很小，正在飞向手牌
 * c.triggerWhenDrawn();
 * this.hand.addToHand(c);
 * this.drawPile.removeTopCard();
 * for (AbstractPower p : this.powers) p.onCardDraw(c);   // ← 本钩子
 * </pre>
 * 所以在 postfix 里读 {@code card.current_x/current_y} 得到的是
 * <b>抽牌堆坐标</b>，而不是手牌位置 —— 这恰好就是「脏牌刚从牌堆被抽出来」
 * 这个事实，因果链起点因此是准确的。
 * <p>
 * <h2>条件判断放在哪</h2>
 * 本钩子<b>不</b>自己判断 {@code STATUS} 与 {@code No Draw}，
 * 而是把判断收在 {@link RedEffectRegistry#onCardDrawn} 里，
 * 保证「判据与原版逐字对齐」这件事只有一个出处（规范 §六「共享常量」的同类要求）。
 */
@SpirePatch2(
    cls = "com.megacrit.cardcrawl.powers.EvolvePower",
    method = "onCardDraw",
    paramtypez = { AbstractCard.class }
)
public final class EvolveDrawPatch {
    private EvolveDrawPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(EvolvePower __instance, Object[] __args) {
        AbstractCard card = __args[0] instanceof AbstractCard
                ? (AbstractCard) __args[0] : null;
        if (__instance.owner == null || card == null) {
            return;
        }
        RedEffectRegistry.onCardDrawn(__instance.owner, card);
    }
}
