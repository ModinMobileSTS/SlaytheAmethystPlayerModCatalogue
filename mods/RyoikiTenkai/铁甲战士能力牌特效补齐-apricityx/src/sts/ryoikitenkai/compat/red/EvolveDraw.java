package sts.ryoikitenkai.compat.red;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;

/**
 * 「进化」的画笔：<b>会生长的三叉光芽</b>。
 * <p>
 * <h2>为什么单独写一套</h2>
 * 这个补丁里另一张牌（金属化）画的是「硬、冷、反光」的金属；
 * 进化要画的是「软、暖、半透」的生物组织。两者画法完全不同：
 * <ul>
 *   <li><b>金属</b>：等宽硬边、明暗突变、斜掠高光</li>
 *   <li><b>生物</b>：粗柔光带、明暗渐变、尖端更亮、整体略透</li>
 * </ul>
 * 而且这张牌的核心是「<b>生长</b>」—— 芽要能按进度<b>从根部往上抽出来</b>，
 * 并在到顶后<b>分叉张开</b>。这是金属化不需要的能力。
 * <h2>一股芽的四个部件</h2>
 * <pre>
 *   ① 根部团光     埋在腰后的一团暗绿，把芽「种」在身上
 *   ② 主干         一条向上略弯的粗光带（三层不同宽度）
 *   ③ 分叉         到顶后左右各一支小叉，随生长进度张开
 *   ④ 尖端光苞     最亮点，读作「还在往外顶」
 * </pre>
 * <h2>为什么主干要画三层</h2>
 * 柔光贴图拉长后边缘发虚。要让它读作「有截面的茎」而不是「一团雾」，
 * 必须叠三层：宽软层（光晕）、中层（主体）、窄亮层（受光面）。
 * 这与观者那边石碑立面的做法是同一套（规范 §5「实心材质」）。
 * <h2>禁止项</h2>
 * <ul>
 *   <li><b>不用星芒</b> —— 旧实现 {@link GlintEffect} 用
 *       {@code ImageMaster.TINY_STAR} 画十字星。星芒是原版卡面 / 奖章的装饰语汇，
 *       混进战斗特效会读成「贴了个装饰」。现已改成柔光斑 + 细亮弧</li>
 *   <li><b>不画细线</b> —— 本类所有厚度下限 {@code ≥ 4.5px × scale}，
 *       且 {@link Draw#line} 在底层还有一道兜底</li>
 *   <li><b>不画具象植物</b> —— 不画叶子、不画花。只画「向上弯的粗光带 + 分叉 + 光苞」，
 *       这三个抽象元素已足够读作「在生长」，画叶子会退化成一团绿</li>
 * </ul>
 * <h2>所有固定像素都乘 {@code Settings.scale}</h2>
 * {@code Settings.scale} = 实际分辨率 ÷ 1920，裸写像素在真机上会整体缩水。
 * <h2>不每帧 new 数组</h2>
 * 主干路径的采样点用<b>静态暂存数组</b>：同屏同时画多股芽时这些调用是串行的
 * （常驻三股、一次性演出三股都是顺序 render），所以复用安全，
 * 也避免每帧产生 {@code float[11]×2×3} 的垃圾（规范 §3 性能红线）。
 */
final class EvolveDraw {
    /** 主干最小厚度（基准像素）。 */
    private static final float STEM_MIN = 9.0f;
    /** 主干厚度相对高度的比例。 */
    private static final float STEM_RATIO = 0.30f;
    /** 主干的分段数：够弯即可（每段两次 draw，段数过高在手机上白费）。 */
    private static final int SEG = 11;
    /** 分叉的最小线宽（基准像素）。 */
    private static final float FORK_MIN = 6.0f;

    /** 主干路径暂存：X / Y，长度 SEG+1。串行复用，不跨帧保留。 */
    private static final float[] PATH_X = new float[SEG + 1];
    private static final float[] PATH_Y = new float[SEG + 1];

    private EvolveDraw() {
    }

    private static float px(float base) {
        return base * Settings.scale;
    }

    /**
     * 一股光芽。
     *
     * @param cx     根部 X
     * @param baseY  根部 Y
     * @param height 完全长成后的高度
     * @param lean   向哪边弯：−1 左 / 0 直 / +1 右
     * @param grow   生长进度 0~1；小于 1 时只有下半截
     * @param fork   分叉张开程度 0~1（生长完成后才张开）
     * @param alpha  不透明度
     * @param sway   尖端摆动的弧度（rad），由调用方按时间算好
     */
    static void sprout(SpriteBatch sb, float cx, float baseY, float height,
                       float lean, float grow, float fork, float alpha, float sway) {
        if (alpha <= 0.004f || height <= 2.0f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        float g = MathUtils.clamp(grow, 0.0f, 1.0f);
        if (g <= 0.0f) {
            return;
        }
        float h = height * g;
        float thick = Math.max(px(STEM_MIN), height * STEM_RATIO);

        // ---- ① 根部团光：把芽「种」在身上 ----
        Draw.glow(sb, cx, baseY, thick * 3.4f, thick * 2.2f, 0.0f,
                EvolveFx.DEEP, a * 0.42f);
        Draw.glow(sb, cx, baseY, thick * 1.6f, thick * 1.1f, 0.0f,
                EvolveFx.BIO, a * 0.30f);

        // ---- ② 主干：三次不同宽度叠出「有截面的茎」 ----
        // 先采样一遍点，三层共用同一组点，避免三次重算
        for (int i = 0; i <= SEG; ++i) {
            float t = i / (float) SEG;
            // 横向弯曲：越往上偏得越多（lean 决定方向），再叠一个摆动
            float bend = (lean * 0.42f + sway) * t * t;
            PATH_X[i] = cx + bend * height * 0.34f;
            PATH_Y[i] = baseY + h * t;
        }

        // 三层：宽软层（光晕）/ 主体层 / 窄亮层（受光面）
        // 越往上越亮 —— 顶端是生长点，根部偏暗，读作「从暗处长出来」
        stroke(sb, thick * 2.4f, a * 0.14f, EvolveFx.DEEP);
        stroke(sb, thick, a * 0.72f, EvolveFx.BIO);
        stroke(sb, Math.max(px(4.5f), thick * 0.36f), a * 0.80f, EvolveFx.CORE);

        // ---- ③ 分叉 + ④ 尖端光苞 ----
        float tipX = PATH_X[SEG];
        float tipY = PATH_Y[SEG];
        float fk = MathUtils.clamp(fork, 0.0f, 1.0f);

        if (fk > 0.02f && g > 0.55f) {
            // 左右各一支小叉，随 fork 张开，并随 sway 一起摆
            float branchLen = height * 0.30f * fk;
            for (int side = -1; side <= 1; side += 2) {
                // 张角：从几乎并在一起张开到 ±约 38°
                float ang = (float) side * (0.66f * fk);
                float bx = tipX + MathUtils.sin(ang) * branchLen
                        + sway * height * 0.10f;
                float by = tipY + MathUtils.cos(ang) * branchLen;
                Draw.line(sb, tipX, tipY, bx, by,
                        Math.max(px(FORK_MIN), thick * 0.62f),
                        EvolveFx.BIO, a * 0.72f * fk);
                Draw.line(sb, tipX, tipY, bx, by,
                        Math.max(px(4.5f), thick * 0.24f),
                        EvolveFx.CORE, a * 0.62f * fk);
                // 叉尖的小光苞
                float br = Math.max(px(5.0f), thick * 0.34f);
                Draw.glow(sb, bx, by, br * 3.0f, br * 3.0f, 0.0f,
                        EvolveFx.BIO, a * 0.34f * fk);
                Draw.glow(sb, bx, by, br * 1.1f, br * 1.1f, 0.0f,
                        EvolveFx.CORE, a * 0.70f * fk);
            }
        }

        // 主尖端的最大光苞：读作「还在往外顶」
        float tipR = Math.max(px(7.0f), thick * 0.44f);
        Draw.glow(sb, tipX, tipY, tipR * 3.4f, tipR * 3.4f, 0.0f,
                EvolveFx.BIO, a * 0.30f);
        Draw.glow(sb, tipX, tipY, tipR * 1.5f, tipR * 1.5f, 0.0f,
                EvolveFx.BIO, a * 0.66f);
        Draw.glow(sb, tipX, tipY, tipR * 0.7f, tipR * 0.7f, 0.0f,
                EvolveFx.CORE, a * 0.88f);
    }

    /**
     * 沿 {@link #PATH_X} / {@link #PATH_Y} 画一条有厚度的光带。
     * <p>
     * 逐段用 {@link Draw#line}：底层已兜最小厚度，
     * 且相邻段共用端点，接缝天然连续。
     * 亮度沿高度从 0.55 爬到 1.0，让茎有「根部暗、顶上亮」的方向感。
     */
    private static void stroke(SpriteBatch sb, float thick, float alpha, Color color) {
        if (alpha <= 0.004f) {
            return;
        }
        for (int i = 1; i < PATH_X.length; ++i) {
            float t = i / (float) (PATH_X.length - 1);
            float lift = MathUtils.lerp(0.55f, 1.0f, t);
            Draw.line(sb, PATH_X[i - 1], PATH_Y[i - 1], PATH_X[i], PATH_Y[i],
                    thick, color, alpha * lift);
        }
    }

    /**
     * 「被吃进去」的弧线：从那张状态牌的位置向上汇进芽尖。
     * <p>
     * 这是因果链的中段 —— 没有它，「脏牌」和「芽长了」两件事就断开了。
     *
     * @param q 0~1，流动进度
     */
    static void absorb(SpriteBatch sb, float fromX, float fromY,
                       float toX, float toY, float alpha, float q) {
        if (alpha <= 0.004f || q <= 0.0f || q >= 1.0f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        // 末端斜率为 0：余弦淡出，不硬切
        float fade = cosineOut(q);
        // 控制点：中点向下压一点，让弧线先往下再扬上去，读作「被吸走」
        float ctrlX = (fromX + toX) * 0.5f;
        float ctrlY = Math.min(fromY, toY) - Math.abs(fromY - toY) * 0.22f;

        // 一串光点沿弧线跑，头在前
        int dots = 7;
        for (int i = 0; i < dots; ++i) {
            float t = MathUtils.clamp(q * 1.2f - i * 0.07f, 0.0f, 1.0f);
            if (t <= 0.0f) {
                continue;
            }
            float u = 1.0f - t;
            float sx = u * u * fromX + 2.0f * u * t * ctrlX + t * t * toX;
            float sy = u * u * fromY + 2.0f * u * t * ctrlY + t * t * toY;
            float head = 1.0f - i * 0.11f;
            float s = px(6.0f) * head;
            Draw.glow(sb, sx, sy, s * 3.0f, s * 3.0f, 0.0f,
                    EvolveFx.BIO, a * fade * 0.26f * head);
            Draw.glow(sb, sx, sy, s * 1.2f, s * 1.2f, 0.0f,
                    EvolveFx.CORE, a * fade * 0.66f * head);
        }
    }

    /**
     * 那张状态牌自己的「被标记」闪动：一圈绿弧箍住它。
     * <p>
     * 这一笔让玩家知道<b>是哪一张</b>触发了。它是整条因果链的起点，
     * 没有它，后面的吸收弧就不知道从哪来。
     * <p>
     * 半径基准由调用方按<b>牌此刻的宽度</b>算好传入，所以牌飞向手牌、
     * 随 {@code drawScale} 长大时，圈会同步长大（不会出现环比牌大十倍）。
     *
     * @param size 半径基准（像素，已含 scale）
     * @param q    0~1，闪动进度
     */
    static void markCard(SpriteBatch sb, float cx, float cy, float size,
                         float alpha, float q) {
        if (alpha <= 0.004f || q <= 0.0f || q >= 1.0f || size <= 1.0f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        float fade = cosineOut(q);
        // 先快速箍紧再松开
        float grow = 1.0f - (1.0f - q) * (1.0f - q);
        float r = Math.max(px(18.0f), size) * (0.72f + 0.36f * grow);

        Draw.arc(sb, cx, cy, r, r * 0.80f, 0.0f, 360.0f,
                Math.max(px(7.0f), r * 0.18f), EvolveFx.BIO, a * 0.85f * fade, 26);
        Draw.arc(sb, cx, cy, r * 0.92f, r * 0.74f, 0.0f, 360.0f,
                Math.max(px(4.5f), r * 0.08f), EvolveFx.CORE, a * 0.60f * fade, 22);
        Draw.glow(sb, cx, cy, r * 1.6f, r * 1.3f, 0.0f, EvolveFx.BIO, a * 0.24f * fade);
    }

    /**
     * 抛向抽牌堆的光苞：读作「换来的额外抽牌」。
     *
     * @param q 0~1，飞行进度
     */
    static void tossed(SpriteBatch sb, float fromX, float fromY,
                       float toX, float toY, float alpha, float q, float spin) {
        if (alpha <= 0.004f || q <= 0.0f || q >= 1.0f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        float fade = cosineOut(q);
        // pow2Out：被抛出去后减速，读作「飞走了」
        float e = 1.0f - (1.0f - q) * (1.0f - q);
        float sx = MathUtils.lerp(fromX, toX, e);
        // 抛物线：先扬后落
        float sy = MathUtils.lerp(fromY, toY, e)
                - MathUtils.sin(e * MathUtils.PI) * Math.abs(toY - fromY) * 0.18f;

        float s = px(7.0f) * (1.0f - 0.30f * q);
        // 光苞：外光 + 内芯，加一枚自转的漂移高光，让它看起来是「一颗」而不是一团
        Draw.glow(sb, sx, sy, s * 3.4f, s * 3.4f, 0.0f,
                EvolveFx.BIO, a * fade * 0.30f);
        Draw.glow(sb, sx, sy, s * 1.5f, s * 1.5f, 0.0f,
                EvolveFx.BIO, a * fade * 0.62f);
        Draw.glow(sb, sx, sy, s * 0.7f, s * 0.7f, 0.0f,
                EvolveFx.CORE, a * fade * 0.86f);
        float hx = sx + MathUtils.cosDeg(spin) * s * 0.40f;
        float hy = sy + MathUtils.sinDeg(spin) * s * 0.40f;
        Draw.glow(sb, hx, hy, s * 0.55f, s * 0.55f, 0.0f,
                EvolveFx.CORE, a * fade * 0.50f);
    }

    /** 余弦淡出：0 → 1 时输出 1 → 0，末端斜率为 0。 */
    static float cosineOut(float q) {
        return MathUtils.cos(MathUtils.clamp(q, 0.0f, 1.0f) * MathUtils.PI * 0.5f);
    }
}
