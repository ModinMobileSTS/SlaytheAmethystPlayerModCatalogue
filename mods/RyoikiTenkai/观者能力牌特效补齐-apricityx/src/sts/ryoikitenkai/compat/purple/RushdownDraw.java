package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

/**
 * 「猛虎下山」的抓痕笔触：<b>只画抓痕，不画细线</b>。
 * <p>
 * <h2>本轮修正：禁止细线条</h2>
 * 上一版抓痕的白热内芯用 {@code w × 0.40}、分叉用 {@code bw × 0.55}，
 * 在手机上就是一根根 1~2px 的细线 —— 又碎又廉价。
 * <p>
 * 现在全部改为<b>有体积的块面</b>：
 * <ul>
 *   <li>主干：用 {@code Draw.glow} 拉长成<b>有宽度的锥形光块</b>，
 *       由「宽外晕 + 主体 + 亮芯」三层堆出来，不是一根线</li>
 *   <li>分叉：同样用锥形光块，只是更短一档（但仍有下限）</li>
 *   <li>白热内芯：改为一层<b>更窄的光带</b>（不是一条线），
 *       宽度至少是主干的 45%</li>
 * </ul>
 * 视觉上仍然是「一道裂开的痕」，但每一段都有厚度，不会退化成发丝。
 * <h2>一道「真实抓痕」的四个特征（保留）</h2>
 * <ol>
 *   <li><b>宽度包络</b>：起点细 → 前段最粗 → 末端收成尖（见 {@link #taper}）</li>
 *   <li><b>轻微弯曲</b>：两组不同频率正弦叠加，幅度约长度 3%</li>
 *   <li><b>分叉裂纹</b>：中后段甩出 2 条短枝 —— 这是「撕裂」与「划线」的区别</li>
 *   <li><b>三层叠画</b>：宽外晕 → 主体 → 亮芯，三层都有厚度</li>
 * </ol>
 * <h2>贴图选择</h2>
 * 全部用 {@code Draw.glow}（柔光块）：抓痕是「有厚度的肉裂」，
 * 用等宽实线反而像激光。不使用星芒贴图。
 */
final class RushdownDraw {
    /** 主干分段数。7 段足以画出弧度与收尖。 */
    private static final int SEG = 7;
    /** 亮芯相对主干的宽度比例。低于它就会变成细线。 */
    private static final float CORE_MIN_RATIO = 0.45f;
    /** 任何一段的最小宽度（像素）。这是「禁止细线」的硬底线。 */
    private static final float MIN_SEG_W = 4.0f;

    private RushdownDraw() {
    }

    /**
     * 宽度包络：起点细、前段最粗、末端收成尖。
     * <p>
     * 峰值放在 0.28 处而不是正中 —— 真实的爪击是「落爪即最宽，然后一路收尖」，
     * 峰值在中段会读成「两头尖的梭形」，像叶子不像爪。
     */
    static float taper(float t) {
        if (t < 0.28f) {
            return MathUtils.lerp(0.34f, 1.0f, t / 0.28f);
        }
        return MathUtils.lerp(1.0f, 0.10f, (t - 0.28f) / 0.72f);
    }

    /** 路径上的弯曲偏移（以线段为基准的法向位移）。 */
    private static float bowAt(float t, float len, float seed) {
        return (MathUtils.sin(t * 3.4f + seed) * 0.65f
                + MathUtils.sin(t * 7.9f + seed * 1.7f) * 0.35f)
                * len * 0.030f * MathUtils.sin(t * MathUtils.PI * 0.5f);
    }

    /**
     * 在一段路径上铺一个「锥形光块」。
     * <p>
     * 这是「不用线条」的核心：每段用一个拉伸的柔光块，
     * 宽度由 {@link #taper} 决定，所以整体是一条有厚度的锥带。
     */
    private static void slab(SpriteBatch sb, float x0, float y0, float x1, float y1,
                             float w0, float w1, Color color, float alpha) {
        if (alpha <= 0.004f) {
            return;
        }
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len <= 0.6f) {
            return;
        }
        float w = Math.max(MIN_SEG_W, (w0 + w1) * 0.5f);
        float rot = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        Draw.glow(sb, (x0 + x1) * 0.5f, (y0 + y1) * 0.5f,
                len * 1.12f, w, rot, color, alpha);
    }

    /**
     * 画一道抓痕。
     *
     * @param width    最粗处的宽度（像素，已含 scale）
     * @param progress 已经「裂」到哪，0~1，用于逐段展开
     * @param seed     形状种子：决定弯曲方向与分叉角度，同一 seed 每帧稳定
     * @param flow     沿痕流动的高光位置 0~1；传负数关闭
     * @param branches 是否画分叉裂纹
     */
    static void claw(SpriteBatch sb, float x0, float y0, float x1, float y1,
                     float width, float sc, Color color, Color core,
                     float alpha, float progress, float seed, float flow,
                     boolean branches) {
        if (alpha <= 0.004f || progress <= 0.0f || width <= 0.5f) {
            return;
        }
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len <= 2.0f) {
            return;
        }
        float nx = -dy / len;
        float ny = dx / len;
        float shown = MathUtils.clamp(progress, 0.0f, 1.0f) * SEG;

        // ---- 主干：逐段铺三层锥形光块 ----
        float prevX = x0;
        float prevY = y0;
        float prevEnv = taper(0.0f);
        for (int i = 1; i <= SEG; ++i) {
            float t = i / (float) SEG;
            float bow = bowAt(t, len, seed);
            float x = x0 + dx * t + nx * bow;
            float y = y0 + dy * t + ny * bow;
            if ((i - 1) < shown) {
                float env = taper(t);
                float wAvg = Math.max(MIN_SEG_W, width * env);
                float wPrev = Math.max(MIN_SEG_W, width * prevEnv);

                // 1. 外晕：宽而淡，让抓痕从背景里立起来
                slab(sb, prevX, prevY, x, y, wPrev * 2.1f, wAvg * 2.1f,
                        color, alpha * 0.16f * env);
                // 2. 主体：有厚度的锥带（不是线）
                slab(sb, prevX, prevY, x, y, wPrev, wAvg,
                        color, alpha * 0.92f);
                // 3. 亮芯：更窄的一层光带，宽度不低于主干的 45%
                float cPrev = Math.max(MIN_SEG_W * 0.7f, wPrev * CORE_MIN_RATIO);
                float cAvg = Math.max(MIN_SEG_W * 0.7f, wAvg * CORE_MIN_RATIO);
                slab(sb, prevX, prevY, x, y, cPrev, cAvg,
                        core, alpha * 0.78f * env);

                // 4. 流动高光：沿痕走的一枚光斑
                if (flow >= 0.0f) {
                    float d = Math.abs(t - flow);
                    float hm = Math.max(0.0f, 1.0f - d / 0.22f);
                    if (hm > 0.01f) {
                        Draw.glow(sb, x, y, wAvg * 3.4f * hm, wAvg * 3.4f * hm, 0.0f,
                                core, alpha * 0.55f * hm);
                    }
                }
            }
            prevX = x;
            prevY = y;
            prevEnv = taper(t);
        }

        // ---- 分叉裂纹：这是「撕裂」而不是「划线」的关键 ----
        if (branches && progress > 0.32f) {
            float baseAng = MathUtils.atan2(dy, dx);
            for (int b = 0; b < 2; ++b) {
                float tb = 0.32f + 0.30f * b
                        + MathUtils.sin(seed * 1.7f + b * 2.3f) * 0.05f;
                if (tb > progress) {
                    continue;
                }
                float bowB = bowAt(tb, len, seed);
                float bx = x0 + dx * tb + nx * bowB;
                float by = y0 + dy * tb + ny * bowB;
                // 分叉方向：切向 ± 一个较大的夹角，读作「裂开」
                float sign = MathUtils.sin(seed * 3.1f + b * 4.7f) > 0.0f ? 1.0f : -1.0f;
                float ang = baseAng + sign * (0.75f + 0.50f
                        * Math.abs(MathUtils.sin(seed * 2.1f + b * 3.3f)));
                float bl = len * (0.12f + 0.10f
                        * (0.5f + 0.5f * MathUtils.sin(seed * 2.7f + b * 5.3f)));
                float ex = bx + MathUtils.cos(ang) * bl;
                float ey = by + MathUtils.sin(ang) * bl;
                // 分叉也有厚度：以主干当时宽度的 62% 起，收到 42%
                float bw = Math.max(MIN_SEG_W * 0.8f, width * taper(tb) * 0.62f);
                float bwEnd = Math.max(MIN_SEG_W * 0.5f, bw * 0.42f);
                slab(sb, bx, by, ex, ey, bw, bwEnd, color, alpha * 0.86f);
                slab(sb, bx, by, ex, ey,
                        bw * CORE_MIN_RATIO, bwEnd * CORE_MIN_RATIO,
                        core, alpha * 0.52f);
            }
        }

        // ---- 末端：一枚亮斑，读作「爪尖落点」 ----
        if (progress > 0.92f) {
            float q = MathUtils.clamp((progress - 0.92f) / 0.08f, 0.0f, 1.0f);
            float tipBow = bowAt(1.0f, len, seed);
            float tx = x1 + nx * tipBow;
            float ty = y1 + ny * tipBow;
            float tr = Math.max(MIN_SEG_W, width * 0.55f);
            Draw.glow(sb, tx, ty, tr * 3.0f, tr * 3.0f, 0.0f, color, alpha * 0.55f * q);
            Draw.glow(sb, tx, ty, tr * 1.4f, tr * 1.4f, 0.0f, core, alpha * 0.82f * q);
        }
    }

    /**
     * 从一点向外裂开的若干短裂纹（伤口中心用）。
     * <p>
     * 每条都是从中心射出的<b>锥形光块</b>，宽度随距离收尖，
     * 所以看起来像「这一击把身体撕开了」，而不是炸开的星星或放射细线。
     */
    static void cracks(SpriteBatch sb, float px, float py, float reach, float sc,
                       Color color, Color core, float alpha, float progress,
                       int count, float seed) {
        if (alpha <= 0.004f || progress <= 0.0f || count <= 0) {
            return;
        }
        float w = Math.max(MIN_SEG_W * 1.3f, reach * 0.16f);
        for (int i = 0; i < count; ++i) {
            float ang = seed + i * MathUtils.PI2 / count
                    + MathUtils.sin(seed * 1.9f + i * 3.7f) * 0.30f;
            float bl = reach * (0.48f + 0.52f
                    * (0.5f + 0.5f * MathUtils.sin(seed + i * 5.3f)));
            claw(sb, px, py,
                    px + MathUtils.cos(ang) * bl, py + MathUtils.sin(ang) * bl,
                    w, sc, color, core, alpha, progress, seed + i * 1.7f, -1.0f, false);
        }
    }

    /**
     * 末端飞溅：从伤口甩出的<b>圆形光点</b>，每个带自己的拖尾光斑。
     * <p>
     * 位置由 {@code seed} 正弦散列决定，同一特效每帧稳定。
     */
    static void sparks(SpriteBatch sb, float px, float py, float reach, float sc,
                       Color color, Color core, float alpha, float progress,
                       int count, float seed) {
        if (alpha <= 0.004f || count <= 0) {
            return;
        }
        float q = MathUtils.clamp(progress, 0.0f, 1.0f);
        for (int i = 0; i < count; ++i) {
            float ang = (i / (float) count) * MathUtils.PI2
                    + MathUtils.sin(seed + i * 3.7f) * 0.55f;
            float dist = reach * (0.12f + 0.62f
                    * (0.5f + 0.5f * MathUtils.sin(seed * 2.1f + i * 5.3f)));
            float d = dist * q;
            float x = px + MathUtils.cos(ang) * d;
            float y = py + MathUtils.sin(ang) * d * 0.72f;
            float size = Math.max(MIN_SEG_W * 0.6f, reach * 0.055f
                    * (0.7f + 0.6f * (0.5f + 0.5f * MathUtils.sin(i * 2.1f))));
            float a = alpha * (1.0f - 0.60f * q);
            // 后方一枚更小的光点当拖尾（仍然是圆点，不是线段）
            Draw.glow(sb, px + MathUtils.cos(ang) * d * 0.62f,
                    py + MathUtils.sin(ang) * d * 0.62f * 0.72f,
                    size * 1.9f, size * 1.9f, 0.0f, color, a * 0.32f);
            Draw.glow(sb, x, y, size * 2.8f, size * 2.8f, 0.0f, color, a * 0.72f);
            Draw.glow(sb, x, y, size * 1.2f, size * 1.2f, 0.0f, core, a * 0.55f);
        }
    }
}
