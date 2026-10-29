package sts.ryoikitenkai.compat.red;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;

/**
 * 本补丁的底层画笔：柔光、实心条、光条、粗弧。
 * <p>
 * <h2>为什么补丁需要自己的一份</h2>
 * 铁甲战士补丁与观者补丁是两个独立的 JAR，不能互相 import，
 * 所以这里留一份最小实现。观者三张牌的经验已经证明：
 * <b>线宽必须在画笔层统一兜底</b>，否则散落在各调用点的小厚度
 * （{@code 1.4f}、{@code 1.6f}、{@code 2.2f}）在手机上全是发丝。
 * <h2>两种条：光条 vs 实心条</h2>
 * <ul>
 *   <li>{@link #line}：<b>光条</b>。LINE 外面再垫一层同色柔光，
 *       适合光芽、光弧、能量条（进化用）</li>
 *   <li>{@link #bar}：<b>实心条</b>。只有 LINE 本身，没有外发光，
 *       适合甲片、碑面这类「有实体材质」的东西（金属化用）。
 *       拿 {@code line} 画甲片会让甲片看成一团发光的蓝条，读不出金属。
 *       ——这正是规范 §5「实心材质」的要求</li>
 * </ul>
 * <h2>厚度下限</h2>
 * <ul>
 *   <li>{@link #MIN_LINE} = {@code 4.5f × Settings.scale}：
 *       「在手机上仍看得出是一根有体积的线」的下限</li>
 *   <li>{@link #bar}、{@link #line}、{@link #arc} 都钳到它，调用点写多小都不会退化成发丝</li>
 *   <li>需要更细的「内部细节」时不要用条，改用 {@link #glow} 画光斑</li>
 * </ul>
 * <b>注意下限本身要乘 {@code Settings.scale}</b>：{@code Settings.scale}
 * = 实际渲染分辨率 ÷ 1920，手机分辨率远高于 1920 时它大于 1。
 * 裸写 4.5f 在 2560 宽的屏上只有基准下 3.4px 的相对大小。
 * <h2>颜色</h2>
 * 所有方法都走加色混合（调用方负责 setBlendFunction），并复用同一个 Color 对象：
 * 这些图案一帧要画几十笔，不能每笔都 new。
 * <b>但 {@link #tint} 返回的是共享对象，不能跨调用持有。</b>
 */
final class Draw {
    static final TextureAtlas.AtlasRegion LINE = ImageMaster.THICK_3D_LINE;
    static final TextureAtlas.AtlasRegion GLOW = ImageMaster.GLOW_SPARK_2;

    /** 线条厚度绝对下限（已乘 scale）。 */
    static final float MIN_LINE = 4.5f * Settings.scale;

    private static final Color C = new Color();

    private Draw() {
    }

    static Color tint(Color color, float alpha) {
        C.set(color);
        C.a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        return C;
    }

    /** 以中心定位的柔光椭圆：做光斑、雾、发光核、铆钉、甲片接缝。 */
    static void glow(SpriteBatch sb, float cx, float cy, float w, float h,
                     float rotation, Color color, float alpha) {
        if (alpha <= 0.004f || w <= 0.5f || h <= 0.5f) {
            return;
        }
        sb.setColor(tint(color, alpha));
        sb.draw(GLOW, cx - w * 0.5f, cy - h * 0.5f,
                w * 0.5f, h * 0.5f, w, h, 1.0f, 1.0f, rotation);
    }

    /**
     * 两点之间的一根<b>实心条</b>：只有线段贴图，没有外发光。
     * <p>
     * 用来画「有材质」的东西（甲片、铭文、碑面）。
     * 厚度钳到至少 {@link #MIN_LINE}。
     */
    static void bar(SpriteBatch sb, float x1, float y1, float x2, float y2,
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
        float t = Math.max(MIN_LINE, thick);
        float rot = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        float mx = (x1 + x2) * 0.5f;
        float my = (y1 + y2) * 0.5f;
        sb.setColor(tint(color, alpha));
        sb.draw(LINE, mx - len * 0.5f, my - t * 0.5f,
                len * 0.5f, t * 0.5f, len, t, 1.0f, 1.0f, rot);
    }

    /**
     * 两点之间的一根<b>光条</b>：实心条外面再垫一层更宽更淡的同色柔光。
     * <p>
     * 用来画「发光」的东西（进化光芽、吸收弧）。厚度同样钳到 {@link #MIN_LINE}。
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
        float t = Math.max(MIN_LINE, thick);
        float rot = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        float mx = (x1 + x2) * 0.5f;
        float my = (y1 + y2) * 0.5f;
        sb.setColor(tint(color, alpha * 0.55f));
        sb.draw(GLOW, mx - len * 0.58f, my - t * 1.45f,
                len * 0.58f, t * 1.45f, len * 1.16f, t * 2.9f,
                1.0f, 1.0f, rot);
        sb.setColor(tint(color, alpha));
        sb.draw(LINE, mx - len * 0.5f, my - t * 0.5f,
                len * 0.5f, t * 0.5f, len, t, 1.0f, 1.0f, rot);
    }

    /** 完整椭圆环（用实心条分段画，不加外发光）。 */
    static void ring(SpriteBatch sb, float cx, float cy, float rx, float ry,
                     float thick, Color color, float alpha, int segments) {
        arc(sb, cx, cy, rx, ry, 0.0f, 360.0f, thick, color, alpha, segments);
    }

    /**
     * 椭圆弧：角度用数学方向（0 向右、逆时针增大）。
     * 厚度钳到 {@link #MIN_LINE}；环是主体轮廓，太细就完全看不出是环。
     */
    static void arc(SpriteBatch sb, float cx, float cy, float rx, float ry,
                    float fromDeg, float sweepDeg, float thick,
                    Color color, float alpha, int segments) {
        if (alpha <= 0.004f || rx <= 0.5f) {
            return;
        }
        int seg = Math.max(3, segments);
        float step = sweepDeg / seg;
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
}
