package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 「虔信」<b>被打出</b>时的演出：<b>十根刻度凭空刻下来，然后整圈锁死成形</b>。
 * <p>
 * <h2>为什么需要它（这是一处真正的缺口）</h2>
 * 之前的 {@code cast(CARD_DEVOTION)} 分支是<b>直接 {@code return}</b>，
 * 理由是「真言要等回合开始才到账，所以施放时不演」。
 * 那个理由只对了一半 —— <b>不该演「加真言」，但必须演「这张牌被挂上了」</b>。
 * 结果就是：<b>打出虔信这张牌，画面上什么都没有发生</b>，
 * 要等到下回合开始才第一次看到刻度环。这是「特效不够明显」里最直接的一条。
 * <p>
 * 原版 {@code Devotion.use()} 真正做的事是
 * {@code ApplyPowerAction(new DevotionPower(p, 2))} ——
 * 也就是「<b>把刻度刻在身上</b>」。所以施放演出就是：
 * <b>十根刻度逐根被刻到空中 → 整圈一亮锁死 → 环开始自转</b>，
 * 让「这张牌现在生效了」有一个明确的、看得见的时间点。
 * <h2>时间线（总长 1.43s，{@code FAST_MODE} × 0.72）</h2>
 * <pre>
 *   0.00–0.22  起势   环中心出现一个亮点（「要刻了」）
 *   0.22–0.88  刻画   十根刻度按 0.066s 间隔逐根从内向外刻出（swingOut 弹到位）
 *   0.88–1.01  锁死   整圈轨道一次性亮起，环锁死成形
 *   1.01–1.43  余韵   整圈向外扩一层涟漪，余弦淡出
 * </pre>
 * <h2>时间轴自洽（规范 §三 补充章节）</h2>
 * 本类用<b>绝对秒</b>驱动，所以：
 * <ol>
 *   <li>所有阶段时长都乘同一个 {@code timeScale}</li>
 *   <li>刻画的<b>间隔直接由跨度算出</b>（{@code span / TICKS}），不是另写一个常量；
 *       每根刻完需要的 {@code 2 × step} 必然在 {@code tCarveEnd} 之前结束 ——
 *       上一版用 {@code step × 2.2f} 会让最后一根拖到锁死之后</li>
 *   <li>{@code life} 由「起势 + 刻画跨度 + 锁死 + 余韵」<b>反推</b>，不是拍脑袋给的</li>
 * </ol>
 * <h2>与常驻的关系</h2>
 * 常驻的 {@link DevotionAuraEffect} 也会自己展开（0.80s 渐显），
 * 但那是「环慢慢出现」；这一段是「<b>被刻上去</b>」——
 * 两者叠在一起正好：刻画给出时间点，常驻给出持续性。
 * 所以这里刻意只画<b>刻度与轨道</b>，不画绕行光与进度弧，避免与常驻重复。
 * <h2>动画四件套（规范 §三）</h2>
 * <ol>
 *   <li><b>出现</b>：每根刻度用 {@code swingOut} 从 0 弹到 1，带过冲</li>
 *   <li><b>消失</b>：锁死后的涟漪向外扩、余弦淡出（末端斜率为 0）</li>
 *   <li><b>位移</b>：刻度从内圈（0.72 半径）向外刻到位（1.0 半径）</li>
 *   <li><b>状态</b>：{@code excite} 带阻尼，控制刻画亮度</li>
 * </ol>
 * <h2>克制</h2>
 * 一局只会发生几次 —— 但仍然是<b>无震屏、无音效</b>：
 * 原版给能力牌的音效已经有了，视觉只补「刻度被刻下来」这一件事。
 */
final class DevotionSealEffect extends AbstractGameEffect {
    /** 起势时长（标准模式，会乘 timeScale）。 */
    private static final float FORM_BASE = 0.22f;
    /** 十根刻度依次刻完的总跨度。 */
    private static final float CARVE_SPAN_BASE = 0.66f;
    /** 锁死时长。 */
    private static final float LOCK_BASE = 0.13f;
    /** 余韵时长。 */
    private static final float TAIL_BASE = 0.42f;
    /** 单根刻度刻完所需时间 = 间隔 × 这个倍数。必须 ≤ 2 才不会越过锁死时刻。 */
    private static final float CARVE_MUL = 1.85f;

    private final float cx;
    private final float cy;
    private final float rx;
    private final float ry;
    private final float life;
    private final float timeScale;
    /** 单根刻度的刻完时间（秒，已缩放）。 */
    private final float carveDur;

    private float time;
    private float excite;

    DevotionSealEffect(AbstractCreature owner) {
        this.timeScale = Settings.FAST_MODE ? 0.72f : 1.0f;
        this.cx = DevotionFx.centerX(owner);
        this.cy = DevotionFx.centerY(owner);
        this.rx = DevotionFx.radiusX(owner);
        this.ry = DevotionFx.radiusY(owner);

        // 间隔由跨度算出，不另写常量：保证最后一根一定在锁死之前刻完
        float span = CARVE_SPAN_BASE * this.timeScale;
        float step = span / DevotionFx.TICKS;
        this.carveDur = step * CARVE_MUL;

        // life 由时间轴反推（规范 §三），不是拍脑袋给的
        this.life = FORM_BASE * this.timeScale
                + span
                + step * (CARVE_MUL - 1.0f)   // 最后一根刻完还要这么久
                + LOCK_BASE * this.timeScale
                + TAIL_BASE * this.timeScale;
        this.startingDuration = this.duration = this.life;
        // 刻度会刻到胸口上方，普通层可能被手牌压住
        this.renderBehind = false;
    }

    @Override
    public void update() {
        float dt = Gdx.graphics.getDeltaTime();
        this.time += dt;
        this.duration -= dt;
        if (this.duration <= 0.0f) {
            this.isDone = true;
            return;
        }
        // 状态：刻画期间拉满，锁死之后回落
        float target = this.progress() < 0.78f ? 1.0f : 0.30f;
        float speed = target > this.excite ? 15.0f : 2.4f;
        this.excite += (target - this.excite) * MathUtils.clamp(dt * speed, 0.0f, 1.0f);
    }

    private float progress() {
        return MathUtils.clamp(1.0f - this.duration / this.life, 0.0f, 1.0f);
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.duration <= 0.0f) {
            return;
        }
        float ex = MathUtils.clamp(this.excite, 0.0f, 1.0f);
        float punch = 0.88f + 0.28f * ex;
        // 施放时真言还没到账，所以固定用「紫白」这一端，不跟进度
        Color main = new Color(DevotionFx.MANTRA);
        Color core = new Color(DevotionFx.MANTRA_CORE);

        float tForm = FORM_BASE * this.timeScale;
        float span = CARVE_SPAN_BASE * this.timeScale;
        float step = span / DevotionFx.TICKS;
        // 最后一根刻完的时刻
        float tCarveEnd = tForm + span + step * (CARVE_MUL - 1.0f);
        float tLockEnd = tCarveEnd + LOCK_BASE * this.timeScale;
        float tailDur = TAIL_BASE * this.timeScale;
        int total = DevotionFx.TICKS;

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        // ================= 1. 起势：中心一颗亮点 =================
        if (this.time < tForm) {
            float q = MathUtils.clamp(this.time / Math.max(0.001f, tForm), 0.0f, 1.0f);
            float grow = Interpolation.swingOut.apply(0.0f, 1.0f, q);
            float r = Math.max(10.0f * Settings.scale, this.rx * 0.20f) * grow;
            Draw.glow(sb, this.cx, this.cy, r * 5.0f, r * 5.0f, 0.0f,
                    main, 0.30f * q * punch);
            Draw.glow(sb, this.cx, this.cy, r * 2.0f, r * 2.0f, 0.0f,
                    main, 0.72f * q * punch);
            Draw.glow(sb, this.cx, this.cy, r * 0.9f, r * 0.9f, 0.0f,
                    core, 0.92f * q);
        }

        // ================= 2. 刻画：十根刻度逐根刻出 =================
        if (this.time >= tForm) {
            float thick = Math.max(7.0f * Settings.scale, this.rx * 0.085f);
            float fullLen = Math.max(15.0f * Settings.scale, this.rx * 0.22f);
            // 锁死之后的整体淡出
            float out = this.time <= tLockEnd ? 1.0f
                    : FastingDraw.cosineOut(MathUtils.clamp(
                            (this.time - tLockEnd) / Math.max(0.001f, tailDur),
                            0.0f, 1.0f));

            for (int i = 0; i < total; ++i) {
                float t0 = tForm + i * step;
                if (this.time < t0) {
                    continue;
                }
                float q = MathUtils.clamp((this.time - t0) / Math.max(0.001f, this.carveDur),
                        0.0f, 1.0f);
                // 刻画：swingOut 有过冲，读作「一笔刻下去」
                float e = Interpolation.swingOut.apply(0.0f, 1.0f, q);
                float a = e * out * punch;
                if (a <= 0.008f) {
                    continue;
                }

                float deg = -90.0f + 360.0f * i / total;
                float rad = deg * MathUtils.degreesToRadians;
                float cs = MathUtils.cos(rad);
                float sn = MathUtils.sin(rad);
                // 从内圈往外刻：0.72 → 1.0
                float rr = MathUtils.lerp(0.72f, 1.0f, e);
                float px = this.cx + cs * this.rx * rr;
                float py = this.cy + sn * this.ry * rr;
                // 椭圆外法线：保证上下左右一样长
                float nx = cs / this.rx;
                float ny = sn / this.ry;
                float nl = (float) Math.sqrt(nx * nx + ny * ny);
                if (nl < 1.0e-5f) {
                    continue;
                }
                nx /= nl;
                ny /= nl;

                float len = fullLen * (0.55f + 0.45f * e);
                Draw.glow(sb, px, py, thick * 4.0f, thick * 4.0f, 0.0f,
                        main, a * 0.34f);
                Draw.line(sb, px, py,
                        px + nx * len, py + ny * len, thick, main, a * 0.95f);
                Draw.line(sb, px, py,
                        px + nx * len * 0.92f, py + ny * len * 0.92f,
                        Math.max(5.0f * Settings.scale, thick * 0.45f), core, a * 0.70f);
            }
        }

        // ================= 3. 锁死：整圈轨道一次性亮起 =================
        if (this.time >= tCarveEnd) {
            float q = MathUtils.clamp((this.time - tCarveEnd)
                    / Math.max(0.001f, LOCK_BASE * this.timeScale), 0.0f, 1.0f);
            float fade = this.time <= tLockEnd ? 1.0f
                    : FastingDraw.cosineOut(MathUtils.clamp(
                            (this.time - tLockEnd) / Math.max(0.001f, tailDur),
                            0.0f, 1.0f));
            // 锁死那一下轨道突然亮起，读作「环扣上了」
            float lockPop = q < 0.35f ? q / 0.35f : 1.0f - 0.25f * ((q - 0.35f) / 0.65f);
            float a = MathUtils.clamp(lockPop, 0.0f, 1.0f) * fade * punch;
            if (a > 0.004f) {
                DevotionDraw.track(sb, this.cx, this.cy, this.rx, this.ry,
                        a * 1.2f, main);
                // 锁死瞬间一圈过曝
                Draw.glow(sb, this.cx, this.cy, this.rx * 2.6f, this.ry * 2.6f, 0.0f,
                        main, a * 0.24f);
            }
        }

        // ================= 4. 余韵：向外扩一层涟漪 =================
        if (this.time > tLockEnd) {
            float q = MathUtils.clamp((this.time - tLockEnd)
                    / Math.max(0.001f, tailDur), 0.0f, 1.0f);
            DevotionDraw.ripple(sb, this.cx, this.cy, this.rx, this.ry,
                    q, punch, main, core);
        }

        sb.setBlendFunction(src, dst);
    }

    @Override
    public void dispose() {
    }
}
