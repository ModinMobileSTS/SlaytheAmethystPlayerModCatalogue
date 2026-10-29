package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;

/**
 * 「先见之明」的画笔：<b>空心光框</b>与<b>显影扫描</b>。
 * <p>
 * <h2>为什么要单独写一套</h2>
 * 观者其它牌的画笔都在画「有内容的东西」：抓痕、石碑、音符、环。
 * 先见之明画的是<b>一个空框</b> —— 它的表达力全在「边框」与「框内是空的」这个对比上，
 * 所以需要的是一个能精确控制<b>四条边 + 四个角 + 内部极淡填充</b>的画笔。
 * <h2>三条硬要求</h2>
 * <ol>
 *   <li><b>没有细线</b>：边框厚度下限 {@code 6px × Settings.scale}，
 *       底层的 {@link Draw#MIN_LINE} = 4.5px 是兜底，这里再抬一档</li>
 *   <li><b>不能只是一圈线</b>：纯线条会读成「一个透明框」，
 *       所以在框内叠一层<b>极淡的填充光</b>，让它有「面」而不只是「边」</li>
 *   <li><b>不能画眼</b>：不画中心竖线、不画瞳孔、不用椭圆拼眼睛
 *       （规范禁止具象物；原实现 {@code FLAME_EYE} 就是在这里出问题的）</li>
 * </ol>
 * <h2>为什么不用 {@link Draw#arc} 拼圆角</h2>
 * 弧在转角处会留下明显的接缝（每段都是一个独立的 {@code THICK_3D_LINE} 四边形），
 * 一个竖长方形的四个角各接一段弧，缝会更显眼。矩形本来就没有圆角，
 * 直接四条边 + 四个角上的小块光斑，读起来更利落。
 * <h2>所有固定像素都乘 {@code Settings.scale}</h2>
 * 这是上一轮（虔信）查出来的坑：本补丁其它特效的尺寸都写了 {@code × Settings.scale}，
 * 而 {@code Settings.scale} = 实际分辨率 ÷ 1920，手机分辨率高时它大于 1。
 * 裸写像素在 2560 宽的屏上会小一圈，看起来「明明写了同样的值却更小」。
 */
final class ForesightDraw {
    /** 边框最小厚度（基准像素）。 */
    private static final float EDGE_MIN = 6.0f;
    /** 边框厚度相对框宽的比例。 */
    private static final float EDGE_RATIO = 0.13f;
    /** 四角光斑相对边厚的倍数。 */
    private static final float CORNER_MUL = 2.1f;

    private ForesightDraw() {
    }

    /** 缩放后的基准像素。 */
    private static float px(float base) {
        return base * Settings.scale;
    }

    /**
     * 一个空心光框。
     * <p>
     * 分四层（规范 §2.4「分层堆叠出质感」）：
     * <ol>
     *   <li>外部大范围体积光：让框「照亮周围」而不是贴在那里</li>
     *   <li>内部极淡填充：给出「面」，避免读成纯线条</li>
     *   <li>四条粗边：主体</li>
     *   <li>四个角光斑：读作「这是一个被钉住的框」，同时盖住边的端点</li>
     * </ol>
     *
     * @param reveal 0~1，显影程度。0 = 未显影（冷暗、边细），1 = 完全显影（冰白、边最粗）
     * @param rotDeg 框自身的倾角（度）
     */
    static void frame(SpriteBatch sb, float cx, float cy, float w, float h,
                      float rotDeg, float alpha, float reveal,
                      Color edge, Color core, Color dormant) {
        if (alpha <= 0.004f || w <= 2.0f || h <= 2.0f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        float rv = MathUtils.clamp(reveal, 0.0f, 1.0f);

        // 边色：未显影用冷暗，显影后插值到冰白
        // 取独立副本，避免被后续绘制覆盖（Draw.tint 返回的是共享对象）
        Color edgeColor = new Color(dormant).lerp(edge, 0.35f + 0.65f * rv);
        float thick = Math.max(px(EDGE_MIN), w * EDGE_RATIO) * (0.78f + 0.55f * rv);

        // 四角的相对坐标（未旋转）
        float hw = w * 0.5f;
        float hh = h * 0.5f;
        float rad = rotDeg * MathUtils.degreesToRadians;
        float cs = MathUtils.cos(rad);
        float sn = MathUtils.sin(rad);

        // ---- 1. 外部体积光 ----
        // 用轴对齐的柔光近似（框很小时旋转对柔光的影响看不出来，省掉一次矩阵运算）
        Draw.glow(sb, cx, cy, w * 2.1f, h * 2.1f, rotDeg, edgeColor,
                a * (0.07f + 0.13f * rv));

        // ---- 2. 内部极淡填充：给出「面」 ----
        // 显影时填充明显变亮，读作「这张牌的内容浮出来了」
        float fillA = a * (0.05f + 0.20f * rv);
        if (fillA > 0.004f) {
            Draw.glow(sb, cx, cy, w * 1.02f, h * 1.02f, rotDeg, edgeColor, fillA * 0.55f);
            Draw.glow(sb, cx, cy, w * 0.78f, h * 0.78f, rotDeg, core, fillA * 0.40f);
        }

        // ---- 3. 四条粗边（旋转后的两个端点） ----
        // 角点在未旋转坐标里的位置，再逐点旋转到世界坐标
        float[] qx = new float[4];
        float[] qy = new float[4];
        corner(qx, qy, 0, cx, cy, -hw, -hh, cs, sn);
        corner(qx, qy, 1, cx, cy, hw, -hh, cs, sn);
        corner(qx, qy, 2, cx, cy, hw, hh, cs, sn);
        corner(qx, qy, 3, cx, cy, -hw, hh, cs, sn);

        float edgeA = a * (0.55f + 0.45f * rv);
        for (int i = 0; i < 4; ++i) {
            int j = (i + 1) & 3;
            Draw.line(sb, qx[i], qy[i], qx[j], qy[j], thick, edgeColor, edgeA);
        }
        // 内侧再压一条更窄更亮的边：夹出「有厚度的框」
        float innerA = a * (0.22f + 0.55f * rv);
        if (innerA > 0.004f) {
            for (int i = 0; i < 4; ++i) {
                int j = (i + 1) & 3;
                Draw.line(sb, qx[i], qy[i], qx[j], qy[j],
                        Math.max(px(4.5f), thick * 0.40f), core, innerA);
            }
        }

        // ---- 4. 四角光斑：盖住端点接缝，并读作「框被钉住了」 ----
        float cr = thick * CORNER_MUL;
        for (int i = 0; i < 4; ++i) {
            Draw.glow(sb, qx[i], qy[i], cr, cr, 0.0f, edgeColor,
                    a * (0.24f + 0.46f * rv));
            Draw.glow(sb, qx[i], qy[i], cr * 0.48f, cr * 0.48f, 0.0f, core,
                    a * (0.16f + 0.60f * rv));
        }
    }

    /** 把未旋转的局部坐标 (lx, ly) 旋转后写到 qx/qy 的第 i 位。 */
    private static void corner(float[] qx, float[] qy, int i,
                               float cx, float cy, float lx, float ly,
                               float cs, float sn) {
        qx[i] = cx + lx * cs - ly * sn;
        qy[i] = cy + lx * sn + ly * cs;
    }

    /**
     * 显影扫描：一道冷白光带自上而下扫过整个框。
     * <p>
     * 这是「占卜」最贴切的一笔 —— 把盖着的牌翻开，光扫过去内容才浮出来。
     * 光带本身用<b>粗横条</b>（{@link Draw#line}），不用细线：
     * 细的扫描线在手机上会变成一条噪点。
     * <p>
     * 光带会溢出框外一点点（宽 1.08 倍），读作「光从框外面扫进来」，
     * 而不是被框裁掉的一段。
     *
     * @param sweep 0~1，光带的纵向位置（0 = 框顶，1 = 框底）
     */
    static void scan(SpriteBatch sb, float cx, float cy, float w, float h,
                     float rotDeg, float sweep, float alpha,
                     Color edge, Color core) {
        if (alpha <= 0.004f || w <= 2.0f || sweep < 0.0f || sweep > 1.0f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        float rad = rotDeg * MathUtils.degreesToRadians;
        float cs = MathUtils.cos(rad);
        float sn = MathUtils.sin(rad);

        // 光带在框内的纵向位置（局部坐标，从 +hh 到 −hh）
        float localY = h * (0.5f - sweep);
        float bandW = w * 1.08f;
        float bandH = Math.max(px(7.0f), h * 0.085f);

        // 光带中心：把局部 (0, localY) 旋转到世界坐标
        float bx = cx - localY * sn;
        float by = cy + localY * cs;
        // 光带方向：沿框的横向（局部 +x 方向旋转后）
        float dx = cs * bandW * 0.5f;
        float dy = sn * bandW * 0.5f;

        // 强度：光带走到框中间最亮（读作「正在被看清」），两端稍弱
        float mid = 1.0f - Math.abs(sweep - 0.5f) * 1.2f;
        float bandA = a * MathUtils.clamp(0.55f + 0.45f * mid, 0.0f, 1.0f);

        // 外层柔光
        Draw.glow(sb, bx, by, bandW * 1.5f, bandH * 3.2f, rotDeg, edge, bandA * 0.26f);
        // 光带本体：粗横条
        Draw.line(sb, bx - dx, by - dy, bx + dx, by + dy, bandH, edge, bandA * 0.85f);
        // 亮芯
        Draw.line(sb, bx - dx * 0.92f, by - dy * 0.92f,
                bx + dx * 0.92f, by + dy * 0.92f,
                Math.max(px(4.5f), bandH * 0.42f), core, bandA * 0.90f);
    }
}
