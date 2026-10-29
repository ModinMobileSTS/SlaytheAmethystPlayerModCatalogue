package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.helpers.ImageMaster;

/**
 * 程序化画一枚音符，支持五线谱常见的几种基本形态。
 * <p>
 * <h2>支持的音符类型（{@link Kind}）</h2>
 * <ul>
 *   <li>{@link Kind#QUARTER} 四分音符 ♩：实心符头 + 符干</li>
 *   <li>{@link Kind#EIGHTH} 八分音符 ♪：实心符头 + 符干 + 单符尾</li>
 *   <li>{@link Kind#SIXTEENTH} 十六分音符：实心符头 + 符干 + 双符尾</li>
 *   <li>{@link Kind#HALF} 二分音符：<b>空心</b>符头 + 符干（无符尾）</li>
 * </ul>
 * <p>
 * <h2>几何定义（单位 = size）</h2>
 * <pre>
 *   符头：椭圆，中心 (-0.16, -0.34)，半径 0.30×0.22，本征倾角 -25°
 *   符干：竖线，从符头右上 (-0.02, -0.30) 向上到 (-0.02, +0.52)
 *   符尾：从符干顶端向右下弯出的撇，八分一撇、十六分两撇
 * </pre>
 * <h2>整体旋转</h2>
 * {@code rotation} 作用于<b>整个字形</b>，所以谱面倾斜时音符跟着倾斜，
 * 不会出现「头转了干没转」的散架。符头再叠加 {@link #HEAD_ROT} 的本征倾角。
 * <h2>厚度下限（本轮新增）</h2>
 * 符干与符尾原先按 {@code STEM_W × size} 算，音符缩小时就变成发丝。
 * 现在 {@link #stroke} 内部把厚度钳到至少 {@link #MIN_STROKE}（4px），
 * 所以音符在任何尺寸下都有体积。
 * <h2>空心符头的画法</h2>
 * 不能用 {@code Draw.ring}（那是轴对齐的椭圆环，无法旋转），
 * 这里自己按角度采样拼出一圈<b>跟随旋转</b>的椭圆环。
 */
final class NoteGlyph {
    /** 音符类型。 */
    enum Kind {
        QUARTER,
        EIGHTH,
        SIXTEENTH,
        HALF;

        private static final Kind[] ALL = values();

        /** 按权重随机：八分与十六分最常见，二分最少。 */
        static Kind random() {
            int r = MathUtils.random(0, 9);
            if (r < 4) {
                return EIGHTH;
            }
            if (r < 7) {
                return SIXTEENTH;
            }
            if (r < 9) {
                return QUARTER;
            }
            return HALF;
        }

        /** 按索引取（用于首次填充时也带点变化，而不是清一色）。 */
        static Kind byIndex(int i) {
            return ALL[((i % ALL.length) + ALL.length) % ALL.length];
        }
    }

    /** 符头与符尾用柔光贴图 —— 压扁后是干净的椭圆。 */
    private static final TextureAtlas.AtlasRegion GLOW = ImageMaster.GLOW_SPARK_2;
    /** 符干用条状贴图，等宽。 */
    private static final TextureAtlas.AtlasRegion LINE = ImageMaster.THICK_3D_LINE;
    private static final Color SCRATCH = new Color();

    /** 符干 / 符尾的最小厚度（像素）。低于它就变成发丝了。 */
    private static final float MIN_STROKE = 4.0f;

    private static final float HEAD_X = -0.16f;
    private static final float HEAD_Y = -0.34f;
    private static final float HEAD_RX = 0.30f;
    private static final float HEAD_RY = 0.22f;
    /** 符头本征倾角（度）。 */
    private static final float HEAD_ROT = -25.0f;
    private static final float STEM_X = -0.02f;
    private static final float STEM_BOTTOM = -0.30f;
    private static final float STEM_TOP = 0.52f;
    private static final float STEM_W = 0.075f;

    private NoteGlyph() {
    }

    /**
     * 画一枚音符。
     *
     * @param stroke 描边宽度倍率：0 = 不描边
     */
    static void draw(SpriteBatch sb, float cx, float cy, float size, float rotation,
                     Kind kind, Color color, float alpha, float stroke) {
        if (alpha <= 0.004f || size <= 0.5f) {
            return;
        }
        if (stroke > 0.01f) {
            drawParts(sb, cx, cy, size * (1.0f + stroke), rotation, kind, Color.WHITE,
                    alpha * 0.80f);
        }
        drawParts(sb, cx, cy, size, rotation, kind, color, alpha);
    }

    /** 默认八分音符、带描边。 */
    static void draw(SpriteBatch sb, float cx, float cy, float size, float rotation,
                     Color color, float alpha, float stroke) {
        draw(sb, cx, cy, size, rotation, Kind.EIGHTH, color, alpha, stroke);
    }

    static void draw(SpriteBatch sb, float cx, float cy, float size, float rotation,
                     Color color, float alpha) {
        draw(sb, cx, cy, size, rotation, Kind.EIGHTH, color, alpha, 0.0f);
    }

    private static void drawParts(SpriteBatch sb, float cx, float cy, float size,
                                  float rotation, Kind kind, Color color, float alpha) {
        float rad = rotation * MathUtils.degreesToRadians;
        float cos = MathUtils.cos(rad);
        float sin = MathUtils.sin(rad);
        SCRATCH.set(color);
        SCRATCH.a = Math.min(1.0f, alpha);
        sb.setColor(SCRATCH);

        // ---- 符头 ----
        float hx = cx + (HEAD_X * cos - HEAD_Y * sin) * size;
        float hy = cy + (HEAD_X * sin + HEAD_Y * cos) * size;
        float headW = HEAD_RX * 2.0f * size;
        float headH = HEAD_RY * 2.0f * size;
        if (kind == Kind.HALF) {
            // 空心符头：自己采样一圈，才能跟随旋转
            hollowHead(sb, hx, hy, headW * 0.5f, headH * 0.5f,
                    rotation + HEAD_ROT, Math.max(MIN_STROKE, size * 0.072f), color, alpha);
        } else {
            sb.setColor(SCRATCH);
            sb.draw(GLOW, hx - headW * 0.5f, hy - headH * 0.5f,
                    headW * 0.5f, headH * 0.5f, headW, headH, 1.0f, 1.0f,
                    rotation + HEAD_ROT);
            // 符头外再垫一层同色柔光，让它有体积而不是一片薄色
            Draw.glow(sb, hx, hy, headW * 1.5f, headH * 1.5f, rotation + HEAD_ROT,
                    color, alpha * 0.22f);
        }

        // ---- 符干 ----
        float s0x = cx + (STEM_X * cos - STEM_BOTTOM * sin) * size;
        float s0y = cy + (STEM_X * sin + STEM_BOTTOM * cos) * size;
        float s1x = cx + (STEM_X * cos - STEM_TOP * sin) * size;
        float s1y = cy + (STEM_X * sin + STEM_TOP * cos) * size;
        sb.setColor(SCRATCH);
        stroke(sb, s0x, s0y, s1x, s1y, STEM_W * size);

        // ---- 符尾 ----
        int tails = kind == Kind.EIGHTH ? 1 : (kind == Kind.SIXTEENTH ? 2 : 0);
        for (int i = 0; i < tails; ++i) {
            float top = STEM_TOP - 0.16f * i;
            float t0x = cx + (STEM_X * cos - top * sin) * size;
            float t0y = cy + (STEM_X * sin + top * cos) * size;
            float t1x = cx + ((STEM_X + 0.34f - 0.03f * i) * cos
                    - (top - 0.16f) * sin) * size;
            float t1y = cy + ((STEM_X + 0.34f - 0.03f * i) * sin
                    + (top - 0.16f) * cos) * size;
            sb.setColor(SCRATCH);
            stroke(sb, t0x, t0y, t1x, t1y, STEM_W * size * (1.15f - 0.10f * i));
        }
    }

    /**
     * 空心符头：沿椭圆采样一圈，每段用条状贴图铺开。
     * 因为每段都是「两点定线」，所以整体自然跟随 rotation。
     */
    private static void hollowHead(SpriteBatch sb, float cx, float cy, float rx, float ry,
                                   float rotationDeg, float thick, Color color, float alpha) {
        float rad = rotationDeg * MathUtils.degreesToRadians;
        float cos = MathUtils.cos(rad);
        float sin = MathUtils.sin(rad);
        SCRATCH.set(color);
        SCRATCH.a = Math.min(1.0f, alpha);

        int seg = 18;
        float prevX = 0.0f;
        float prevY = 0.0f;
        for (int i = 0; i <= seg; ++i) {
            float a = MathUtils.PI2 * i / seg;
            float lx = MathUtils.cos(a) * rx;
            float ly = MathUtils.sin(a) * ry;
            float x = cx + lx * cos - ly * sin;
            float y = cy + lx * sin + ly * cos;
            if (i > 0) {
                sb.setColor(SCRATCH);
                stroke(sb, prevX, prevY, x, y, thick);
            }
            prevX = x;
            prevY = y;
        }
    }

    /**
     * 两点定一条线段，用条状贴图铺开。
     * <p>
     * 厚度会被钳到至少 {@link #MIN_STROKE}：音符缩小时符干不会退化为发丝。
     */
    private static void stroke(SpriteBatch sb, float x1, float y1, float x2, float y2,
                               float thick) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len <= 0.4f) {
            return;
        }
        float t = Math.max(MIN_STROKE, thick);
        float rot = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        float mx = (x1 + x2) * 0.5f;
        float my = (y1 + y2) * 0.5f;
        sb.draw(LINE, mx - len * 0.5f, my - t * 0.5f,
                len * 0.5f, t * 0.5f, len, t, 1.0f, 1.0f, rot);
    }
}
