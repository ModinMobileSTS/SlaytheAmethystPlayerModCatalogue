package sts.ryoikitenkai.compat.green;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;

/**
 * 「精准」的<b>曲线供能连线 + 卡牌附魔边框</b>。
 * <h2>连线：本轮彻底重做（上一版是最大败笔）</h2>
 * 上一版的问题看截图就清楚：几条粗米褐色带子横穿半个屏幕、压过角色与血量条，
 * 读作「电缆 / 绳索」，完全没有能量感。三个根因：
 * <ol>
 *   <li><b>被画笔下限撑粗了</b>：{@code GreenDraw.stroke} 里有
 *       {@code MIN_LINE = 7px × scale} 的强下限，而它内部还是
 *       「外晕 + 内芯 + 高光」三层叠加 —— 画长线时视觉宽度接近 20px。
 *       下限是为了让<b>短刻线</b>在手机上看得见，不适合<b>跨屏长线</b></li>
 *   <li><b>几乎不弯</b>：控制点贴着连线放，所以是「斜杠」而不是「曲线」</li>
 *   <li><b>颜色偏脏</b>：暗金 {@code (0.62,0.50,0.32)} 在暗场景里加色混合后是土褐色</li>
 * </ol>
 * 现在改为：
 * <ul>
 *   <li><b>不走下限</b>：本类自带 {@link #thinQuad}，直接用纯白方片画 1.6~2.6px 的细线，
 *       并随距离<b>两端渐隐</b> —— 线在卡牌和标靶附近最亮，中间几乎看不见</li>
 *   <li><b>强烈弯曲</b>：控制点抬到连线的一侧，做出明显的弧形垂挂感</li>
 *   <li><b>暖白</b>：{@link AccuracyFx#LINK} 取接近白的暖色，加色混合后是「一道细光」而不是褐带</li>
 *   <li><b>封顶 3 条</b>：手牌里小刀再多也只连 3 张，避免满屏拉线</li>
 * </ul>
 * <h2>卡牌边框：只画四条边</h2>
 * 不铺整张卡（{@code CARD_*_BG_SILHOUETTE} 是实心填充，铺它等于给整张牌染色）。
 * 四角由 {@link CardFit} 的剪影尺寸算出、按 {@code card.angle} 旋转，所以永远贴合。
 */
final class AccuracyLinkDraw {
    /** 曲线采样点数。 */
    private static final int CURVE_SEG = 40;
    /** 卡框角括的长度比例（相对半边）。 */
    private static final float CORNER_LEN = 0.26f;
    /** 四条边的线宽（相对卡宽）。 */
    private static final float EDGE_THICK = 0.022f;
    /** 连线的基准线宽（像素，未乘 scale）—— 刻意低于画笔下限。 */
    private static final float LINK_THIN = 2.4f;
    /** 连线中点的弯曲量（相对连线长度的垂直偏移）。 */
    private static final float LINK_BOW = 0.16f;

    private static final TextureRegion WHITE =
            new TextureRegion(ImageMaster.WHITE_SQUARE_IMG);
    private static final TextureAtlas.AtlasRegion GLOW = ImageMaster.GLOW_SPARK_2;

    private static final float[] CURVE_X = new float[CURVE_SEG + 1];
    private static final float[] CURVE_Y = new float[CURVE_SEG + 1];
    private static final float[] EDGE = new float[2];
    private static final float[] NORMAL = new float[2];
    /** 四个角的世界坐标暂存。 */
    private static final float[] CX = new float[4];
    private static final float[] CY = new float[4];
    private static final Color SCRATCH = new Color();

    AccuracyLinkDraw() {
    }

    /**
     * <b>不走画笔下限</b>的细线：两点之间的一段纯白方片，可任意旋转。
     * <p>
     * 画笔的 {@code MIN_LINE} 是为了让短刻线可见，对跨屏长线过粗；
     * 这里直接画 1~3px，配合两端渐隐，读作「一道细光」。
     */
    private static void thinQuad(SpriteBatch sb, float x1, float y1, float x2, float y2,
                                float thick, Color color, float alpha) {
        if (alpha <= 0.004f) {
            return;
        }
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len <= 0.3f) {
            return;
        }
        float rot = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        float mx = (x1 + x2) * 0.5f;
        float my = (y1 + y2) * 0.5f;
        float l = len * 1.05f;
        SCRATCH.set(color);
        SCRATCH.a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        sb.setColor(SCRATCH);
        sb.draw(WHITE, mx - l * 0.5f, my - thick * 0.5f,
                l * 0.5f, thick * 0.5f, l, thick, 1.0f, 1.0f, rot);
    }

    /** 小幅柔光点（不走下限，用来给连线中段加点「流动感」）。 */
    private static void dot(SpriteBatch sb, float x, float y, float size,
                            Color color, float alpha) {
        if (alpha <= 0.004f || size <= 0.3f) {
            return;
        }
        SCRATCH.set(color);
        SCRATCH.a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        sb.setColor(SCRATCH);
        sb.draw(GLOW, x - size * 0.5f, y - size * 0.5f, size, size);
    }

    /**
     * 画一条从标靶到卡牌的<b>细曲线</b>连线。
     * <p>
     * 线宽只有 1.6~2.6px，并且<b>两端亮、中间极淡</b> ——
     * 所以它不会像上一版那样变成横穿屏幕的粗带子。
     *
     * @param fromX,fromY 标靶上的出发点
     * @param card        目标卡牌
     * @param unit        标靶半径
     * @param phase       0~1，流动光点在曲线上的位置
     */
    static void link(SpriteBatch sb, float fromX, float fromY, AbstractCard card,
                     float unit, float alpha, float phase, Color charm) {
        if (card == null || alpha <= 0.004f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);

        // 终点 = 卡牌边缘上离标靶最近的那条边的法向落点
        CardFit.nearestEdgeNormal(card, fromX, fromY, NORMAL);
        CardFit.edgePoint(card, NORMAL[0], NORMAL[1], EDGE);
        float toX = EDGE[0];
        float toY = EDGE[1];

        float dx = toX - fromX;
        float dy = toY - fromY;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 20.0f) {
            return;
        }
        float ux = dx / len;
        float uy = dy / len;
        // 连线的法向：控制点沿它抬出去，做出明显的弧
        float nx = -uy;
        float ny = ux;

        // 起点控制点：沿行进方向伸出 1/4
        float c1x = fromX + ux * len * 0.26f;
        float c1y = fromY + uy * len * 0.26f;
        // 终点控制点：沿卡牌边法向反向退出，做「垂直切入」
        float back = Math.min(len * 0.26f, unit * 2.0f);
        float c2x = toX + NORMAL[0] * back;
        float c2y = toY + NORMAL[1] * back;
        // 中点：沿法向抬起，明显弯曲（下垂感：统一往屏幕下方偏）
        float bow = len * LINK_BOW * (ny >= 0.0f ? 1.0f : -1.0f);
        c1x += nx * bow * 0.6f;
        c1y += ny * bow * 0.6f;
        c2x += nx * bow * 0.6f;
        c2y += ny * bow * 0.6f;

        for (int i = 0; i <= CURVE_SEG; ++i) {
            float t = i / (float) CURVE_SEG;
            float u = 1.0f - t;
            CURVE_X[i] = u * u * u * fromX + 3.0f * u * u * t * c1x
                    + 3.0f * u * t * t * c2x + t * t * t * toX;
            CURVE_Y[i] = u * u * u * fromY + 3.0f * u * u * t * c1y
                    + 3.0f * u * t * t * c2y + t * t * t * toY;
        }

        // ---- 细线本体：两端亮、中间淡（sin 包络，两端为 0）----
        float base = Math.max(LINK_THIN * Settings.scale, unit * 0.045f);
        for (int i = 1; i <= CURVE_SEG; ++i) {
            float t = i / (float) CURVE_SEG;
            // 两端 0.9、中间 0.22：视觉上只有两端可见，中间几乎消失
            float env = 0.22f + 0.68f * Math.abs(2.0f * t - 1.0f);
            float th = base * (0.7f + 0.5f * env);
            thinQuad(sb, CURVE_X[i - 1], CURVE_Y[i - 1], CURVE_X[i], CURVE_Y[i],
                    th, charm, a * 0.55f * env);
        }

        // ---- 两端各一小团光：读作「从这里抽出来、接到这里」----
        float ns = Math.max(2.6f * Settings.scale, unit * 0.16f);
        dot(sb, fromX, fromY, ns * 2.4f, charm, a * 0.22f);
        dot(sb, fromX, fromY, ns * 1.1f, charm, a * 0.45f);
        dot(sb, toX, toY, ns * 2.0f, charm, a * 0.20f);
        dot(sb, toX, toY, ns * 0.95f, charm, a * 0.42f);

        // ---- 流动光点：一颗小点沿曲线走 ----
        int idx = MathUtils.clamp((int) (MathUtils.clamp(phase, 0.0f, 1.0f) * CURVE_SEG),
                1, CURVE_SEG - 1);
        float ps = Math.max(2.4f * Settings.scale, unit * 0.14f);
        // 中段的光点本身要淡一些，否则又变成「一串珠子」
        float mid = 0.55f;
        dot(sb, CURVE_X[idx], CURVE_Y[idx], ps * 2.2f, charm, a * 0.18f * mid);
        dot(sb, CURVE_X[idx], CURVE_Y[idx], ps, charm, a * 0.50f * mid);
    }

    /**
     * <b>卡牌附魔边框</b>：只在卡牌边缘画四条细线 + 四角短括，<b>牌面内部一个像素都不画</b>。
     * <p>
     * 四个角由 {@link CardFit} 的半宽 / 半高算出，再按 {@code card.angle} 绕卡心旋转，
     * 所以卡牌移动、缩放、旋转时边框都严丝合缝。
     */
    void frame(SpriteBatch sb, AbstractCard card, float alpha, float time,
               Color charm) {
        if (card == null || alpha <= 0.004f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        float hw = CardFit.frameHalfW(card);
        float hh = CardFit.frameHalfH(card);
        if (hw <= 1.0f || hh <= 1.0f) {
            return;
        }

        // 附魔呼吸：不同卡牌错相（用卡牌哈希做相位）
        float phase = (time * 0.45f + (card.hashCode() & 0xFF) * 0.0039f) % 1.0f;
        float pulse = 0.84f + 0.16f * MathUtils.sin(phase * MathUtils.PI2);
        float aa = a * pulse;

        // ---- 四个角：按卡牌角度旋转 ----
        float cx = card.current_x;
        float cy = card.current_y;
        float rot = card.angle * MathUtils.degreesToRadians;
        float cos = MathUtils.cos(rot);
        float sin = MathUtils.sin(rot);
        for (int i = 0; i < 4; ++i) {
            float lx = (i == 0 || i == 3) ? hw : -hw;
            float ly = (i < 2) ? hh : -hh;
            CX[i] = cx + lx * cos - ly * sin;
            CY[i] = cy + lx * sin + ly * cos;
        }

        float th = Math.max(GreenDraw.MIN_LINE * 0.7f, hw * EDGE_THICK);
        // ---- 四条边：细线（牌面内部不画任何东西）----
        for (int i = 0; i < 4; ++i) {
            int j = (i + 1) % 4;
            GreenDraw.quad(sb, CX[i], CY[i], CX[j], CY[j], th, charm, aa * 0.34f);
        }

        // ---- 四角短括：加粗一小段，给边框一点「刻印」的细节 ----
        float bth = Math.max(GreenDraw.MIN_LINE * 0.85f, hw * 0.045f);
        float kx = hw * CORNER_LEN;
        float ky = hh * CORNER_LEN;
        for (int i = 0; i < 4; ++i) {
            float sx = (i == 0 || i == 3) ? -1.0f : 1.0f;
            float sy = (i < 2) ? -1.0f : 1.0f;
            float hx = CX[i] + (sx * kx) * cos;
            float hy = CY[i] + (sx * kx) * sin;
            GreenDraw.quad(sb, CX[i], CY[i], hx, hy, bth, charm, aa * 0.55f);
            float vx = CX[i] - (sy * ky) * sin;
            float vy = CY[i] + (sy * ky) * cos;
            GreenDraw.quad(sb, CX[i], CY[i], vx, vy, bth, charm, aa * 0.55f);
        }
    }
}
