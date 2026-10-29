package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 「斋戒」<b>回合开始被扣能量</b>时的演出：
 * <b>能量球从能量槽缓起 → 加速飞到观者 → 原版光圈依次亮起</b>。
 * <p>
 * <h2>光圈复用原版 {@link com.megacrit.cardcrawl.vfx.combat.FastingEffect}</h2>
 * 之前自绘「红圈 / 绿圈」的时间轴一波三折（线性 → 余弦 → 时间轴错位）。
 * 现在<b>直接复用原版斋戒施放时的那一圈</b>：
 * <pre>
 * FastingDraw.vanillaRing(cx, cy, color);
 * </pre>
 * 原版曲线（{@code swingOut} 弹出 + alpha 前后半段交替、时长 1.0s、
 * 自旋并同时画一份旋转 180° 的副本形成「双环交叉」）已经调好，
 * 天然不会「消失突兀」，也就不需要我再维护一套包络与时间轴。
 * <p>
 * 命中后按 <b>0.14s 间隔</b>依次放两圈：
 * <ol>
 *   <li>先红（力量）—— 对应原版先施加 {@code StrengthPower}</li>
 *   <li>后绿（敏捷）—— 对应后施加 {@code DexterityPower}</li>
 * </ol>
 * <h2>时间轴整体缩放</h2>
 * 引入 {@code timeScale}，把<b>所有</b>阶段时长同比缩放，
 * 并由时间轴反推 {@code life}：
 * <pre>
 *   life = impactAt + 原版光圈时长 × timeScale + 错开 + 余量
 * </pre>
 * 这样光圈永远有时间播完，不会被整个特效裁掉。
 * <h2>时间线（标准模式，总长约 2.60s）</h2>
 * <pre>
 *   0.00–0.32  抽出   球在能量槽里成形、旋转、被「拔」出来
 *   0.32–1.35  飞行   沿弧线先慢后快飞向胸口，全程带能量球残影
 *   1.35       命中   白色爆闪 + 红圈（原版光圈）
 *   1.49       第二圈 绿圈（原版光圈）
 *   1.35–2.60  余韵   光点外飞，余弦淡出
 * </pre>
 * <h2>加速过程：两条曲线混合</h2>
 * <pre>
 *   ease = 0.55 × pow2In(t) + 0.45 × pow3In(t)
 * </pre>
 * 前段柔和、后段更猛，比单一曲线更像「被吸过去」。
 * <h2>禁止细线条</h2>
 * 全文没有任何 {@code Draw.line}。
 * <h2>可见性</h2>
 * 走 {@link PurpleEffectRegistry#queueTop}：轨迹从屏幕左下角横穿手牌区。
 * <h2>克制</h2>
 * 每回合都会发生 —— <b>不震屏、不音效</b>。
 */
final class FastingTollEffect extends AbstractGameEffect {
    /** 标准模式下的抽出时长（秒）。实际会乘 timeScale。 */
    private static final float PULL_BASE = 0.32f;
    /** 标准模式下的飞行时长（秒）。实际会乘 timeScale。 */
    private static final float FLY_BASE = 1.03f;
    /** 原版 FastingEffect 的时长（秒），用于反推 life。 */
    private static final float VANILLA_RING_TIME = 1.0f;
    /** 命中之后额外留的余量（秒，标准模式）。 */
    private static final float TAIL_PAD = 0.30f;

    /** 球半径（相对身高）。 */
    private static final float ORB_RATIO = 0.072f;
    /** 残影个数。 */
    private static final int GHOSTS = 7;
    /** 每圈外飞的光点数。 */
    private static final int SPARKS = 14;

    private final float sx;
    private final float sy;
    private final float ex;
    private final float ey;
    /** 弧线控制点：让轨迹向上拱起，不贴着地面飞。 */
    private final float ctrlX;
    private final float ctrlY;
    private final float orbR;
    private final int count;
    private final float life;
    /** 时间整体缩放系数：所有阶段都用它，避免时间轴错位。 */
    private final float timeScale;
    /** 抽出 / 飞行 / 命中（都已乘 timeScale，是绝对的「秒」）。 */
    private final float pullTime;
    private final float flyTime;
    private final float impactAt;
    /** 能量贴图（可能为 null，此时走光晕降级）。 */
    private final Texture tex;
    /** 球的颜色：跟着当前角色的能量配色走。 */
    private final Color glowColor;

    private float time;
    /** 带阻尼的兴奋度：命中瞬间拉满。 */
    private float excite;
    /** 上一帧球心，用来算速度方向。 */
    private float prevX;
    private float prevY;
    private boolean prevValid;
    /** 球的自转角度。 */
    private float spin;
    /** 红圈（力量）是否已放出。 */
    private boolean redFired;
    /** 绿圈（敏捷）是否已放出。 */
    private boolean greenFired;

    private final float[] cur = new float[2];

    FastingTollEffect(AbstractCreature owner, int amount) {
        this.count = MathUtils.clamp(amount, 1, 3);
        // 整体时间缩放：快进模式下压缩到 0.72，但各阶段同比缩放
        this.timeScale = Settings.FAST_MODE ? 0.72f : 1.0f;
        this.pullTime = PULL_BASE * this.timeScale;
        this.flyTime = FLY_BASE * this.timeScale;

        float[] orb = new float[2];
        FastingFx.energyOrbPos(orb);
        this.sx = orb[0];
        this.sy = orb[1];
        this.ex = FastingFx.chestX(owner);
        this.ey = FastingFx.chestY(owner);

        // 弧线：控制点在两点连线中段的上方，拱起高度取水平距离的 26%
        float dx = this.ex - this.sx;
        this.ctrlX = (this.sx + this.ex) * 0.5f + dx * 0.05f;
        this.ctrlY = Math.max(this.sy, this.ey) + Math.abs(dx) * 0.26f;

        this.orbR = Math.max(10.0f * Settings.scale,
                BodyFx.height(owner) * ORB_RATIO);

        // 能量贴图 + 颜色：直接取角色自己的，所以自动跟随能量配色
        this.tex = FastingDraw.energyTexture();
        this.glowColor = FastingFx.energyColor();

        // 命中时刻 = 抽出 + 飞行（光圈在命中时立刻放，不再等最后一颗球）
        this.impactAt = this.pullTime + this.flyTime;
        // 总时长 = 命中 + 原版光圈时长 + 绿圈错开 + 余量（全部同比缩放）
        this.life = this.impactAt
                + VANILLA_RING_TIME * this.timeScale
                + FastingFx.RING_STEP * this.timeScale
                + TAIL_PAD * this.timeScale;
        this.startingDuration = this.duration = this.life;
        // 轨迹穿过手牌区，必须走最顶层
        this.renderBehind = false;
    }

    @Override
    public void update() {
        float dt = Gdx.graphics.getDeltaTime();
        this.time += dt;
        this.duration -= dt;
        // 自转：球一直在转，所以它看起来是「活的」
        this.spin += dt * 150.0f;

        // ---- 命中瞬间：放第一圈（红 / 力量）----
        // 状态变更只在 update 里做，render 保持纯绘制
        if (!this.redFired && this.time >= this.impactAt) {
            this.redFired = true;
            FastingDraw.vanillaRing(this.ex, this.ey, FastingDraw.STRENGTH);
        }
        // ---- 错开 RING_STEP 后：放第二圈（绿 / 敏捷）----
        if (this.redFired && !this.greenFired
                && this.time >= this.impactAt + FastingFx.RING_STEP * this.timeScale) {
            this.greenFired = true;
            FastingDraw.vanillaRing(this.ex, this.ey, FastingDraw.DEXTERITY);
        }

        if (this.duration <= 0.0f) {
            this.isDone = true;
            return;
        }
        // 状态：带阻尼（规范 §三.4）。命中瞬间拉满，之后缓慢回落。
        float target = this.time < this.impactAt + 0.30f * this.timeScale
                ? 1.0f : 0.26f;
        float speed = target > this.excite ? 16.0f : 2.2f;
        this.excite += (target - this.excite) * MathUtils.clamp(dt * speed, 0.0f, 1.0f);
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.duration <= 0.0f) {
            return;
        }
        float ex = MathUtils.clamp(this.excite, 0.0f, 1.0f);
        float punch = 0.85f + 0.35f * ex;
        float breath = 0.5f + 0.5f * MathUtils.sin(this.time * MathUtils.PI2 / 0.9f);

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        // ================= 0. 抽出：能量槽位置留一圈暗色余痕 =================
        if (this.time < this.pullTime + 0.20f * this.timeScale) {
            float out = 1.0f - MathUtils.clamp(
                    (this.time - this.pullTime) / (0.20f * this.timeScale), 0.0f, 1.0f);
            // 证明球是从这里出去的
            FastingDraw.drainedMark(sb, this.sx, this.sy, this.orbR * 1.5f,
                    out * 0.95f);
        }

        // ================= 1. 逐颗：抽出 → 飞行 → 命中 =================
        for (int i = 0; i < this.count; ++i) {
            float start = i * FastingFx.RING_STEP * this.timeScale;
            float t = this.time - start;
            if (t <= 0.0f) {
                continue;
            }

            // ---- 抽出阶段：球在能量槽位置长大、旋转 ----
            if (t < this.pullTime) {
                float q = t / this.pullTime;
                float grow = Interpolation.pow2Out.apply(0.0f, 1.0f, q);
                FastingDraw.orb(sb, this.tex, this.sx, this.sy,
                        this.orbR * (0.30f + 0.70f * grow),
                        0.45f + 0.55f * grow, this.spin, Color.WHITE,
                        this.glowColor, q);
                continue;
            }

            // ---- 飞行阶段：沿弧线「先慢后快」扑向胸口 ----
            float flyQ = MathUtils.clamp((t - this.pullTime) / this.flyTime, 0.0f, 1.0f);
            if (flyQ < 1.0f) {
                // 两条曲线混合加速：前段柔和、后段更猛
                float e = 0.55f * Interpolation.pow2In.apply(0.0f, 1.0f, flyQ)
                        + 0.45f * Interpolation.pow3In.apply(0.0f, 1.0f, flyQ);
                bezier(e, this.cur);
                float px = this.cur[0];
                float py = this.cur[1];

                // 速度（用于残影方向与长度）：由这一帧的位移算出
                float vx = 0.0f;
                float vy = 0.0f;
                if (this.prevValid) {
                    vx = px - this.prevX;
                    vy = py - this.prevY;
                }

                // 残影：一串缩小的能量球（不是细线）
                if (this.prevValid) {
                    float span = this.orbR * (1.2f + 4.6f * e);
                    FastingDraw.afterimage(sb, this.tex, px, py, vx, vy,
                            this.orbR * 0.92f, 0.95f,
                            GHOSTS, span, this.glowColor, this.spin);
                }
                this.prevX = px;
                this.prevY = py;
                this.prevValid = true;

                // 球体：随速度略微放大，读到「越飞越急」
                float grow = 0.92f + 0.30f * e;
                FastingDraw.orb(sb, this.tex, px, py,
                        this.orbR * grow, 1.0f, this.spin + e * 200.0f,
                        Color.WHITE, this.glowColor, breath);

                // 沿途能量碎点（圆形光点，不是线）
                sprinkle(sb, px, py, e, punch);
                continue;
            }

            // ---- 命中之后：白色爆闪 + 该颗颜色的光点 ----
            impactPulse(sb, t - this.pullTime - this.flyTime, i, punch);
        }

        sb.setBlendFunction(src, dst);
    }

    /** 二次贝塞尔取点。 */
    private void bezier(float t, float[] out) {
        float u = 1.0f - t;
        out[0] = u * u * this.sx + 2.0f * u * t * this.ctrlX + t * t * this.ex;
        out[1] = u * u * this.sy + 2.0f * u * t * this.ctrlY + t * t * this.ey;
    }

    /**
     * 沿途撒的碎点。<b>只画圆形光点，不画尾巴</b> —— 细线在手机上就是发丝。
     */
    private void sprinkle(SpriteBatch sb, float px, float py, float e, float punch) {
        for (int k = 0; k < 5; ++k) {
            float f = e - 0.038f * (k + 1);
            if (f <= 0.0f) {
                continue;
            }
            bezier(f, this.cur);
            float ox = MathUtils.sin(f * 17.3f + k * 2.7f) * this.orbR * 1.8f;
            float oy = MathUtils.cos(f * 13.1f + k * 1.9f) * this.orbR * 1.6f;
            float r = this.orbR * (0.40f - 0.055f * k);
            float a = (1.0f - k * 0.17f) * 0.72f;
            FastingDraw.spark(sb, px + ox, py + oy, r, a * punch, this.glowColor);
        }
    }

    /**
     * 单颗命中：白色爆闪 + 圆形光点外飞。
     * <p>
     * 这里<b>不再画自绘的环</b> —— 环交给原版 {@code FastingEffect}。
     * 只保留白色爆闪（命中反馈）与光点（动能感）。
     *
     * @param after 该颗命中后经过的秒数
     */
    private void impactPulse(SpriteBatch sb, float after, int index, float punch) {
        if (after < 0.0f) {
            after = 0.0f;
        }
        // 白色爆闪：0.35s（同比缩放），末端余弦化掉
        float flashSpan = 0.35f * this.timeScale;
        float flashQ = MathUtils.clamp(after / flashSpan, 0.0f, 1.0f);
        if (flashQ < 1.0f) {
            float bright = flashQ < 0.18f ? flashQ / 0.18f
                    : FastingDraw.cosineOut((flashQ - 0.18f) / 0.82f);
            if (bright > 0.01f) {
                float grow = 1.0f + 0.50f * flashQ;
                Draw.glow(sb, this.ex, this.ey,
                        this.orbR * 8.0f * grow, this.orbR * 7.2f * grow, 0.0f,
                        FastingDraw.CORE, bright * 0.85f * punch);
                Draw.glow(sb, this.ex, this.ey,
                        this.orbR * 3.6f, this.orbR * 3.2f, 0.0f,
                        FastingDraw.CORE, bright * 0.75f);
            }
        }

        // 该颗颜色的圆形光点外飞（第 0 颗红、第 1 颗绿、第 2 颗再红）
        Color c = (index % 2 == 0) ? FastingDraw.STRENGTH : FastingDraw.DEXTERITY;
        float sparkSpan = 1.05f * this.timeScale;
        float sparkQ = MathUtils.clamp(after / sparkSpan, 0.0f, 1.0f);
        if (sparkQ >= 1.0f) {
            return;
        }
        float sparkA = FastingDraw.cosineOut(sparkQ);
        float spread = 1.0f - (1.0f - sparkQ) * (1.0f - sparkQ);
        float reach = this.orbR * 14.0f;
        for (int k = 0; k < SPARKS; ++k) {
            float ang = k * MathUtils.PI2 / SPARKS
                    + MathUtils.sin(index * 3.1f + k * 2.3f) * 0.35f;
            float dist = reach * (0.35f + 0.65f * spread);
            float sx2 = this.ex + MathUtils.cos(ang) * dist;
            float sy2 = this.ey + MathUtils.sin(ang) * dist * 0.86f;
            float r = this.orbR * (0.30f + 0.26f * sparkA);
            FastingDraw.spark(sb, sx2, sy2, r, sparkA * 0.85f * punch, c);
        }
    }

    @Override
    public void dispose() {
    }
}
