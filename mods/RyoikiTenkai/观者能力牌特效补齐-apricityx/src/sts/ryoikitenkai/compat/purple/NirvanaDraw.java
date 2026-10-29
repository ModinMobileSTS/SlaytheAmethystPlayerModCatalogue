package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;

/**
 * 「涅槃」的画笔：<b>六边护壁</b>。
 * <p>
 * <h2>为什么是六边形而不是圆</h2>
 * 圆形在这套里已经极度拥挤（如水 / 心之堡垒 / 斋戒 / 虔信都在用环）。
 * 六边形有两个圆没有的性质：
 * <ul>
 *   <li><b>有棱</b>：六条直线段 + 六个顶点，读作「结构 / 甲壳」</li>
 *   <li><b>有内外</b>：棱是分界，里面是「被护住的空间」</li>
 * </ul>
 * 圆读作「光晕」或「环」，六边形读作「壳」。这正是「护壁」需要的词。
 * <h2>怎么画才有金属感（加色混合里没有阴影）</h2>
 * <pre>
 * 1. 内衬  六边形内部的暗玉色填充 —— 给出「里面是实心的」
 * 2. 棱    六条粗直边，主色青铜
 * 3. 亮棱  每条棱再压一条更窄更亮的，读作「被打磨的棱」
 * 4. 顶点  六个角上小块团光，盖住接缝并读作「铆钉」
 * </pre>
 * 关键是第 3 步：**同一条棱上叠两档不同亮的线**，
 * 这一层决定它看起来是「有厚度的金属」还是「一根发光的线」。
 * <h2>禁止项</h2>
 * <ul>
 *   <li><b>不用 {@code Draw.star}</b> —— 旧实现 {@code Motif.MANDALA} 在每个花瓣尖端
 *       都调了它。这是本补丁最后一处星芒违规，本条即为修掉它</li>
 *   <li><b>不画花瓣 / 不画莲花</b> —— 八瓣莲花是具象物，禁止；
 *       而且小尺寸下会读成一团放射状碎线</li>
 *   <li><b>不画细线</b> —— 旧实现用了 {@code 3.4f / 2.4f / 1.4f / 1.8f} 四档厚度，
 *       底层兜底会把它们全拉到 4.5px，层次全部丢失。
 *       本类所有厚度下限 {@code ≥ 5px × scale}，并主动拉开两档差距</li>
 * </ul>
 * <h2>所有固定像素都乘 {@code Settings.scale}</h2>
 * 见虔信那轮查出来的坑。
 */
final class NirvanaDraw {
    /** 棱的最小厚度（基准像素）。 */
    private static final float RIB_MIN = 6.0f;
    /** 亮棱相对棱厚的比例：必须明显更窄，才有「两档」的层次。 */
    private static final float LIGHT_RATIO = 0.40f;
    /** 亮棱的绝对下限（基准像素）。 */
    private static final float LIGHT_MIN = 4.5f;

    private NirvanaDraw() {
    }

    private static float px(float base) {
        return base * Settings.scale;
    }

    /** 算出六边形的六个顶点，写进 {@code outX/outY}。 */
    static void hex(float cx, float cy, float rx, float ry,
                    float spinDeg, float[] outX, float[] outY) {
        for (int k = 0; k < NirvanaFx.SIDES; ++k) {
            float rad = (NirvanaFx.vertexDeg(k) + spinDeg) * MathUtils.degreesToRadians;
            outX[k] = cx + MathUtils.cos(rad) * rx;
            outY[k] = cy + MathUtils.sin(rad) * ry;
        }
    }

    /**
     * 一层六边护壁。
     *
     * @param inner 0~1，内衬填充强度（越内层越实，越外层越透）
     * @param flash 0~1，这一层刚被顶出时的过曝量
     */
    static void shell(SpriteBatch sb, float cx, float cy, float rx, float ry,
                      float spinDeg, float inner, float flash, float alpha) {
        if (alpha <= 0.004f || rx <= 2.0f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        float fl = MathUtils.clamp(flash, 0.0f, 1.0f);

        float[] vx = new float[NirvanaFx.SIDES];
        float[] vy = new float[NirvanaFx.SIDES];
        hex(cx, cy, rx, ry, spinDeg, vx, vy);

        float thick = Math.max(px(RIB_MIN), rx * 0.085f);

        // ---- 1. 内衬：六边形内部一层暗玉色填充 ----
        // 用中心柔光近似（六边形内部用椭圆垫底，棱会盖住边缘）
        float fillA = a * inner * (0.10f + 0.16f * fl);
        if (fillA > 0.004f) {
            Draw.glow(sb, cx, cy, rx * 1.92f, ry * 1.92f, 0.0f,
                    NirvanaFx.JADE, fillA * 0.55f);
            Draw.glow(sb, cx, cy, rx * 1.62f, ry * 1.62f, 0.0f,
                    NirvanaFx.BRONZE, fillA * 0.40f);
        }

        // ---- 2. 棱：六条粗直边 ----
        for (int k = 0; k < NirvanaFx.SIDES; ++k) {
            int j = (k + 1) % NirvanaFx.SIDES;
            Draw.line(sb, vx[k], vy[k], vx[j], vy[j], thick,
                    NirvanaFx.BRONZE, a * 0.80f);
        }

        // ---- 3. 亮棱：同一条棱上再压一条更窄更亮的 ----
        // 这一层决定它是「有厚度的金属」还是「一根发光的线」
        float lightT = Math.max(px(LIGHT_MIN), thick * LIGHT_RATIO);
        for (int k = 0; k < NirvanaFx.SIDES; ++k) {
            int j = (k + 1) % NirvanaFx.SIDES;
            Draw.line(sb, vx[k], vy[k], vx[j], vy[j], lightT,
                    NirvanaFx.EDGE_LIGHT, a * (0.34f + 0.46f * fl));
        }

        // ---- 4. 顶点：六个角上的团光，盖住接缝并读作「铆钉」 ----
        float r = thick * 1.9f;
        for (int k = 0; k < NirvanaFx.SIDES; ++k) {
            Draw.glow(sb, vx[k], vy[k], r, r, 0.0f,
                    NirvanaFx.BRONZE, a * (0.22f + 0.40f * fl));
            Draw.glow(sb, vx[k], vy[k], r * 0.45f, r * 0.45f, 0.0f,
                    NirvanaFx.EDGE_LIGHT, a * (0.14f + 0.52f * fl));
        }
    }

    /**
     * 一道沿护壁扫过的亮环：读作「这一层刚刚成形 / 刚被顶出」。
     * <p>
     * 做法是画一个<b>比目标层略小的六边形</b>，从 0 快速扩到 1 倍并淡出，
     * 于是读起来像一圈光从内侧顶到棱上。
     *
     * @param q 0~1，扩张进度
     */
    static void ring(SpriteBatch sb, float cx, float cy, float rx, float ry,
                     float spinDeg, float q, float alpha) {
        if (alpha <= 0.004f || q <= 0.0f || q >= 1.0f || rx <= 2.0f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        // 末端斜率为 0：余弦淡出，不硬切
        float fade = FastingDraw.cosineOut(q);
        // 半径从 0.55 扩到 1.08（略微冲出再收回）
        float e = 1.0f - (1.0f - q) * (1.0f - q);
        float scale = MathUtils.lerp(0.55f, 1.08f, e);

        float[] vx = new float[NirvanaFx.SIDES];
        float[] vy = new float[NirvanaFx.SIDES];
        hex(cx, cy, rx * scale, ry * scale, spinDeg, vx, vy);

        // 扩到最大时开始收窄，读作「光被棱吸收」
        float thick = Math.max(px(6.0f), rx * 0.10f) * (1.0f - 0.45f * q);
        for (int k = 0; k < NirvanaFx.SIDES; ++k) {
            int j = (k + 1) % NirvanaFx.SIDES;
            Draw.line(sb, vx[k], vy[k], vx[j], vy[j], thick,
                    NirvanaFx.EDGE_LIGHT, a * 0.85f * fade);
        }
        Draw.glow(sb, cx, cy, rx * scale * 2.05f, ry * scale * 2.05f, 0.0f,
                NirvanaFx.BRONZE, a * 0.20f * fade);
    }

    /**
     * 护壁的「薄壳」状态：格挡已经用光时，只剩最内层一圈很淡的轮廓。
     * <p>
     * 需要它是因为：不要把护壁画成「有格挡才有」，那会让能力看起来消失了。
     * 一格薄壳读作「这个能力还装着，只是现在没在挡什么」。
     */
    static void spentShell(SpriteBatch sb, float cx, float cy, float rx, float ry,
                           float spinDeg, float alpha) {
        if (alpha <= 0.004f || rx <= 2.0f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        float[] vx = new float[NirvanaFx.SIDES];
        float[] vy = new float[NirvanaFx.SIDES];
        hex(cx, cy, rx, ry, spinDeg, vx, vy);
        float thick = Math.max(px(5.0f), rx * 0.055f);
        for (int k = 0; k < NirvanaFx.SIDES; ++k) {
            int j = (k + 1) % NirvanaFx.SIDES;
            Draw.line(sb, vx[k], vy[k], vx[j], vy[j], thick,
                    NirvanaFx.BRONZE, a * 0.30f);
        }
        // 六个顶点保留一点光，避免看起来像断开的六段
        float r = thick * 1.4f;
        for (int k = 0; k < NirvanaFx.SIDES; ++k) {
            Draw.glow(sb, vx[k], vy[k], r, r, 0.0f,
                    NirvanaFx.BRONZE, a * 0.22f);
        }
    }
}
