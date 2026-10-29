package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.combat.FastingEffect;

/**
 * 「斋戒」的画笔：<b>真实能量球</b>、<b>球体残影</b>，以及<b>原版光圈</b>的转发。
 * <p>
 * <h2>光圈直接用原版 {@link FastingEffect}（本轮改动）</h2>
 * 之前自绘了「红圈 / 绿圈」的双层环。现在改为<b>完全复用原版</b>
 * 斋戒施放时播放的那个光圈：
 * <pre>
 * queue(new FastingEffect(x, y, color));
 * </pre>
 * 这样有三个好处：
 * <ol>
 *   <li><b>与原版完全一致</b> —— 玩家对这张牌既有认知不会被改写</li>
 *   <li><b>不用自己调包络</b> —— 原版的曲线（{@code swingOut} 弹出 +
 *       {@code fade} 前后半段交替）已经调好，天然不会「消失突兀」</li>
 *   <li><b>少一套自绘代码</b> —— 不再有 `ringEnvelope` / `ringSpread` /
 *       `ringFill` 那套时间轴，也就没有「时间轴错位」这类 bug</li>
 * </ol>
 * 原版行为（已对照字节码）：
 * <pre>
 *   时长 1.0s，贴图 WHITE_RING，初始 alpha 0
 *   0.5–1.0s（前半）：alpha = fade(0.45 → 0)
 *   0.0–0.5s（后半）：alpha = fade(0 → 0.45)，
 *                      scale 用 swingOut 从 0 弹到 3.0
 *   自旋 -205°/s，并同时画一份旋转 +180° 的副本 → 视觉上是「双环交叉」
 * </pre>
 * 注意它的 alpha 上限是 <b>0.45</b>（不是 1.0），所以叠在角色身上刚好是
 * 「有效果但不盖住立绘」的强度 —— 这也是原版就调好的。
 * <h2>颜色</h2>
 * 原版施放斋戒时传的是 {@code Color.CHARTREUSE}（黄绿）。
 * 这里：
 * <ul>
 *   <li>力量 → {@link #STRENGTH}（红）</li>
 *   <li>敏捷 → {@link #DEXTERITY}（绿）</li>
 * </ul>
 * 与原版同一色系，但两种属性可分。
 * <h2>球仍然是自绘的（必须保留）</h2>
 * 能量球是本补丁独有的表达（原版斋戒没有「能量球飞出」这件事），
 * 所以球体、拖尾残影仍由本类绘制；只有「打在身上的那一下」用原版。
 * <h2>禁止细线条</h2>
 * 本类不含任何 {@code Draw.line}。
 */
final class FastingDraw {
    /** 力量：红。 */
    static final Color STRENGTH = new Color(1.00f, 0.42f, 0.32f, 1.0f);
    /** 敏捷：绿。 */
    static final Color DEXTERITY = new Color(0.46f, 0.96f, 0.48f, 1.0f);
    /** 白热：球体内芯。 */
    static final Color CORE = new Color(1.00f, 0.97f, 0.86f, 1.0f);
    /** 被抽空的能量：暗色残迹。 */
    static final Color DRAINED = new Color(0.34f, 0.30f, 0.22f, 1.0f);

    /** 球贴图相对半径的放大倍数：贴图本身带留白，放大后才填满。 */
    private static final float TEX_SCALE = 2.6f;

    private FastingDraw() {
    }

    // ---------------- 原版光圈转发 ----------------

    /**
     * 在指定位置播放<b>原版斋戒光圈</b>。
     * <p>
     * 这就是原版 {@code Fasting.use()} 里那一句：
     * <pre>
     * addToBot(new VFXAction(new FastingEffect(p.hb.cX, p.hb.cY, Color.CHARTREUSE)));
     * </pre>
     * 原版走的是 {@code VFXAction}（会随动作队列延迟播放），
     * 这里为了和自绘的能量球同步，直接塞进最顶层队列立即播放。
     * <p>
     * 用 {@code queueTop} 是因为光圈落在角色身上，而角色上方可能压着手牌区，
     * 普通层会被手牌盖掉。
     *
     * @param x     光圈中心 X
     * @param y     光圈中心 Y
     * @param color 圈色（力量红 / 敏捷绿）
     */
    static void vanillaRing(float x, float y, Color color) {
        if (color == null) {
            return;
        }
        PurpleEffectRegistry.queueTop(new FastingEffect(x, y, color));
    }

    /** 当前角色的能量贴图；拿不到返回 null（此时走纯光晕降级路径）。 */
    static Texture energyTexture() {
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null) {
            return null;
        }
        try {
            return p.getEnergyImage();
        } catch (Throwable ignored) {
            // 某些自定义角色可能没实现，降级到纯光晕球
            return null;
        }
    }

    /** 余弦淡出：0 → 1 时输出 1 → 0，末端斜率为 0。 */
    static float cosineOut(float q) {
        return MathUtils.cos(MathUtils.clamp(q, 0.0f, 1.0f) * MathUtils.PI * 0.5f);
    }

    // ---------------- 能量球 ----------------

    /**
     * 一颗能量球。
     * <p>
     * 有贴图时：贴图自转 + 三层光晕 + 白热内芯，是有纹理的实体。
     * 无贴图时：退化为一团多层光晕。
     */
    static void orb(SpriteBatch sb, Texture tex, float x, float y, float r,
                    float alpha, float spin, Color tint, Color glow, float breath) {
        if (alpha <= 0.004f || r <= 0.5f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        float r2 = r * (1.0f + 0.08f * breath);

        // ---- 外层光晕：极大极淡，让球「照亮周围」而不是「一块色」 ----
        Draw.glow(sb, x, y, r2 * 7.0f, r2 * 7.0f, 0.0f, glow, a * 0.16f);
        Draw.glow(sb, x, y, r2 * 3.6f, r2 * 3.6f, 0.0f, glow, a * 0.34f);

        // ---- 球体：有贴图就用贴图，这是「像能量球」的关键 ----
        if (tex != null) {
            float size = r2 * TEX_SCALE;
            sb.setColor(Draw.tint(tint, a * 0.95f));
            sb.draw(tex, x - size * 0.5f, y - size * 0.5f,
                    size * 0.5f, size * 0.5f, size, size,
                    1.0f, 1.0f, spin, 0, 0,
                    tex.getWidth(), tex.getHeight(), false, false);
            Draw.glow(sb, x, y, r2 * 2.2f, r2 * 2.2f, 0.0f, glow, a * 0.26f);
        } else {
            Draw.glow(sb, x, y, r2 * 2.4f, r2 * 2.4f, 0.0f, glow, a * 0.72f);
            Draw.glow(sb, x, y, r2 * 1.3f, r2 * 1.3f, 0.0f, glow, a * 0.85f);
        }

        // ---- 白热内芯：读作「核心在烧」 ----
        Draw.glow(sb, x, y, r2 * 1.0f, r2 * 1.0f, 0.0f, CORE, a * 0.55f);

        // ---- 呼吸高光：球在转，高光位置也随之漂移 ----
        float hx = x + MathUtils.cosDeg(spin) * r2 * 0.34f;
        float hy = y + MathUtils.sinDeg(spin) * r2 * 0.34f;
        Draw.glow(sb, hx, hy, r2 * 0.9f, r2 * 0.9f, 0.0f, CORE, a * 0.42f);
    }

    /**
     * 残影：沿轨迹回溯的一串<b>缩小能量球</b>。
     * <p>
     * 为什么不用连线：细线在手机上就是一根发丝，既不像能量也不好看。
     */
    static void afterimage(SpriteBatch sb, Texture tex, float px, float py,
                           float vx, float vy, float r, float alpha,
                           int count, float span, Color glow, float baseSpin) {
        if (alpha <= 0.004f || r <= 0.5f || count <= 0) {
            return;
        }
        float len = (float) Math.sqrt(vx * vx + vy * vy);
        // 速度太低（刚出发或已停下）就不画残影，否则会糊成一团
        if (len <= 0.05f) {
            return;
        }
        float ux = vx / len;
        float uy = vy / len;

        for (int i = count; i >= 1; --i) {
            float f = i / (float) (count + 1);
            float ex = px - ux * span * f;
            float ey = py - uy * span * f;
            float er = r * (1.0f - 0.66f * f);
            float ea = alpha * (1.0f - f) * 0.72f;
            if (ea <= 0.01f || er <= 0.6f) {
                continue;
            }
            if (tex != null) {
                float size = er * TEX_SCALE;
                sb.setColor(Draw.tint(Color.WHITE, ea * 0.85f));
                sb.draw(tex, ex - size * 0.5f, ey - size * 0.5f,
                        size * 0.5f, size * 0.5f, size, size,
                        1.0f, 1.0f, baseSpin - f * 120.0f, 0, 0,
                        tex.getWidth(), tex.getHeight(), false, false);
            }
            Draw.glow(sb, ex, ey, er * 3.4f, er * 3.4f, 0.0f, glow, ea * 0.34f);
            Draw.glow(sb, ex, ey, er * 1.3f, er * 1.3f, 0.0f, glow, ea * 0.48f);
        }
    }

    /**
     * 能量被抽走后，在能量槽位置留下的一圈暗色余痕。
     * <p>
     * 有它，「球是从那里出来的」这件事才成立。
     * 用粗弧（≥ 4.5px）而不是细线。
     */
    static void drainedMark(SpriteBatch sb, float x, float y, float r, float alpha) {
        if (alpha <= 0.004f || r <= 0.5f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        Draw.arc(sb, x, y, r * 2.1f, r * 1.95f, 0.0f, 360.0f,
                Math.max(4.5f, r * 0.42f), DRAINED, a * 0.72f, 26);
        Draw.glow(sb, x, y, r * 3.8f, r * 3.5f, 0.0f, DRAINED, a * 0.26f);
    }

    /** 圆形光点（代替火星／放射线这类细线元素）。 */
    static void spark(SpriteBatch sb, float x, float y, float r,
                      float alpha, Color color) {
        if (alpha <= 0.004f || r <= 0.4f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        Draw.glow(sb, x, y, r * 4.2f, r * 4.2f, 0.0f, color, a * 0.30f);
        Draw.glow(sb, x, y, r * 1.9f, r * 1.9f, 0.0f, color, a * 0.72f);
        Draw.glow(sb, x, y, r * 0.85f, r * 0.85f, 0.0f, CORE, a * 0.62f);
    }
}
