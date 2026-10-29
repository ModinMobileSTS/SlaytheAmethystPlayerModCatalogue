package sts.ryoikitenkai.compat.green;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;

/**
 * 「精准」的<b>标靶</b>画笔。
 * <h2>本轮修正「糊成一个红点」</h2>
 * 上一轮把标靶缩到 {@code 0.58 × 半宽 / 36px 下限}，同时把所有不透明度压到
 * 外环 0.44、内环 0.30、靶心 0.38 —— 三者叠在一起后，在手机上糊成一团暗红。
 * <p>
 * 现在：
 * <ul>
 *   <li>尺寸回调到 {@code 0.70 × 半宽 / 44px 下限}</li>
 *   <li>外环提到 0.72、内环 0.50、靶心实芯 0.55 —— 结构线重新看得清</li>
 *   <li>配色改成<b>饱和的暗红</b>（不是黑红），内环用<b>暖白</b>，
 *       这样三层之间才有明度差，不会糊在一起</li>
 * </ul>
 * 加色混合会把每层亮度相加，所以「层次」靠<b>明度差</b>而不是靠「都压得很低」。
 * <h2>结构（无任何横条）</h2>
 * <pre>
 *   靶心     3 层同心环 + 砖红实芯
 *   准星     上下左右四段极短刻（靶半径的 0.20）
 *   外环     主光带 + 一道淡扫光
 *   内环     细光环（暖白，给清晰的结构线）
 *   双环     外环内侧一道更暗的细环
 * </pre>
 * 已删除：外沿 36 道细齿、12 道径向刻痕、符文环 6 段断续弧。
 */
final class AccuracyDraw {
    /** 外环分段。 */
    private static final int RING_SEG = 80;
    /** 双环（内侧细环）分段。 */
    private static final int RING2_SEG = 76;
    /** 内环分段。 */
    private static final int INNER_SEG = 68;
    /** 靶心同心环分段。 */
    private static final int CORE_SEG = 56;

    private static final float[] RING_X = new float[RING_SEG + 2];
    private static final float[] RING_Y = new float[RING_SEG + 2];
    private static final float[] RING2_X = new float[RING2_SEG + 2];
    private static final float[] RING2_Y = new float[RING2_SEG + 2];
    private static final float[] INNER_X = new float[INNER_SEG + 2];
    private static final float[] INNER_Y = new float[INNER_SEG + 2];
    private static final float[] CORE_X = new float[CORE_SEG + 2];
    private static final float[] CORE_Y = new float[CORE_SEG + 2];

    private AccuracyDraw() {
    }

    /**
     * 整个标靶。
     *
     * @param offX,offY 额外偏移（常驻的缓动滞后 + 轻浮 / 消失上浮）
     * @param grow      出现进度 0~1
     * @param over      过载程度 0~1（层数很高时轻微泛亮）
     * @param time      累计时间（驱动扫光与呼吸）
     * @param pulse     命中脉动 0~1
     */
    static void target(SpriteBatch sb, AbstractCreature owner,
                       float offX, float offY, float alpha, float grow, float over,
                       float time, float pulse,
                       Color ring, Color ringHi, Color core, Color link) {
        if (alpha <= 0.004f || grow <= 0.0f) {
            return;
        }
        float g = MathUtils.clamp(grow, 0.0f, 1.0f);
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        float cx = AccuracyFx.targetX(owner) + offX;
        float cy = AccuracyFx.targetY(owner) + offY;
        float r = AccuracyFx.radius(owner) * g;
        if (r <= 3.0f) {
            return;
        }

        // 呼吸 0.84~1.0；层数很高时轻微泛亮
        float breathe = 0.84f + 0.16f * (0.5f + 0.5f * MathUtils.sin(time * 1.6f));
        float boost = breathe * (1.0f + 0.24f * over + 0.16f * pulse);

        // ================= 靶心：3 层同心环 + 砖红实芯 =================
        float cr = r * AccuracyFx.CORE_R;
        GreenDraw.glow(sb, cx, cy, cr * 2.8f, cr * 2.8f, 0.0f, core, a * 0.16f * boost);
        coreRing(sb, cx, cy, r * (AccuracyFx.CORE_R + AccuracyFx.CORE_R2) * 0.5f,
                Math.max(GreenDraw.MIN_LINE * 0.9f, r * 0.038f), core, a * 0.44f * boost);
        coreRing(sb, cx, cy, r * (AccuracyFx.CORE_R2 + AccuracyFx.CORE_R3) * 0.5f,
                Math.max(GreenDraw.MIN_LINE * 0.9f, r * 0.032f), core, a * 0.52f * boost);
        GreenDraw.glow(sb, cx, cy, r * AccuracyFx.CORE_R3 * 2.0f,
                r * AccuracyFx.CORE_R3 * 2.0f, 0.0f, core, a * 0.55f * boost);
        GreenDraw.glow(sb, cx, cy, r * AccuracyFx.CORE_R3 * 1.0f,
                r * AccuracyFx.CORE_R3 * 1.0f, 0.0f, ringHi, a * 0.46f * boost);

        // ================= 十字准星：靶心外四段极短刻 =================
        float crossLen = r * AccuracyFx.CROSS_R;
        float crossTh = Math.max(GreenDraw.MIN_LINE * 0.8f, r * 0.026f);
        GreenDraw.quad(sb, cx, cy + crossLen * 0.45f, cx, cy + crossLen,
                crossTh, ringHi, a * 0.50f * boost);
        GreenDraw.quad(sb, cx, cy - crossLen * 0.45f, cx, cy - crossLen,
                crossTh, ringHi, a * 0.50f * boost);
        GreenDraw.quad(sb, cx - crossLen, cy, cx - crossLen * 0.45f, cy,
                crossTh, ringHi, a * 0.50f * boost);
        GreenDraw.quad(sb, cx + crossLen * 0.45f, cy, cx + crossLen, cy,
                crossTh, ringHi, a * 0.50f * boost);

        // ================= 双环：外环内侧一道更暗的细环 =================
        ringPath(RING2_X, RING2_Y, cx, cy, r * AccuracyFx.RING2_R, RING2_SEG);
        GreenDraw.stroke(sb, RING2_X, RING2_Y, RING2_SEG + 1,
                Math.max(GreenDraw.MIN_LINE * 0.9f, r * 0.026f), ring, a * 0.30f * boost);

        // ================= 外环：主光带 =================
        ringPath(RING_X, RING_Y, cx, cy, r, RING_SEG);
        GreenDraw.stroke(sb, RING_X, RING_Y, RING_SEG + 1,
                AccuracyFx.ringThick(owner), ring, a * 0.72f * boost);

        // ================= 内环：细光环（暖白，清晰的结构线）=================
        ringPath(INNER_X, INNER_Y, cx, cy, r * AccuracyFx.INNER_R, INNER_SEG);
        GreenDraw.stroke(sb, INNER_X, INNER_Y, INNER_SEG + 1,
                AccuracyFx.innerThick(owner), ringHi, a * 0.50f * boost);

        // ================= 扫光：沿外环匀速走一圈（光点，不是横条）=================
        float sweep = (time * 0.55f) % MathUtils.PI2;
        float sx = cx + MathUtils.cos(sweep) * r;
        float sy = cy + MathUtils.sin(sweep) * r * 0.98f;
        float ss = Math.max(GreenDraw.MIN_LINE * 1.4f, r * 0.16f);
        GreenDraw.glow(sb, sx, sy, ss * 2.2f, ss * 2.2f, 0.0f, ring, a * 0.26f * boost);
        GreenDraw.glow(sb, sx, sy, ss * 1.0f, ss * 1.0f, 0.0f, link, a * 0.34f * boost);
    }

    /** 生成一条闭合圆环的路径（首尾重合，便于 {@link GreenDraw#stroke} 直接拼）。 */
    private static void ringPath(float[] xs, float[] ys,
                                 float cx, float cy, float r, int seg) {
        for (int i = 0; i <= seg; ++i) {
            float ang = MathUtils.PI2 * i / seg;
            xs[i] = cx + MathUtils.cos(ang) * r;
            ys[i] = cy + MathUtils.sin(ang) * r;
        }
    }

    /** 靶心用的一圈半径（走独立数组，不与内环共用）。 */
    private static void coreRing(SpriteBatch sb, float cx, float cy, float r,
                                 float thick, Color color, float alpha) {
        ringPath(CORE_X, CORE_Y, cx, cy, r, CORE_SEG);
        GreenDraw.stroke(sb, CORE_X, CORE_Y, CORE_SEG + 1, thick, color, alpha);
    }
}
