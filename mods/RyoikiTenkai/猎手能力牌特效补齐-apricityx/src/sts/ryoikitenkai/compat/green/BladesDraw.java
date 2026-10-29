package sts.ryoikitenkai.compat.green;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

/**
 * 「无限刀刃」的画笔：<b>卡面里那种匕首</b>。
 * <p>
 * <h2>五个部件</h2>
 * <pre>
 *    ╱  ← ① 刃尖（楔形收窄到一点，末端亮点）
 *   ╱   ← ② 刃身（冰白，中段一道蓝白裂纹）
 *  ─┼─  ← ③ 护手（深色，横压）
 *   ▮   ← ④ 木柄（暗暖色，两道缠绳）
 *   ●   ← ⑤ 柄头
 * </pre>
 * 少一件就只是「一根线」，读不出是匕首。
 * <h2>刃身：楔形用「均匀实心条」分段，不用 3D 线贴图</h2>
 * 刃身是<b>从根到尖逐渐收窄</b>的楔（等宽线两端一样粗，读作「针」）。
 * 分段拼接时如果继续用 {@code THICK_3D_LINE}，每段都会重复一遍贴图的端部着色，
 * 刃上就会看到一格一格的接缝 —— 所以这里改用
 * {@link GreenDraw#quad}（纯白均匀条），分段重叠后完全无缝。
 * <h2>全部加色混合</h2>
 * 木柄 / 护手的「材质感」靠<b>颜色区分</b>（暗暖 / 暗灰），不靠普通混合。
 */
final class BladesDraw {
    /** 刃身分段数：4 段在刀这个尺度上已看不出台阶。 */
    private static final int WEDGE_STEPS = 4;
    /** 刃根宽度（× 刀长）。 */
    private static final float ROOT_W = 0.28f;
    /** 刃身占全长的比例 / 护手所在位置比例。 */
    private static final float BLADE_SPAN = 0.62f;
    private static final float GUARD_AT = 0.68f;
    /** 柄长 / 护手半宽（× 刀长）。 */
    private static final float HILT_LEN = 0.28f;
    private static final float GUARD_HALF = 0.16f;

    private BladesDraw() {
    }

    /**
     * 画一把匕首。
     *
     * @param cx,cy  刀身<b>中点</b>
     * @param len    刀全长（尖端到柄头）
     * @param dirDeg 刃尖朝向（度，数学方向：0 向右、逆时针增大）
     * @param alpha  不透明度
     * @param glow   额外能量光强度 0~1（施放 / 落地时提高）
     */
    static void dagger(SpriteBatch sb, float cx, float cy, float len, float dirDeg,
                       float alpha, float glow) {
        if (alpha <= 0.004f || len <= 3.0f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        float rad = dirDeg * MathUtils.degreesToRadians;
        float ux = MathUtils.cos(rad);
        float uy = MathUtils.sin(rad);
        // 法向：用于护手与受光 / 背光侧
        float nx = -uy;
        float ny = ux;

        float half = len * 0.5f;
        float tipX = cx + ux * half;
        float tipY = cy + uy * half;

        // 沿轴的几个关键点（自尖端向柄头）
        float bladeLen = len * BLADE_SPAN;
        float rootX = tipX - ux * bladeLen;
        float rootY = tipY - uy * bladeLen;
        float rootW = len * ROOT_W;

        // ---- 整把刀的绿色能量外光：把刀「托」在环境里 ----
        float bodyX = (rootX + tipX) * 0.5f - ux * len * 0.08f;
        float bodyY = (rootY + tipY) * 0.5f - uy * len * 0.08f;
        GreenDraw.glow(sb, bodyX, bodyY, len * 0.86f, len * 0.48f, dirDeg,
                BladesFx.GREEN, a * (0.12f + 0.30f * glow));

        // ---- ① 刃尖：亮点，让尖端在暗背景上「亮出来」 ----
        float gs = Math.max(GreenDraw.MIN_LINE * 1.6f, rootW * (0.80f + 0.90f * glow));
        GreenDraw.glow(sb, tipX, tipY, gs * 2.6f, gs * 2.6f, 0.0f,
                BladesFx.CRACK, a * (0.26f + 0.42f * glow));
        GreenDraw.glow(sb, tipX, tipY, gs * 1.1f, gs * 1.1f, 0.0f,
                BladesFx.CRACK, a * (0.60f + 0.34f * glow));

        // ---- ② 刃身：从根到尖逐渐收窄的楔（均匀条分段，无接缝）----
        wedge(sb, rootX, rootY, tipX, tipY, rootW, rootW * 0.10f,
                BladesFx.BLADE, a * 0.94f);

        // ---- 刃的背光侧：同侧一条更暗的窄线，给厚度 ----
        GreenDraw.line(sb, rootX - nx * rootW * 0.26f, rootY - ny * rootW * 0.26f,
                tipX - nx * rootW * 0.08f, tipY - ny * rootW * 0.08f,
                Math.max(GreenDraw.MIN_LINE, rootW * 0.26f),
                BladesFx.GUARD, a * 0.46f);

        // ---- 刃上裂纹：卡面里刀刃中线那道蓝白裂（中段，折一下才像裂）----
        float c0X = rootX + ux * bladeLen * 0.14f;
        float c0Y = rootY + uy * bladeLen * 0.14f;
        float c1X = rootX + ux * bladeLen * 0.74f;
        float c1Y = rootY + uy * bladeLen * 0.74f;
        float kinkX = (c0X + c1X) * 0.5f + nx * rootW * 0.14f;
        float kinkY = (c0Y + c1Y) * 0.5f + ny * rootW * 0.14f;
        float cth = Math.max(GreenDraw.MIN_LINE, rootW * 0.16f);
        // 裂纹是两段折线，用 quad 避免两段之间的贴图端部台阶
        GreenDraw.quad(sb, c0X, c0Y, kinkX, kinkY, cth,
                BladesFx.CRACK, a * (0.60f + 0.30f * glow));
        GreenDraw.quad(sb, kinkX, kinkY, c1X, c1Y, cth * 0.9f,
                BladesFx.CRACK, a * (0.50f + 0.30f * glow));

        // ---- ③ 护手：横压的深色短线 ----
        float gx = tipX - ux * len * GUARD_AT;
        float gy = tipY - uy * len * GUARD_AT;
        float gh = len * GUARD_HALF;
        GreenDraw.line(sb, gx + nx * gh, gy + ny * gh, gx - nx * gh, gy - ny * gh,
                Math.max(GreenDraw.MIN_LINE * 1.4f, len * 0.075f),
                BladesFx.GUARD, a * 0.90f);

        // ---- ④ 木柄：暗暖色实心条 + 两道缠绳 ----
        float hiltLen = len * HILT_LEN;
        float hx = gx - ux * hiltLen;
        float hy = gy - uy * hiltLen;
        float hiltThick = Math.max(GreenDraw.MIN_LINE * 1.6f, len * 0.13f);
        GreenDraw.line(sb, gx, gy, hx, hy, hiltThick, BladesFx.WOOD, a * 0.92f);
        for (int i = 1; i <= 2; ++i) {
            float t = i / 3.0f;
            float mx = gx - ux * hiltLen * t;
            float my = gy - uy * hiltLen * t;
            GreenDraw.line(sb, mx + nx * hiltThick * 0.46f, my + ny * hiltThick * 0.46f,
                    mx - nx * hiltThick * 0.46f, my - ny * hiltThick * 0.46f,
                    Math.max(GreenDraw.MIN_LINE, hiltThick * 0.34f),
                    BladesFx.GUARD, a * 0.58f);
        }

        // ---- ⑤ 柄头 ----
        float hs = Math.max(GreenDraw.MIN_LINE * 1.2f, len * 0.09f);
        GreenDraw.glow(sb, hx, hy, hs * 2.0f, hs * 2.0f, 0.0f,
                BladesFx.GUARD, a * 0.56f);
    }

    /**
     * 一段<b>楔形</b>实心条：起点宽 {@code w0}、终点宽 {@code w1}。
     * <p>
     * 沿轴分 {@link #WEDGE_STEPS} 段，每段画一条<b>等宽均匀条</b>
     * （{@link GreenDraw#quad}），宽度按段内位置插值、都以轴线为中心。
     * 相邻段中心线连续、宽度线性收窄，整体读作一条渐窄的刃，
     * 而且因为用的是均匀贴图，<b>段间没有任何明暗台阶</b>。
     */
    private static void wedge(SpriteBatch sb, float x0, float y0, float x1, float y1,
                              float w0, float w1, Color color, float alpha) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1.0f) {
            return;
        }
        float ux = dx / len;
        float uy = dy / len;
        for (int i = 0; i < WEDGE_STEPS; ++i) {
            float t0 = i / (float) WEDGE_STEPS;
            float t1 = (i + 1) / (float) WEDGE_STEPS;
            // 段端点多给 12% 重叠，避免段间出现细缝
            float s1 = Math.min(1.0f, t1 * 1.12f);
            float ax = x0 + ux * len * t0;
            float ay = y0 + uy * len * t0;
            float bx = x0 + ux * len * s1;
            float by = y0 + uy * len * s1;
            float w = MathUtils.lerp(w0, w1, (t0 + t1) * 0.5f);
            GreenDraw.quad(sb, ax, ay, bx, by, w, color, alpha);
        }
    }

    /**
     * 落地溅光：匕首砸到地面那一瞬的一小圈横向光。
     * <p>
     * 只在施放段用（一局几次）。它是「落地」这个动作的唯一反馈 ——
     * 没有它，刀就是「凭空出现在地上」。
     *
     * @param q 0~1，溅光扩散进度
     */
    static void impact(SpriteBatch sb, float x, float y, float len, float alpha, float q) {
        if (alpha <= 0.004f || q <= 0.0f || q >= 1.0f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        // 末端斜率为 0：余弦淡出
        float fade = MathUtils.cos(MathUtils.clamp(q, 0.0f, 1.0f) * MathUtils.PI * 0.5f);
        float spread = len * (0.40f + 1.6f * q);
        // 贴地，所以极力压扁
        GreenDraw.glow(sb, x, y, spread * 1.9f, spread * 0.50f, 0.0f,
                BladesFx.GREEN, a * 0.40f * fade);
        GreenDraw.glow(sb, x, y, spread * 0.95f, spread * 0.26f, 0.0f,
                BladesFx.CRACK, a * 0.34f * fade);
    }
}
