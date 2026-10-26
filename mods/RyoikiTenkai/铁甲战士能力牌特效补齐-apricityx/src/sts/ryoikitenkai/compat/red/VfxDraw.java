package sts.ryoikitenkai.compat.red;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.helpers.ImageMaster;

/**
 * 几个特效共用的绘制工具。
 * <p>
 * 游戏没有现成的「线段」图元，常见做法是拿一张贴图水平拉长当短棒用。
 * <b>贴图的选择很关键</b>：用径向渐变的光斑（GLOW_SPARK_2）拼接时，
 * 每一段的两端都会自然衰减，段与段的接缝因此发暗，整条线看起来断断续续。
 * 所以这里分成两套：
 * <ul>
 *   <li>{@link #bar}：柔和短棒，适合光晕、发光条；</li>
 *   <li>{@link #line}：实心贴图，两端不衰减，适合拼出连续不断的轮廓/丝带。</li>
 * </ul>
 * 所有方法都只写 sb 的当前颜色，不改动调用方传进来的 Color 对象，
 * 因为那些颜色往往是静态常量，被改掉会污染其它特效。
 */
public final class VfxDraw {
    /** 柔光贴图：两端会衰减，只适合单段短棒。 */
    private static final TextureAtlas.AtlasRegion GLOW = ImageMaster.GLOW_SPARK_2;
    /** 实心环形贴图：整张均匀，拉长后拼接不会出现接缝暗带。 */
    private static final TextureAtlas.AtlasRegion SOLID = ImageMaster.WHITE_RING;
    private static final Color SCRATCH = new Color();

    private VfxDraw() {
    }

    /** 画一根柔和的发光短棒（两端会衰减，不要用来拼长线）。 */
    public static void bar(SpriteBatch sb, float x1, float y1, float x2, float y2,
                           Color color, float alpha, float thickness) {
        segment(sb, GLOW, x1, y1, x2, y2, color, alpha, thickness);
    }

    /**
     * 画一段实心连线。两端不衰减，因此多段首尾相接可以拼出连续不断的线条。
     * 用于需要「清晰轮廓」的场合，例如剪影描边、DNA 双链。
     */
    public static void line(SpriteBatch sb, float x1, float y1, float x2, float y2,
                            Color color, float alpha, float thickness) {
        segment(sb, SOLID, x1, y1, x2, y2, color, alpha, thickness);
    }

    private static void segment(SpriteBatch sb, TextureAtlas.AtlasRegion tex,
                                float x1, float y1, float x2, float y2,
                                Color color, float alpha, float thickness) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.4f || alpha <= 0.004f || thickness <= 0.01f) {
            return;
        }
        float scaleX = len / tex.packedWidth;
        float scaleY = thickness / tex.packedHeight;
        SCRATCH.set(color);
        SCRATCH.a = Math.min(1.0f, alpha);
        sb.setColor(SCRATCH);
        sb.draw(tex,
                (x1 + x2) * 0.5f - tex.packedWidth * scaleX * 0.5f,
                (y1 + y2) * 0.5f - tex.packedHeight * scaleY * 0.5f,
                tex.packedWidth * 0.5f, tex.packedHeight * 0.5f,
                tex.packedWidth, tex.packedHeight,
                scaleX, scaleY,
                MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees);
    }

    /**
     * 画一个椭圆环。sweepPhase 用来做沿环移动的高光：
     * 与它同向的那一段会更亮，看起来像有一道光在壳上绕圈。
     *
     * @param solid true 用实心贴图（轮廓清晰、连续），false 用柔光贴图
     */
    public static void ellipseRing(SpriteBatch sb, float cx, float cy, float rx, float ry,
                                   int segments, Color color, float alpha, float thickness,
                                   float sweepPhase, float sweepStrength, boolean solid) {
        float prevX = 0.0f;
        float prevY = 0.0f;
        for (int i = 0; i <= segments; ++i) {
            float a = MathUtils.PI2 * i / segments;
            float x = cx + rx * MathUtils.cos(a);
            float y = cy + ry * MathUtils.sin(a);
            if (i > 0) {
                float segAlpha = alpha;
                if (sweepStrength > 0.0f) {
                    float d = MathUtils.cos(a - sweepPhase);
                    if (d > 0.0f) {
                        segAlpha *= 1.0f + sweepStrength * d * d * d * d;
                    }
                }
                if (solid) {
                    line(sb, prevX, prevY, x, y, color, segAlpha, thickness);
                } else {
                    bar(sb, prevX, prevY, x, y, color, segAlpha, thickness);
                }
            }
            prevX = x;
            prevY = y;
        }
    }

    /** 兼容旧调用：默认用柔光贴图。 */
    public static void ellipseRing(SpriteBatch sb, float cx, float cy, float rx, float ry,
                                   int segments, Color color, float alpha, float thickness,
                                   float sweepPhase, float sweepStrength) {
        ellipseRing(sb, cx, cy, rx, ry, segments, color, alpha, thickness, sweepPhase, sweepStrength, false);
    }
}
