package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.helpers.ImageMaster;

/**
 * 用游戏自带贴图「画图案」的画笔。
 * <p>
 * 卡面里的五线谱、漩涡、符文、水带、Ω、花瓣、掌印、火焰环都不是现成特效，
 * 而是图形。这里只提供最底层的几笔：光条、柔光、星点、椭圆环、弧、人形剪影、折线铭文，
 * 上层的每种图案都由这几笔拼出来，因此不需要任何外部贴图，
 * 也不会和原版已有的特效撞形。
 * <p>
 * <h2>厚度下限（本轮新增，重要）</h2>
 * 之前大量调用点传的是 {@code 1.3f * sc}、{@code 1.6f * sc}、{@code 2.2f * sc} 这类厚度，
 * 在手机上就是一根根发丝 —— 又碎又廉价，这是「还存在一些以细线为主的特效」的根因。
 * <p>
 * 现在在<b>最底层统一兜底</b>：
 * <ul>
 *   <li>{@link #line} 的厚度钳到至少 {@link #MIN_LINE}（4.5px）</li>
 *   <li>{@link #arc} 同理，所以所有环/弧也跟着变粗</li>
 * </ul>
 * 这样即使某个调用点写了很小的值，也不会画出细线。
 * 需要更细的「内部细节」时，不要用 line，用 {@link #glow} 画光斑。
 * <p>
 * 所有方法都走加色混合（调用方负责 setBlendFunction），并复用同一个 Color 对象：
 * 这些图案一帧要画几十笔，不能每笔都 new 一个 Color。
 */
final class Draw {
    static final TextureAtlas.AtlasRegion LINE = ImageMaster.THICK_3D_LINE;
    static final TextureAtlas.AtlasRegion GLOW = ImageMaster.GLOW_SPARK_2;
    static final TextureAtlas.AtlasRegion STAR = ImageMaster.TINY_STAR;

    /**
     * 线条厚度绝对下限（像素）。
     * <p>
     * 4.5px 是「在手机上仍看得出是一根有体积的线」的下限；
     * 低于它就退化为发丝，读作「廉价特效」。
     */
    static final float MIN_LINE = 4.5f;

    private static final Color C = new Color();

    private Draw() {
    }

    static Color tint(Color color, float alpha) {
        C.set(color);
        C.a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        return C;
    }

    /**
     * 两点之间的一根光条。
     * <p>
     * 厚度会被钳到至少 {@link #MIN_LINE}，所以调用方写多小的值都不会画出细线。
     */
    static void line(SpriteBatch sb, float x1, float y1, float x2, float y2,
                     float thick, Color color, float alpha) {
        if (alpha <= 0.004f) {
            return;
        }
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len <= 0.6f) {
            return;
        }
        // 厚度兜底：细线一律加粗到可见
        float t = Math.max(MIN_LINE, thick);
        float rot = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        float mx = (x1 + x2) * 0.5f;
        float my = (y1 + y2) * 0.5f;
        sb.setColor(tint(color, alpha));
        // 外面再垫一层更宽更淡的同色光，让光条有体积而不是薄片
        sb.draw(GLOW, mx - len * 0.58f, my - t * 1.45f,
                len * 0.58f, t * 1.45f, len * 1.16f, t * 2.9f,
                1.0f, 1.0f, rot);
        sb.setColor(tint(color, alpha));
        sb.draw(LINE, mx - len * 0.5f, my - t * 0.5f,
                len * 0.5f, t * 0.5f, len, t, 1.0f, 1.0f, rot);
    }

    /** 以中心定位的柔光椭圆：做光斑、雾、发光核。 */
    static void glow(SpriteBatch sb, float cx, float cy, float w, float h,
                     float rotation, Color color, float alpha) {
        if (alpha <= 0.004f || w <= 0.5f || h <= 0.5f) {
            return;
        }
        sb.setColor(tint(color, alpha));
        sb.draw(GLOW, cx - w * 0.5f, cy - h * 0.5f,
                w * 0.5f, h * 0.5f, w, h, 1.0f, 1.0f, rotation);
    }

    /** 星点。 */
    static void star(SpriteBatch sb, float cx, float cy, float size, float rotation,
                     Color color, float alpha) {
        if (alpha <= 0.004f || size <= 0.5f) {
            return;
        }
        sb.setColor(tint(color, alpha));
        sb.draw(STAR, cx - size * 0.5f, cy - size * 0.5f,
                size * 0.5f, size * 0.5f, size, size, 1.0f, 1.0f, rotation);
    }

    /** 完整椭圆环。 */
    static void ring(SpriteBatch sb, float cx, float cy, float rx, float ry,
                     float thick, Color color, float alpha, int segments) {
        arc(sb, cx, cy, rx, ry, 0.0f, 360.0f, thick, color, alpha, segments);
    }

    /**
     * 椭圆弧：角度用数学方向（0 向右、逆时针增大）。
     * <p>
     * 厚度的下限比直线更严：环是主体轮廓，太细就完全看不出是环。
     */
    static void arc(SpriteBatch sb, float cx, float cy, float rx, float ry,
                    float fromDeg, float sweepDeg, float thick,
                    Color color, float alpha, int segments) {
        if (alpha <= 0.004f) {
            return;
        }
        int seg = Math.max(3, segments);
        float step = sweepDeg / seg;
        // 环的厚度兜底
        float t = Math.max(MIN_LINE, thick);
        float prevX = 0.0f;
        float prevY = 0.0f;
        for (int i = 0; i <= seg; ++i) {
            float rad = (fromDeg + step * i) * MathUtils.degreesToRadians;
            float x = cx + MathUtils.cos(rad) * rx;
            float y = cy + MathUtils.sin(rad) * ry;
            if (i > 0) {
                // 直接内联绘制，避免再次经过 line 的额外光晕（环会叠得太亮）
                float dx = x - prevX;
                float dy = y - prevY;
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                if (len > 0.4f) {
                    float rot = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
                    sb.setColor(tint(color, alpha));
                    sb.draw(LINE, (prevX + x) * 0.5f - len * 0.5f,
                            (prevY + y) * 0.5f - t * 0.5f,
                            len * 0.5f, t * 0.5f, len, t, 1.0f, 1.0f, rot);
                }
            }
            prevX = x;
            prevY = y;
        }
    }

    /**
     * 人形剪影：头 + 肩 + 躯干 + 双腿。
     * 「洞悉现实」的镜面分身、「突袭」的残影都用它，读起来是一个人而不是一团光。
     */
    static void silhouette(SpriteBatch sb, float cx, float cy, float size,
                           Color color, float alpha) {
        if (alpha <= 0.004f || size <= 1.0f) {
            return;
        }
        glow(sb, cx, cy + size * 0.42f, size * 0.34f, size * 0.42f, 0.0f, color, alpha * 0.95f);
        line(sb, cx - size * 0.28f, cy + size * 0.26f, cx + size * 0.28f, cy + size * 0.26f,
                size * 0.13f, color, alpha * 0.8f);
        line(sb, cx, cy + size * 0.24f, cx, cy - size * 0.26f, size * 0.30f, color, alpha * 0.75f);
        line(sb, cx, cy - size * 0.20f, cx - size * 0.15f, cy - size * 0.60f,
                size * 0.11f, color, alpha * 0.66f);
        line(sb, cx, cy - size * 0.20f, cx + size * 0.15f, cy - size * 0.60f,
                size * 0.11f, color, alpha * 0.66f);
    }

    /**
     * 抽象符文（折线铭文）线段表，坐标归一化到 -1~1。
     * 每条符文 4 段 = 16 个数；「研习」的浮空铭文就按这张表画出来，
     * 不必再造一套贴图，也不像原版任何现成图案。
     */
    static final float[][] RUNE = {
        {-0.70f, -0.85f, 0.62f, -0.85f, 0.62f, -0.85f, 0.62f, 0.15f,
         0.62f, 0.15f, -0.10f, 0.90f, -0.10f, 0.90f, -0.70f, 0.00f},
        {-0.55f, 0.92f, -0.15f, 0.05f, -0.15f, 0.05f, 0.65f, -0.35f,
         0.65f, -0.35f, 0.15f, -0.90f, 0.15f, -0.90f, 0.72f, -0.72f},
        {-0.68f, 0.55f, 0.00f, 0.90f, 0.00f, 0.90f, 0.68f, 0.55f,
         0.68f, 0.55f, -0.68f, 0.10f, -0.68f, 0.10f, 0.68f, 0.10f},
        {0.00f, 0.95f, 0.00f, -0.90f, -0.60f, 0.30f, 0.60f, 0.30f,
         0.60f, 0.30f, 0.60f, -0.80f, -0.60f, 0.30f, -0.60f, -0.80f},
        {-0.72f, -0.90f, -0.72f, 0.60f, -0.72f, 0.60f, 0.55f, 0.60f,
         0.55f, 0.60f, 0.20f, -0.25f, 0.20f, -0.25f, -0.30f, -0.90f},
        {0.00f, -0.95f, -0.65f, -0.20f, -0.65f, -0.20f, 0.00f, 0.45f,
         0.00f, 0.45f, 0.65f, -0.20f, 0.65f, -0.20f, 0.00f, -0.95f}
    };
}
