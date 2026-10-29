package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;

/**
 * 「虔信」的画笔：<b>刻度光轮</b>、<b>进度弧</b>、<b>诵经光点</b>。
 * <p>
 * <h2>本轮返工：为什么原来看不见（用户反馈「特效不够明显」）</h2>
 * 上一版的刻度是「横向扁点」：
 * <pre>
 *   tickW = rx × 0.13 ≈ 12px
 *   tickH = ry × 0.055 ≈ 2.1px  → 被钳到 3px
 * </pre>
 * 也就是每个刻度只有 <b>12×3 像素</b>，再乘上 {@code alpha × 0.26} 的光晕，
 * 在手机上完全看不出是「一圈格子」。而且横放的扁点在椭圆轨道上是<b>错的方向</b>——
 * 刻度应该<b>沿径向</b>排布（像钟表 / 仪表盘的刻度线），横点会被读成噪声。
 * 现在改了三件事：
 * <ol>
 *   <li><b>刻度改成径向粗棒</b>：沿椭圆的外法线方向，长度/厚度都是<b>带缩放的固定像素</b>
 *       （{@code ≥ 13px × scale} / {@code ≥ 6px × scale}），所以上下左右一样长短，
 *       不会在椭圆顶端缩成一点。</li>
 *   <li><b>新增进度弧</b>：从 0 号刻度起，沿轨道铺一条<b>粗亮的弧</b>，
 *       弧长 = 已积真言的角度。这是整圈里最亮的一条，进度一眼可读。</li>
 *   <li><b>未点亮的刻度不再是「一片黑」</b>：{@link DevotionFx#DIM} 提亮，
 *       并用统一下限画出凹槽，让「还有几格」始终看得见。</li>
 * </ol>
 * <h2>为什么下限必须乘 {@code Settings.scale}</h2>
 * 本补丁其它所有特效的尺寸都写了 {@code × Settings.scale}
 * （见 {@code FoundationAuraEffect} / {@code RushdogClawEffect} / {@code MotifAuraEffect}）。
 * 原因是 {@code Settings.scale} 等于「实际渲染分辨率 ÷ 1920 基准」，
 * 手机分辨率远高于 1920 时它会大于 1。<b>裸写 13px 在 2560 宽的屏上只有
 * 基准下 9.75px 的相对大小</b>，于是「和别的牌写得一样」却看起来小一圈。
 * 所以这里的固定下限一律 {@code × Settings.scale}。
 * <h2>部件与它们各自回答的问题</h2>
 * <table border="1">
 *   <caption>一个部件只回答一个问题</caption>
 *   <tr><td>部件</td><td>回答</td><td>画法</td></tr>
 *   <tr><td>{@link #track}</td><td>「这一圈是什么」</td><td>粗弧轨道 + 内外光晕</td></tr>
 *   <tr><td>{@link #gauge}</td><td>「积了多少」</td><td>沿轨道的粗亮进度弧 + 弧头光点</td></tr>
 *   <tr><td>{@link #ticks}</td><td>「一共十格，亮到第几格」</td><td>10 根径向粗棒</td></tr>
 *   <tr><td>{@link #innerRing}</td><td>「正在诵经」</td><td>内圈六段反向旋转的粗弧</td></tr>
 *   <tr><td>{@link #chant}</td><td>「诵到哪一句」</td><td>沿轨道跑的光点 + 拖尾</td></tr>
 *   <tr><td>{@link #ripple}</td><td>「刚诵完一句」</td><td>向外扩的粗环（一次性）</td></tr>
 * </table>
 * <h2>禁止细线条</h2>
 * 所有厚度都显式 {@code ≥ 6px × scale}（{@link Draw#MIN_LINE} = 4.5px 是底层兜底，
 * 这里再抬一档）。圆的元素一律用 {@link Draw#arc} 的粗弧，不用放射线、不用星芒。
 */
final class DevotionDraw {
    /** 轨道最小厚度（基准像素）。比 Draw 的 4.5px 兜底再抬一档。 */
    private static final float TRACK_THICK = 6.0f;
    /** 轨道的分段数：够圆即可。 */
    private static final int TRACK_SEG = 56;
    /** 进度弧的分段密度（每 7° 一段）。 */
    private static final float GAUGE_DEG_PER_SEG = 7.0f;
    /** 刻度棒最小厚度 / 最小长度（基准像素）。恒定，与半径解耦。 */
    private static final float TICK_MIN_THICK = 6.0f;
    private static final float TICK_MIN_LEN = 14.0f;
    /** 刻度棒长度相对半径的倍数（半径大时跟着长大，但有下限）。 */
    private static final float TICK_LEN_RATIO = 0.20f;
    /** 内圈段数。 */
    private static final int INNER_SEG = 6;

    private DevotionDraw() {
    }

    /** 缩放后的基准像素：所有「固定像素」下限都走它。 */
    private static float px(float base) {
        return base * Settings.scale;
    }

    /** 刻度 i 在圆周上的角度（度）。从正上方开始顺时针。 */
    private static float tickDeg(int i, float spinDeg) {
        return -90.0f + spinDeg + 360.0f * i / DevotionFx.TICKS;
    }

    /**
     * 极淡的闭合轨道：只提供「这一圈在哪」的位置感。
     * <p>
     * 本轮加粗加亮：它现在是真的能看见的一圈轮廓，而不是一层雾。
     */
    static void track(SpriteBatch sb, float cx, float cy, float rx, float ry,
                      float alpha, Color color) {
        if (alpha <= 0.004f || rx <= 1.0f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        // 内外的体积光：让轨道有「厚度」而不是一条边
        Draw.glow(sb, cx, cy, rx * 2.60f, ry * 2.60f, 0.0f, color, a * 0.16f);
        Draw.glow(sb, cx, cy, rx * 1.90f, ry * 1.90f, 0.0f, color, a * 0.12f);
        // 轨道本体：一圈粗弧
        Draw.arc(sb, cx, cy, rx, ry, 0.0f, 360.0f,
                Math.max(px(TRACK_THICK), rx * 0.055f), color, a * 0.46f, TRACK_SEG);
        // 内侧再压一条稍亮、略窄的弧，夹出「金属环」的边
        Draw.arc(sb, cx, cy, rx * 0.965f, ry * 0.965f, 0.0f, 360.0f,
                Math.max(px(4.5f), rx * 0.022f), color, a * 0.26f, TRACK_SEG);
    }

    /**
     * 进度弧：从 0 号刻度起沿轨道铺一条粗亮的弧，弧长 = 已积真言的角度。
     * <p>
     * 这是整圈里<b>最亮、最粗</b>的一条 —— 读进度不用数格子，看弧有多长就行。
     *
     * @param filled  已积的真言点数 0~10
     * @param spinDeg 整圈的旋转偏移（度），必须与 {@link #ticks} 用同一个值
     */
    static void gauge(SpriteBatch sb, float cx, float cy, float rx, float ry,
                      int filled, float spinDeg, float alpha,
                      Color color, Color coreColor) {
        if (alpha <= 0.004f || rx <= 1.0f || filled <= 0) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        int total = DevotionFx.TICKS;
        int n = Math.min(filled, total);
        float sweep = 360.0f * n / total;
        int seg = Math.max(6, (int) (sweep / GAUGE_DEG_PER_SEG));
        float from = -90.0f + spinDeg;

        // 外层：极宽的柔光垫，让进度弧「发亮」而不是「一条线」
        Draw.arc(sb, cx, cy, rx, ry, from, sweep,
                Math.max(px(TRACK_THICK * 2.6f), rx * 0.16f), color, a * 0.20f, seg);
        // 主弧：粗、亮
        Draw.arc(sb, cx, cy, rx, ry, from, sweep,
                Math.max(px(TRACK_THICK), rx * 0.075f), color, a * 0.92f, seg);
        // 亮芯：窄一档、更白
        Draw.arc(sb, cx, cy, rx, ry, from, sweep,
                Math.max(px(4.5f), rx * 0.028f), coreColor, a * 0.72f, seg);

        // 弧头一颗光点：读作「刚积到这里」
        if (n < total) {
            float rad = (from + sweep) * MathUtils.degreesToRadians;
            float hx = cx + MathUtils.cos(rad) * rx;
            float hy = cy + MathUtils.sin(rad) * ry;
            float r = Math.max(px(9.0f), rx * 0.11f);
            Draw.glow(sb, hx, hy, r * 3.4f, r * 3.4f, 0.0f, color, a * 0.34f);
            Draw.glow(sb, hx, hy, r * 1.5f, r * 1.5f, 0.0f, color, a * 0.85f);
            Draw.glow(sb, hx, hy, r * 0.7f, r * 0.7f, 0.0f, coreColor, a * 0.95f);
        }
    }

    /**
     * 10 根径向刻度：已积的亮而长，未积的暗而短。
     * <p>
     * <b>方向</b>用椭圆在该点的外法线，<b>长度</b>用带缩放的固定像素下限，
     * 所以椭圆顶端的那根不会缩成一个点（上一版就是在这里失去可读性的）。
     *
     * @param lit      已点亮的根数
     * @param visible  已显形的根数（出现时逐个冒出）；{@code <= 0} 表示按 10 根算
     * @param pulseT   脉动相位（秒）
     * @param hot      0~1，越接近满值越亮
     */
    static void ticks(SpriteBatch sb, float cx, float cy, float rx, float ry,
                      int lit, int visible, float spinDeg, float alpha,
                      Color litColor, Color coreColor, Color dimColor,
                      float pulseT, float hot) {
        if (alpha <= 0.004f || rx <= 1.0f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        int total = DevotionFx.TICKS;
        int shown = visible <= 0 ? total : Math.min(visible, total);
        int n = MathUtils.clamp(lit, 0, shown);
        float h = MathUtils.clamp(hot, 0.0f, 1.0f);

        for (int i = 0; i < shown; ++i) {
            float rad = tickDeg(i, spinDeg) * MathUtils.degreesToRadians;
            float cs = MathUtils.cos(rad);
            float sn = MathUtils.sin(rad);
            float px0 = cx + cs * rx;
            float py0 = cy + sn * ry;

            // 椭圆外法线 ∝ (cos/rx, sin/ry)；归一化后取「固定像素」长度，
            // 于是上下左右的刻度棒一样长。
            float nx = cs / rx;
            float ny = sn / ry;
            float nl = (float) Math.sqrt(nx * nx + ny * ny);
            if (nl < 1.0e-5f) {
                continue;
            }
            nx /= nl;
            ny /= nl;

            boolean isLit = i < n;
            float len = Math.max(px(TICK_MIN_LEN), rx * TICK_LEN_RATIO);
            float thick = Math.max(px(TICK_MIN_THICK), rx * 0.070f);

            if (isLit) {
                // 已点亮：呼吸 + 越靠后越亮（读作「一路积过来的」）
                float rank = (i + 1) / (float) total;
                float breath = 0.86f + 0.14f * MathUtils.sin(pulseT * 2.8f + i * 0.9f);
                float bright = breath * (0.74f + 0.26f * rank) * (1.0f + 0.30f * h);
                // 最后一根额外放大：读作「刚点上去的」
                float isLast = (i == n - 1) ? 1.0f : 0.0f;
                float grow = 1.0f + 0.50f * isLast
                        * (0.6f + 0.4f * MathUtils.sin(pulseT * 7.0f));
                float l = len * (1.0f + 0.30f * rank) * grow;
                float t = thick * grow;

                // 根部的团光：让刻度「有分量」
                Draw.glow(sb, px0, py0, t * 3.6f, t * 3.6f, 0.0f, litColor,
                        a * 0.30f * bright);
                // 径向粗棒：向外伸
                radial(sb, px0, py0, nx, ny, l * 0.55f, l, t, a * 0.92f * bright, litColor);
                // 亮芯
                radial(sb, px0, py0, nx, ny, l * 0.60f, l * 0.92f,
                        Math.max(px(4.5f), t * 0.42f), a * 0.60f * bright, coreColor);
            } else {
                // 未点亮：短一截、暗一档，但仍要有体积，才看得出「还有几格」
                radial(sb, px0, py0, nx, ny, len * 0.30f, len * 0.62f,
                        Math.max(px(4.5f), thick * 0.72f), a * 0.40f, dimColor);
            }
        }
    }

    /**
     * 内圈：六段粗弧，<b>反向</b>慢转。读作「诵经这件事一直在转」。
     * <p>
     * 与外侧刻度环的转速方向相反，两个方向叠在一起才有「旋转」的体积感；
     * 只有一圈单向转的话，看起来更像静止的圆。
     */
    static void innerRing(SpriteBatch sb, float cx, float cy, float rx, float ry,
                          float spinDeg, float alpha, Color color) {
        if (alpha <= 0.004f || rx <= 1.0f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        float span = 360.0f / INNER_SEG;
        float irx = rx * 0.60f;
        float iry = ry * 0.60f;
        for (int i = 0; i < INNER_SEG; ++i) {
            float from = -90.0f + spinDeg + i * span;
            Draw.arc(sb, cx, cy, irx, iry, from, span * 0.52f,
                    Math.max(px(5.0f), rx * 0.032f), color, a * 0.42f, 10);
        }
        // 内圈整体的一层薄光，避免六段弧看起来像浮着的碎片
        Draw.glow(sb, cx, cy, irx * 2.0f, iry * 2.0f, 0.0f, color, a * 0.10f);
    }

    /**
     * 沿圆周绕行的诵经光：读作「正在诵读、不断累积」。
     *
     * @param phase    累计相位 0~1
     * @param count    绕行的光点数
     * @param tailFade 拖尾强度 0~1
     */
    static void chant(SpriteBatch sb, float cx, float cy, float rx, float ry,
                      float phase, int count, float alpha, Color color,
                      Color coreColor, float tailFade) {
        if (alpha <= 0.004f || count <= 0 || rx <= 1.0f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        float tail = MathUtils.clamp(tailFade, 0.0f, 1.0f);
        float size = Math.max(px(8.0f), rx * 0.105f);

        for (int k = 0; k < count; ++k) {
            float f = (phase + k / (float) count) % 1.0f;
            float rad = (-90.0f + f * 360.0f) * MathUtils.degreesToRadians;
            float pxc = cx + MathUtils.cos(rad) * rx;
            float pyc = cy + MathUtils.sin(rad) * ry;

            Draw.glow(sb, pxc, pyc, size * 5.2f, size * 5.2f, 0.0f, color, a * 0.34f);
            Draw.glow(sb, pxc, pyc, size * 2.2f, size * 2.2f, 0.0f, color, a * 0.78f);
            Draw.glow(sb, pxc, pyc, size * 0.9f, size * 0.9f, 0.0f, coreColor, a * 0.92f);

            // 拖尾：沿逆着运动方向回溯几点，读作「刚诵到这一句」
            if (tail > 0.01f) {
                for (int j = 1; j <= 4; ++j) {
                    float tf = (f - j * 0.026f + 1.0f) % 1.0f;
                    float trad = (-90.0f + tf * 360.0f) * MathUtils.degreesToRadians;
                    float tx = cx + MathUtils.cos(trad) * rx;
                    float ty = cy + MathUtils.sin(trad) * ry;
                    float ts = size * (1.0f - j * 0.20f);
                    float ta = a * tail * (0.30f - j * 0.06f);
                    if (ta <= 0.004f) {
                        continue;
                    }
                    Draw.glow(sb, tx, ty, ts * 2.6f, ts * 2.6f, 0.0f, color, ta);
                }
            }
        }
    }

    /**
     * 一次「诵了一句」的涟漪：从刻度环向外扩一圈粗环。
     * <p>
     * 每回合开始都会发生，所以<b>只扩一圈、只活 0.62s</b>，不吵。
     * 它的价值在于让「这一回合真的加了真言」有一个明确的时间点。
     *
     * @param q 0~1，扩张进度
     */
    static void ripple(SpriteBatch sb, float cx, float cy, float rx, float ry,
                       float q, float alpha, Color color, Color coreColor) {
        if (alpha <= 0.004f || q <= 0.0f || q >= 1.0f || rx <= 1.0f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        // 末端斜率为 0：先快后慢地扩开
        float grow = 1.0f - (1.0f - q) * (1.0f - q);
        float scale = 1.0f + 0.95f * grow;
        float fade = FastingDraw.cosineOut(q);
        float arx = rx * scale;
        float ary = ry * scale;
        float thick = Math.max(px(7.0f), rx * 0.085f) * (1.0f - 0.45f * q);

        Draw.arc(sb, cx, cy, arx, ary, 0.0f, 360.0f, thick, color,
                a * 0.85f * fade, 40);
        Draw.arc(sb, cx, cy, arx * 0.97f, ary * 0.97f, 0.0f, 360.0f,
                Math.max(px(4.5f), thick * 0.42f), coreColor, a * 0.55f * fade, 36);
        Draw.glow(sb, cx, cy, arx * 2.1f, ary * 2.1f, 0.0f, color, a * 0.18f * fade);
    }

    /**
     * 满值那一瞬的「刻度齐亮」：10 根刻度同时炸亮。
     *
     * @param burst 0~1，爆发进度
     */
    static void tickBurst(SpriteBatch sb, float cx, float cy, float rx, float ry,
                          float burst, float alpha, Color color, Color coreColor) {
        if (alpha <= 0.004f || burst <= 0.0f || burst >= 1.0f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        // 亮度：起势极快、末端余弦化掉（不硬切）
        float bright = burst < 0.14f ? burst / 0.14f
                : FastingDraw.cosineOut((burst - 0.14f) / 0.86f);
        // 刻度向外弹一点再收回
        float pop = 1.0f + 0.32f * MathUtils.sin(burst * MathUtils.PI);
        int total = DevotionFx.TICKS;
        float thick = Math.max(px(9.0f), rx * 0.11f);
        float len = Math.max(px(18.0f), rx * 0.28f);

        for (int i = 0; i < total; ++i) {
            float rad = tickDeg(i, 0.0f) * MathUtils.degreesToRadians;
            float cs = MathUtils.cos(rad);
            float sn = MathUtils.sin(rad);
            float px0 = cx + cs * rx * pop;
            float py0 = cy + sn * ry * pop;
            float nx = cs / rx;
            float ny = sn / ry;
            float nl = (float) Math.sqrt(nx * nx + ny * ny);
            if (nl < 1.0e-5f) {
                continue;
            }
            nx /= nl;
            ny /= nl;

            Draw.glow(sb, px0, py0, thick * 4.4f, thick * 4.4f, 0.0f, color,
                    a * 0.40f * bright);
            radial(sb, px0, py0, nx, ny, len * 0.55f, len, thick, a * 0.95f * bright, color);
            radial(sb, px0, py0, nx, ny, len * 0.60f, len * 0.92f,
                    Math.max(px(5.0f), thick * 0.48f), a * 0.85f * bright, coreColor);
        }
    }

    /**
     * 向心收束：一圈从外围缩向角色，读作「献上、收进体内」。
     *
     * @param q 0~1，收束进度；1 = 已收到中心
     */
    static void collapse(SpriteBatch sb, float cx, float cy, float rx, float ry,
                         float q, float alpha, Color color, Color coreColor) {
        if (alpha <= 0.004f || q <= 0.0f || q >= 1.0f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        // 半径从 1.0 缩到 0.10：向内收
        float shrink = MathUtils.lerp(1.0f, 0.10f, FastingDraw.cosineOut(q));
        // 亮度：起势亮、末端余弦化掉
        float bright = FastingDraw.cosineOut(q);
        float arx = rx * shrink;
        float ary = ry * shrink;

        Draw.arc(sb, cx, cy, arx, ary, 0.0f, 360.0f,
                Math.max(px(9.0f), arx * 0.16f), color, a * 0.92f * bright, 40);
        Draw.arc(sb, cx, cy, arx * 0.88f, ary * 0.88f, 0.0f, 360.0f,
                Math.max(px(5.0f), arx * 0.075f), coreColor, a * 0.75f * bright, 34);
        // 一路留下的光晕残迹：越靠内越亮
        Draw.glow(sb, cx, cy, arx * 3.2f, ary * 3.2f, 0.0f, color,
                a * 0.26f * bright);
        Draw.glow(sb, cx, cy, arx * 1.4f, ary * 1.4f, 0.0f, color,
                a * 0.34f * bright);
    }

    /**
     * 沿外法线画一根粗棒：从 {@code (px,py)+(nx,ny)×from} 到 {@code ×to}。
     * <p>
     * 单独抽出来是为了让「长度」与「厚度」可以是不同的下限 ——
     * {@link Draw#line} 只兜厚度，长度由这里控制。
     */
    private static void radial(SpriteBatch sb, float px, float py,
                               float nx, float ny, float from, float to,
                               float thick, float alpha, Color color) {
        if (alpha <= 0.004f) {
            return;
        }
        Draw.line(sb, px + nx * from, py + ny * from,
                px + nx * to, py + ny * to, thick, color, alpha);
    }
}
