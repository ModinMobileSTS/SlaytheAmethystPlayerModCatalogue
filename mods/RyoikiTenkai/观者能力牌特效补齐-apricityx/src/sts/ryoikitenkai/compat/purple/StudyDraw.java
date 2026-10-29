package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;

/**
 * 「研习」的画笔：<b>会翻的纸页</b>。
 * <p>
 * <h2>为什么单独写一套</h2>
 * 纸是本补丁里<b>全新的材质</b>。已有的画笔都画「发光的东西」
 * （环、框、石碑的亮面、能量球），而纸是<b>不透明薄片</b>，它的关键特征是：
 * <ul>
 *   <li><b>有正面和背面</b> —— 翻动时朝向会变，从亮面变成暗面</li>
 *   <li><b>有边缘</b> —— 薄片侧对着光时收成一条边，而不是消失</li>
 *   <li><b>有折角</b> —— 纸角翘起是「这是一张纸」最省力的信号</li>
 * </ul>
 * 这三件事都不是现成画笔能表达的，所以抽出来。
 * <h2>翻页怎么画</h2>
 * 不做真正的 3D 旋转（纸很薄，旋转矩阵的收益小于成本），
 * 用<b>横向压缩 + 明暗互换</b>足够：
 * <pre>
 *   flip = 0   → 完全正面，宽度 × 1.00，亮面
 *   flip = 0.5 → 侧对屏幕，宽度 × 0.18，最暗（正在过边缘）
 *   flip = 1   → 完全背面，宽度 × 1.00，比正面暗一档
 * </pre>
 * 宽度用 {@code |cos|} 压缩（薄片绕竖轴转的正弦投影），亮度按收窄程度插值。
 * <h2>禁止项</h2>
 * <ul>
 *   <li><b>不用 {@code Draw.star}</b> —— 星芒是原版卡面风格，混进战斗特效会读成装饰。
 *       旧实现 {@code Motif.RUNE} 在橙刺尖端用了它，这是必须先修掉的问题</li>
 *   <li><b>不画细线</b> —— 页边用 {@link Draw#line}，底层已兜 4.5px；
 *       本类再把下限抬到 {@code 5px × scale}</li>
 *   <li><b>不画文字/符文具象内容</b> —— 只在页面上留几道<b>抽象横槽</b>，
 *       读作「写过字」，不画任何可辨认的字符</li>
 * </ul>
 * <h2>所有固定像素都乘 {@code Settings.scale}</h2>
 * 这是虔信那轮查出来的坑：{@code Settings.scale} = 实际分辨率 ÷ 1920，
 * 裸写像素在真机上会整体缩水。
 */
final class StudyDraw {
    /** 页边最小厚度（基准像素）。 */
    private static final float EDGE_MIN = 5.0f;
    /** 页面横槽的最小厚度（基准像素）。 */
    private static final float SLOT_MIN = 4.0f;

    private StudyDraw() {
    }

    private static float px(float base) {
        return base * Settings.scale;
    }

    /**
     * 一页纸。
     *
     * @param cx,cy  页心
     * @param w,h    页的完整宽高（flip 会在内部压缩宽度）
     * @param tilt   页自身的倾角（度）
     * @param flip   0~1 翻页相位；0 = 正面，0.5 = 侧对屏幕，1 = 背面
     * @param alpha  不透明度
     */
    static void page(SpriteBatch sb, float cx, float cy, float w, float h,
                     float tilt, float flip, float alpha) {
        if (alpha <= 0.004f || w <= 2.0f || h <= 2.0f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        float f = MathUtils.clamp(flip, 0.0f, 1.0f);

        // 横向压缩：flip 0 和 1 时最宽，0.5 时收到最窄
        // 用 |cos| 而不是线性 —— 这是真实的「薄片绕竖轴转」投影
        float squeeze = Math.abs(MathUtils.cos(f * MathUtils.PI));
        float squash = MathUtils.lerp(0.18f, 1.0f, squeeze);
        // 侧对屏幕时最暗：那时光几乎扫不到页面
        float faceLit = MathUtils.lerp(0.35f, 1.0f, squeeze);
        // 背面比正面暗一档：读作「翻到了另一面」
        float backSide = f > 0.5f ? 0.72f : 1.0f;

        float rad = tilt * MathUtils.degreesToRadians;
        float cs = MathUtils.cos(rad);
        float sn = MathUtils.sin(rad);

        // 沿页自身横轴的半宽（已压缩）与纵轴半高
        float hw = w * squash * 0.5f;
        float hh = h * 0.5f;
        float lit = faceLit * backSide;
        float thick = Math.max(px(EDGE_MIN), w * squash * 0.055f);

        // 取独立副本：Draw 内部用的是共享 Color，不能跨调用持有
        Color paper = new Color(StudyFx.PAPER);
        paper.mul(lit, lit, lit, 1.0f);
        Color ink = new Color(StudyFx.INK);
        ink.mul(lit, lit, lit, 1.0f);

        // ---- 1. 外光：让纸在暗背景上「有存在感」 ----
        // 不能太强，否则会读成发光体而不是纸
        Draw.glow(sb, cx, cy, w * squash * 2.4f, h * 2.4f, tilt,
                ink, a * 0.10f * lit);

        // ---- 2. 页体：实心面 ----
        // 用三层不同尺寸叠出「有厚度、边缘发虚」的纸面（加色混合里没有阴影）
        Draw.glow(sb, cx, cy, w * squash * 1.02f, h * 1.02f, tilt,
                paper, a * 0.62f * lit);
        Draw.glow(sb, cx, cy, w * squash * 0.94f, h * 0.94f, tilt,
                paper, a * 0.72f * lit);
        Draw.glow(sb, cx, cy, w * squash * 0.80f, h * 0.86f, tilt,
                ink, a * 0.16f * lit);

        // ---- 3. 页边：薄片的轮廓 ----
        // 侧对屏幕时宽度已经很窄，这时只靠一条竖边读作「一张纸侧对着我」
        if (squash > 0.22f) {
            float[] qx = new float[4];
            float[] qy = new float[4];
            corner(qx, qy, 0, cx, cy, -hw, -hh, cs, sn);
            corner(qx, qy, 1, cx, cy, hw, -hh, cs, sn);
            corner(qx, qy, 2, cx, cy, hw, hh, cs, sn);
            corner(qx, qy, 3, cx, cy, -hw, hh, cs, sn);
            for (int i = 0; i < 4; ++i) {
                int j = (i + 1) & 3;
                Draw.line(sb, qx[i], qy[i], qx[j], qy[j], thick, paper,
                        a * 0.85f * lit);
            }
        } else {
            // 完全侧对：画一条竖边（沿页自身纵轴），读作「纸片的侧面」
            Draw.line(sb, cx - hh * sn, cy - hh * cs, cx + hh * sn, cy + hh * cs,
                    thick, paper, a * 0.90f * lit);
        }

        // ---- 4. 抽象横槽：读作「上面写过东西」 ----
        // 只在纸面够宽时才画，否则会糊成一团
        if (squash > 0.55f) {
            float slotH = Math.max(px(SLOT_MIN), h * 0.035f);
            for (int i = 0; i < 4; ++i) {
                // 从上往下排，越靠下越短，读作「段落收尾」
                float ly = h * (0.28f - i * 0.17f);
                float halfLen = hw * (0.62f - i * 0.11f);
                if (halfLen <= 1.0f) {
                    continue;
                }
                Draw.line(sb,
                        cx - halfLen * cs - ly * sn,
                        cy - halfLen * sn + ly * cs,
                        cx + halfLen * cs - ly * sn,
                        cy + halfLen * sn + ly * cs,
                        slotH, ink, a * 0.42f * lit);
            }
        }

        // ---- 5. 折角：纸角翘起，是最省力的「这是纸」信号 ----
        // 只在页面接近正对时画；侧对时看不到折角
        if (squash > 0.70f) {
            float fold = Math.max(px(9.0f), w * squash * 0.20f);
            // 右上角：从「角沿边内缩 fold」到「角」画一道斜线
            float tipX = cx + (hw - fold) * cs - hh * sn;
            float tipY = cy + (hw - fold) * sn + hh * cs;
            float cornerX = cx + hw * cs - hh * sn;
            float cornerY = cy + hw * sn + hh * cs;
            Draw.line(sb, tipX, tipY, cornerX, cornerY,
                    Math.max(px(4.5f), thick * 0.8f), ink, a * 0.62f * lit);
        }
    }

    private static void corner(float[] qx, float[] qy, int i,
                               float cx, float cy, float lx, float ly,
                               float cs, float sn) {
        qx[i] = cx + lx * cs - ly * sn;
        qy[i] = cy + lx * sn + ly * cs;
    }

    /**
     * 落点的「归档涟漪」：抽牌堆图标上扩一圈粗弧。
     * <p>
     * 这一笔是因果链的收尾 —— 没有它，页落下去就只是「消失了」；
     * 有它，才读作「被收进去了」。
     *
     * @param q 0~1，扩张进度
     */
    static void settle(SpriteBatch sb, float cx, float cy, float base,
                       float q, float alpha) {
        if (alpha <= 0.004f || q <= 0.0f || q >= 1.0f || base <= 1.0f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        float grow = 1.0f - (1.0f - q) * (1.0f - q);
        float r = base * (0.35f + 0.95f * grow);
        // 末端斜率为 0：余弦淡出，不硬切
        float fade = FastingDraw.cosineOut(q);
        float thick = Math.max(px(6.0f), base * 0.20f) * (1.0f - 0.40f * q);

        Draw.arc(sb, cx, cy, r, r, 0.0f, 360.0f, thick,
                StudyFx.INK, a * 0.80f * fade, 30);
        Draw.arc(sb, cx, cy, r * 0.96f, r * 0.96f, 0.0f, 360.0f,
                Math.max(px(4.5f), thick * 0.42f), StudyFx.CORE,
                a * 0.55f * fade, 26);
        Draw.glow(sb, cx, cy, r * 2.2f, r * 2.2f, 0.0f, StudyFx.INK,
                a * 0.16f * fade);
    }
}
