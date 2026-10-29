package sts.ryoikitenkai.compat.green;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

/**
 * 在<b>卡牌自己的渲染流程里</b>给「小刀」画附魔边框。
 * <h2>为什么挂在卡牌渲染上</h2>
 * 卡框若是一个独立特效，就必须自己每帧推算卡牌的位置、缩放与旋转；
 * 只要有一项算错（例如漏了 {@code Settings.scale}）就会和卡牌脱开。
 * <p>
 * 挂在 {@code AbstractCard.render} 的 <b>Postfix</b> 上，就能在同一帧、同一坐标系里作画，
 * 并且直接复用原版卡牌发光的参数（{@link CardFit}）：
 * 卡牌类型对应的剪影尺寸、{@code 倍率 × drawScale × Settings.scale}、{@code card.angle}。
 * 同一个尺寸、同一套变换，结构上不可能错位。
 * <h2>只画边框，不铺满整张卡</h2>
 * {@code CARD_*_BG_SILHOUETTE} 是<b>实心填充</b>的卡牌形状，铺它等于给整张牌染色
 * （上一版就踩了这个坑，小刀整张发白）。现在只按四角画四条细边 + 四角短括，
 * <b>牌面内部一个像素都不画</b>。
 * <h2>什么时候不画</h2>
 * <ul>
 *   <li>不是 {@code Shiv} → 不画</li>
 *   <li>没有「精准」能力 → 不画（规范 §2.9）</li>
 *   <li>玩家关掉了这个特效 → 不画</li>
 *   <li>卡牌被选中 / 悬停弹出 → 不画，避免和原版高亮、拖影打架</li>
 * </ul>
 */
@SpirePatch2(
    cls = "com.megacrit.cardcrawl.cards.AbstractCard",
    method = "render",
    paramtypez = { SpriteBatch.class, boolean.class }
)
public final class AccuracyCardRenderPatch {
    /** 复用的绘制器：内部持有颜色副本，避免每次渲染都 new。 */
    private static final AccuracyLinkDraw PAINTER = new AccuracyLinkDraw();

    private AccuracyCardRenderPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(AbstractCard __instance, Object[] __args) {
        if (__instance == null || __args == null || __args.length == 0) {
            return;
        }
        if (!(__args[0] instanceof SpriteBatch)) {
            return;
        }
        // 只有小刀需要附魔
        if (!(__instance instanceof com.megacrit.cardcrawl.cards.tempCards.Shiv)) {
            return;
        }
        // 没有「精准」就没有附魔（规范 §2.9）
        if (!GreenEffectRegistry.isOn(GreenEffectRegistry.ACCURACY_CARD)) {
            return;
        }
        if (GreenEffectRegistry.stacks(AbstractDungeon.player) <= 0) {
            return;
        }
        // 被选中 / 悬停弹出的那张不画，避免和原版高亮、拖影打架
        if (__instance.isSelected || __instance.hoverTimer > 0.15f) {
            return;
        }

        SpriteBatch sb = (SpriteBatch) __args[0];
        int blendSrc = sb.getBlendSrcFunc();
        int blendDst = sb.getBlendDstFunc();
        try {
            // 全部是光：加色混合
            sb.setBlendFunction(770, 1);
            // 旋转由四角坐标自己承担（见 AccuracyLinkDraw.frame），不需要改变换矩阵
            PAINTER.frame(sb, __instance, 0.90f, gameTime(), AccuracyFx.LINK);
        } finally {
            sb.setBlendFunction(blendSrc, blendDst);
        }
    }

    /**
     * 供边框呼吸用的累计时间。
     * <p>
     * 卡牌对象没有「累计时间」字段，这里用 {@code System.nanoTime} 折算 ——
     * 只驱动一个 0.45/s 的呼吸，精度要求极低。
     */
    private static float gameTime() {
        return (float) (System.nanoTime() / 1_000_000_000.0);
    }
}
