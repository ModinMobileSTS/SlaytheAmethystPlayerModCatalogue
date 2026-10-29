package sts.ryoikitenkai.compat.green;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.AbstractPower;

import java.util.List;

/**
 * 「精准」（Accuracy）的规格、几何、配色与读数。
 * <h2>机制（按字节码核实）</h2>
 * <pre>
 * // AccuracyPower —— 无任何 atXxx 回合回调，三处都在做同一件事：
 * //   updateExistingShivs() / stackPower(int) / onDrawOrDiscard()
 * //   c.baseDamage = (c.upgraded ? 6 : 4) + this.amount;
 * </pre>
 * 效果<b>持续生效</b>，且作用于<b>每一张小刀</b> → 充能做成常驻表现。
 * <h2>配色（本轮修正「偏脏」）</h2>
 * 上一轮为了压暗把颜色调到 {@code (0.62,0.50,0.32)} 这种暗金，
 * 加色混合后混着场景的暖色调，看起来是<b>土褐色</b>。
 * 现在改成：
 * <ul>
 *   <li>靶体仍取卡面的红，但把<b>饱和度拉高、明度略提</b>：
 *       {@code (0.78, 0.20, 0.16)} —— 是「暗红」而不是「黑红」</li>
 *   <li>内环用<b>暖白</b> {@code (0.86, 0.80, 0.74)}，给靶心一圈清晰的结构线</li>
 *   <li>连线用<b>暖白偏金</b> {@code (0.95, 0.88, 0.74)} ——
 *       加色混合后是「一道细光」，而不是褐带</li>
 * </ul>
 * 连线细、靶体饱和，两者在暗场景里都能读出来。
 * <h2>几何（本轮回调大小）</h2>
 * 上一轮缩到 {@code 0.58 × 半宽 / 36px 下限}，在手机上糊成「一个红点」。
 * 现在回到 {@code 0.70 × 半宽 / 44px 下限}：比最初小，但结构看得清。
 * <pre>
 *   靶心 X = centerX − 半宽 × 1.28
 *   靶心 Y = 脚底  + 身高 × 0.60 + bobOffset(time)
 *   半径   = max(44px × scale, 半宽 × 0.70)
 * </pre>
 */
final class AccuracyFx {
    // ---- 靶体：卡面红，但保持饱和 ----
    /** 外环：暗红（饱和，不是黑红）。 */
    static final Color RING = new Color(0.78f, 0.20f, 0.16f, 1.0f);
    /** 内环 / 准星：暖白。 */
    static final Color RING_HI = new Color(0.86f, 0.80f, 0.74f, 1.0f);
    /** 靶心：砖红。 */
    static final Color CORE = new Color(0.86f, 0.32f, 0.22f, 1.0f);
    /** 连线与卡框：暖白偏金 —— 加色后是一道细光。 */
    static final Color LINK = new Color(0.95f, 0.88f, 0.74f, 1.0f);

    /** 靶心横向位置（× 身体半宽）。 */
    private static final float CX_RATIO = -1.28f;
    /** 靶心高度（× 身高）。 */
    private static final float CY_RATIO = 0.60f;
    /** 靶半径（× 身体半宽）。 */
    private static final float R_RATIO = 0.70f;
    /** 靶半径的像素下限。 */
    private static final float R_MIN = 44.0f;

    // ---- 各层半径（以外环半径为单位）----
    static final float RING2_R = 0.955f;
    static final float INNER_R = 0.50f;
    static final float CORE_R = 0.26f;
    static final float CORE_R2 = 0.175f;
    static final float CORE_R3 = 0.095f;
    static final float CROSS_R = 0.20f;

    static final float BOB = 5.0f;
    static final float BOB_PERIOD = 3.6f;
    /** 最多同时画几条连线：再多会满屏拉线。 */
    static final int MAX_LINKS = 3;

    private AccuracyFx() {
    }

    // ---------------- 标靶几何 ----------------

    static float targetX(AbstractCreature owner) {
        return BodyFx.centerX(owner) + BodyFx.halfWidth(owner) * CX_RATIO;
    }

    static float targetY(AbstractCreature owner) {
        return BodyFx.groundY(owner) + BodyFx.height(owner) * CY_RATIO;
    }

    static float bobOffset(float time) {
        return MathUtils.sin(time * MathUtils.PI2 / BOB_PERIOD) * BOB * Settings.scale;
    }

    static float radius(AbstractCreature owner) {
        return Math.max(R_MIN * Settings.scale, BodyFx.halfWidth(owner) * R_RATIO);
    }

    static float ringThick(AbstractCreature owner) {
        return Math.max(GreenDraw.MIN_LINE * 1.4f * Settings.scale, radius(owner) * 0.14f);
    }

    static float innerThick(AbstractCreature owner) {
        return Math.max(GreenDraw.MIN_LINE * 0.8f * Settings.scale, radius(owner) * 0.050f);
    }

    /** 过载程度 0~1：层数很高时整靶轻微泛亮。 */
    static float overdrive(int stacks) {
        return MathUtils.clamp((stacks - 8) / 8.0f, 0.0f, 1.0f);
    }

    // ---------------- 卡牌几何（委托 CardFit） ----------------

    static float frameHalfW(AbstractCard card) {
        return CardFit.frameHalfW(card);
    }

    static float frameHalfH(AbstractCard card) {
        return CardFit.frameHalfH(card);
    }

    static void edgePoint(AbstractCard card, float nx, float ny, float[] out) {
        CardFit.edgePoint(card, nx, ny, out);
    }

    static void nearestEdgeNormal(AbstractCard card, float px, float py, float[] out) {
        CardFit.nearestEdgeNormal(card, px, py, out);
    }

    /** 收集手牌里实际的小刀到调用方给的列表里（不新建列表）。 */
    static int collectShivs(List<AbstractCard> out, int cap) {
        out.clear();
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null || p.hand == null || p.hand.group == null) {
            return 0;
        }
        int n = 0;
        for (int i = 0; i < p.hand.group.size() && n < cap; ++i) {
            AbstractCard c = p.hand.group.get(i);
            if (c instanceof com.megacrit.cardcrawl.cards.tempCards.Shiv) {
                out.add(c);
                ++n;
            }
        }
        return n;
    }

    static int stacks(AbstractCreature owner) {
        if (owner == null || owner.powers == null) {
            return 0;
        }
        AbstractPower p = owner.getPower(GreenEffectRegistry.ACCURACY_ID);
        return p == null ? 0 : Math.max(0, Math.abs(p.amount));
    }
}
