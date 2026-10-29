package sts.ryoikitenkai.compat.green;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;

/**
 * 本补丁的底层画笔：等宽线、柔光、均匀光带。
 * <h2>三种笔，各管一件事</h2>
 * <table border="1">
 *   <tr><td>{@link #line}</td><td>{@code THICK_3D_LINE}</td>
 *       <td><b>一条完整的线</b>（刻痕、护手、柄、角括）—— 只画一段，不要拼接</td></tr>
 *   <tr><td>{@link #glow}</td><td>{@code GLOW_SPARK_2}</td>
 *       <td>点位柔光（尖端、能量节点、光晕）—— 圆各向同性，叠多少都不会有接缝</td></tr>
 *   <tr><td>{@link #stroke}</td><td>{@code WHITE_SQUARE_IMG}</td>
 *       <td><b>曲线 / 长折线</b>（环、连线）—— 纯白方片沿长度方向没有渐变，
 *       拼多少段都无缝</td></tr>
 * </table>
 * <h2>「一格一格拼起来」是怎么来的</h2>
 * {@code THICK_3D_LINE} 是给「画一条完整的线」设计的贴图：它<b>沿长度方向有明暗变化</b>
 * （3D 倒角与端部收光）。切成几十段首尾相接拼成圆环时，每一段都重复一遍端部着色 ——
 * 于是环上每隔一小段出现一次明暗台阶。所以多段拼接一律改用 {@link #stroke}。
 * <h2>「不够平滑」是怎么来的（本轮修的）</h2>
 * {@link #stroke} 的外晕是「沿路径按固定间距撒圆点」。间距若取厚度的 0.85 倍，
 * 在细线（厚度小）上间距也小、看不出问题；但靶环很粗（厚度大），
 * 间距就跟着变大，于是两点之间露出<b>直线弦</b> —— 圆弧看上去一段一段的。
 * <p>
 * 现在把间距改成<b>同时受「厚度」与「半径」约束</b>：取两者中更小的，
 * 并且外晕的柔光点直径放大到能盖住间距的三倍以上。
 * 另外内芯也用更密的采样点（见各调用的采样段数），曲线段与段之间的转角随之变小。
 * <h2>厚度下限：{@link #MIN_LINE} = 7px × scale</h2>
 * 之前是 {@code 4.5f}，手机上读作发丝；而且调用点还写 {@code MIN_LINE * 0.8}
 * 把下限又压回去。所以：下限提到 7px，并<b>禁止</b> {@code MIN_LINE * k}（k&lt;1）。
 * <h2>缓动也在这里</h2>
 * {@link #easeOut} / {@link #easeIn} / {@link #smoothstep} 是单调曲线。
 * <b>不要</b>用 {@code swingOut} 再 {@code clamp(0,1)}：过冲被切掉后末尾会留下一段死平。
 */
final class GreenDraw {
    static final TextureAtlas.AtlasRegion LINE = ImageMaster.THICK_3D_LINE;
    static final TextureAtlas.AtlasRegion GLOW = ImageMaster.GLOW_SPARK_2;

    /**
     * 纯白方片包成 {@link TextureRegion}。
     * <p>
     * {@code WHITE_SQUARE_IMG} 是 {@code Texture}，{@code SpriteBatch} 对 {@code Texture}
     * 的重载<b>没有 rotation 参数</b>；包成 {@code TextureRegion} 之后就能用
     * 「原点 + 宽高 + 旋转」那一版 —— 这才是能斜着摆的实心条。
     */
    private static final TextureRegion WHITE =
            new TextureRegion(ImageMaster.WHITE_SQUARE_IMG);

    /** 线条厚度绝对下限（像素，未乘 scale）。 */
    static final float MIN_LINE = 7.0f;

    private static final Color C = new Color();

    private GreenDraw() {
    }

    static Color tint(Color color, float alpha) {
        C.set(color);
        C.a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        return C;
    }

    /** 厚度兜底。<b>不要</b>在调用点写 {@code MIN_LINE * 0.8} 之类的系数。 */
    static float width(float thick) {
        return Math.max(MIN_LINE * Settings.scale, thick);
    }

    // ---------------- 单段线 ----------------

    /**
     * 两点之间的一根<b>完整</b>等宽线（{@code THICK_3D_LINE}）。
     * <p>
     * 只用于「本来就只有一段」的东西：刻痕、护手、柄、卡框角括。
     * <b>不要</b>用它拼曲线或长折线。
     */
    static void line(SpriteBatch sb, float x1, float y1, float x2, float y2,
                     float thick, Color color, float alpha) {
        if (alpha <= 0.004f) {
            return;
        }
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len <= 0.5f) {
            return;
        }
        float t = width(thick);
        float rot = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        float mx = (x1 + x2) * 0.5f;
        float my = (y1 + y2) * 0.5f;
        sb.setColor(tint(color, alpha));
        sb.draw(LINE, mx - len * 0.5f, my - t * 0.5f,
                len * 0.5f, t * 0.5f, len, t, 1.0f, 1.0f, rot);
    }

    // ---------------- 柔光 ----------------

    /** 以中心定位的柔光椭圆：光斑、扫光、尖端、能量节点。 */
    static void glow(SpriteBatch sb, float cx, float cy, float w, float h,
                     float rotation, Color color, float alpha) {
        if (alpha <= 0.004f || w <= 0.5f || h <= 0.5f) {
            return;
        }
        sb.setColor(tint(color, alpha));
        sb.draw(GLOW, cx - w * 0.5f, cy - h * 0.5f,
                w * 0.5f, h * 0.5f, w, h, 1.0f, 1.0f, rotation);
    }

    // ---------------- 均匀四边形（多段拼接专用） ----------------

    /**
     * 两点之间的一段<b>均匀实心条</b>（纯白方片，可任意旋转）。
     * <p>
     * 纯白沿长度方向<b>没有任何渐变</b>，所以相邻段首尾相接时接缝完全不可见。
     * 长度多给 6%，避免斜接处出现发丝缝。
     */
    static void quad(SpriteBatch sb, float x1, float y1, float x2, float y2,
                     float thick, Color color, float alpha) {
        if (alpha <= 0.004f) {
            return;
        }
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len <= 0.4f) {
            return;
        }
        float t = width(thick);
        float rot = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        float mx = (x1 + x2) * 0.5f;
        float my = (y1 + y2) * 0.5f;
        float l = len * 1.06f;
        sb.setColor(tint(color, alpha));
        sb.draw(WHITE, mx - l * 0.5f, my - t * 0.5f,
                l * 0.5f, t * 0.5f, l, t, 1.0f, 1.0f, rot);
    }

    /**
     * <b>光滑光带</b>：三层叠出「有截面的光线」，用来画环与曲线。
     * <ol>
     *   <li><b>外晕</b>：沿路径密排柔和圆点（各向同性，重叠即融）</li>
     *   <li><b>内芯</b>：逐段均匀实心条（无长度方向渐变）</li>
     *   <li><b>高光</b>：再叠一条更细更亮的同路径均匀条 —— 质感的来源</li>
     * </ol>
     * <h2>采样密度（本轮修的「不平滑」）</h2>
     * 外晕点距取 <b>{@code min(厚度 × 0.42, 路径段长 × 0.5)}</b>：
     * 粗线上点距也不会被厚度顶大，细线上也不会稀疏。
     * 点的直径给到点距的 3 倍，相邻点重叠 2/3，圆弧因此看不出弦。
     *
     * @param n 路径点数（含首尾），至少 2
     */
    static void stroke(SpriteBatch sb, float[] xs, float[] ys, int n,
                       float thick, Color color, float alpha) {
        if (n < 2 || alpha <= 0.004f) {
            return;
        }
        float t = width(thick);
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);

        // ---- ① 外晕：圆点密排 ----
        // 点距同时受厚度与段长约束，避免粗线在两点之间露出直线弦
        float stepThick = t * 0.42f;
        float minStep = 1.6f * Settings.scale;
        float dot = t * 2.9f;
        float carry = 0.0f;
        for (int i = 1; i < n; ++i) {
            float dx = xs[i] - xs[i - 1];
            float dy = ys[i] - ys[i - 1];
            float segLen = (float) Math.sqrt(dx * dx + dy * dy);
            if (segLen < 0.0001f) {
                continue;
            }
            float ux = dx / segLen;
            float uy = dy / segLen;
            float step = Math.max(minStep, Math.min(stepThick, segLen * 0.5f));
            float d = carry;
            while (d <= segLen) {
                glow(sb, xs[i - 1] + ux * d, ys[i - 1] + uy * d,
                        dot, dot, 0.0f, color, a * 0.14f);
                d += step;
            }
            carry = d - segLen;
        }

        // ---- ② 内芯 + ③ 高光：两条同路径、不同宽度的均匀条 ----
        float core = t * 0.64f;
        float hi = t * 0.26f;
        for (int i = 1; i < n; ++i) {
            quad(sb, xs[i - 1], ys[i - 1], xs[i], ys[i], core, color, a * 0.80f);
            quad(sb, xs[i - 1], ys[i - 1], xs[i], ys[i], hi, color, a * 0.58f);
        }
    }

    // ---------------- 缓动 ----------------

    /**
     * 单调的「落位」缓动：{@code pow2Out}。
     * <b>不要用 {@code swingOut} 再 {@code clamp(0,1)}</b> —— 末尾会留下一段死平。
     */
    static float easeOut(float t) {
        float x = MathUtils.clamp(t, 0.0f, 1.0f);
        return 1.0f - (1.0f - x) * (1.0f - x);
    }

    /** 单调的「收走」缓动：{@code pow2In}。 */
    static float easeIn(float t) {
        float x = MathUtils.clamp(t, 0.0f, 1.0f);
        return x * x;
    }

    /** smoothstep：两端斜率都是 0，用于 alpha 包络。 */
    static float smoothstep(float t) {
        float x = MathUtils.clamp(t, 0.0f, 1.0f);
        return x * x * (3.0f - 2.0f * x);
    }
}
