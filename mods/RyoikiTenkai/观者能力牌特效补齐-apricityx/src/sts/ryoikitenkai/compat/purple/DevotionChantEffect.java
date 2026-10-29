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
 * 「虔信」<b>回合开始累加真言</b>时的演出：
 * <b>胸口涌起光团 → N 颗光点沿弧线并入刻度环 → 落在对应刻度上点亮它</b>。
 * <p>
 * <h2>这个动作对应什么</h2>
 * 原版 {@code DevotionPower.atStartOfTurnPostDraw()} 每回合做的事是：
 * <pre>
 * ApplyPowerAction(owner, owner, new MantraPower(owner, amount), amount)
 * </pre>
 * 也就是「<b>又诵出一句真言，并进刻度里</b>」。所以画面是
 * <b>从胸口浮起几颗光点、沿弧线汇入光轮、对应刻度随之点亮</b>。
 * <p>
 * 光点数量 = {@code DevotionPower.amount}（原版 2，升级 3）——
 * 与它实际加的真言点数一一对应，数字对得上。
 * <h2>本轮返工：加大加亮（用户反馈「特效不够明显」）</h2>
 * <ul>
 *   <li>光点半径从 {@code 身高×0.045} 提到 <b>{@code ×0.075}</b>，
 *       并给每个光点加一层<b>大范围外光</b>（{@code r×6}），飞过时照亮周围</li>
 *   <li>起势段新增<b>胸口光柱</b>：一条自下而上抽起的竖向光带，
 *       让「真言从体内升起」这件事在地面上就看得到</li>
 *   <li>并入环之后，落点留下一颗<b>持续闪亮的刻度光</b>，
 *       直到特效结束 —— 相当于把「这一句已经记在环上了」钉在画面上</li>
 *   <li>全程走<b>最顶层队列</b>（{@link PurpleEffectRegistry#queueTop}）：
 *       光点会飞到胸口高度，普通层会被手牌压住</li>
 * </ul>
 * <h2>时间线（总长 1.55s，{@code FAST_MODE} × 0.72）</h2>
 * <pre>
 *   0.00–0.22  起势   胸口光柱抽起，光团成形（swingOut 由 0.4 弹到 1.0）
 *   0.18–0.66  上升   光点沿弧线浮向刻度环（各错开 0.09s）
 *   0.66–0.86  并入   光点撞上环，落点炸亮
 *   0.86–1.55  余韵   落点继续闪亮 + 沿轨道滑走的余辉，余弦淡出
 * </pre>
 * <h2>动画四件套（规范 §三）</h2>
 * <ol>
 *   <li><b>出现</b>：光点在胸口由 0.4 弹到 1.0（{@code swingOut}）</li>
 *   <li><b>消失</b>：并入后转为沿轨道滑行的余辉，余弦淡出，不硬切</li>
 *   <li><b>位移</b>：沿二次贝塞尔从胸口浮到环上的落点</li>
 *   <li><b>状态</b>：{@code excite} 带阻尼，控制亮度与光点尺寸</li>
 * </ol>
 * <h2>克制</h2>
 * 每回合都会发生 —— <b>不震屏、不音效</b>。
 * 全部在局部变量里算，<b>不产生子特效</b>。
 */
final class DevotionChantEffect extends AbstractGameEffect {
    /** 起势时长（标准模式，会乘 timeScale）。 */
    private static final float FORM_BASE = 0.22f;
    /** 上升时长。 */
    private static final float RISE_BASE = 0.48f;
    /** 余韵时长。 */
    private static final float TAIL_BASE = 0.69f;
    /** 光点之间的错开（秒，标准模式）。 */
    private static final float STAGGER_BASE = 0.09f;
    /** 最多画几颗光点。 */
    private static final int MAX_ORBS = 4;

    private final float cx;
    private final float chestY;
    private final float footY;
    private final float ringX;
    private final float ringY;
    private final float rx;
    private final float ry;
    private final float orbR;
    private final int count;
    private final float life;
    private final float timeScale;
    private final float formTime;
    private final float riseTime;
    /** 光点各自的落点角度（度）与弧线控制偏移。构造时定死。 */
    private final float[] landDeg = new float[MAX_ORBS];
    private final float[] ctrlOff = new float[MAX_ORBS];

    private float time;
    private float excite;

    private final float[] cur = new float[2];

    DevotionChantEffect(AbstractCreature owner, int amount, int mantraNow) {
        this.timeScale = Settings.FAST_MODE ? 0.72f : 1.0f;
        this.formTime = FORM_BASE * this.timeScale;
        this.riseTime = RISE_BASE * this.timeScale;

        this.cx = DevotionFx.centerX(owner);
        this.footY = BodyFx.groundY(owner);
        this.chestY = BodyFx.groundY(owner) + BodyFx.height(owner) * 0.42f;
        this.ringX = DevotionFx.centerX(owner);
        this.ringY = DevotionFx.centerY(owner);
        this.rx = DevotionFx.radiusX(owner);
        this.ry = DevotionFx.radiusY(owner);
        // 光点尺寸：上一版 0.045 太小，在手机上就是个像素点
        this.orbR = Math.max(9.0f * Settings.scale, BodyFx.height(owner) * 0.075f);
        this.count = MathUtils.clamp(amount, 1, MAX_ORBS);

        // 落点角度：从「这次之前」的真言数对应刻度开始，依次往后 ——
        // 读作「光点正好落在那几根刚要亮的刻度上」
        int from = MathUtils.clamp(mantraNow - amount, 0, DevotionFx.TICKS - 1);
        for (int i = 0; i < MAX_ORBS; ++i) {
            int tick = (from + i) % DevotionFx.TICKS;
            this.landDeg[i] = -90.0f + 360.0f * tick / DevotionFx.TICKS;
            // 弧线控制点轻微偏外，让路径是弧的
            this.ctrlOff[i] = (i % 2 == 0 ? 1.0f : -1.0f) * 0.20f;
        }

        this.life = this.formTime + this.riseTime
                + (this.count - 1) * STAGGER_BASE * this.timeScale
                + TAIL_BASE * this.timeScale;
        this.startingDuration = this.duration = this.life;
        // 光点会飞到胸口高度，普通层会被手牌压住
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
        // 状态：带阻尼。起势快、回落慢。
        float target = this.progress() < 0.60f ? 1.0f : 0.25f;
        float speed = target > this.excite ? 14.0f : 2.2f;
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
        float punch = 0.90f + 0.30f * ex;
        // 颜色跟随当前进度（紫白 → 粉），与常驻环一致
        AbstractCreature me = com.megacrit.cardcrawl.dungeons.AbstractDungeon.player;
        float prog = me == null ? 0.0f : DevotionFx.progress(me);
        Color main = new Color(DevotionFx.tint(prog));
        Color core = new Color(DevotionFx.coreTint(prog));

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        // ================= 0. 胸口光柱：真言从体内升起 =================
        // 只在起势 + 上升前段出现，读作「诵出这一句」的起点
        float pillarQ = MathUtils.clamp(this.time / Math.max(0.001f,
                this.formTime + this.riseTime * 0.55f), 0.0f, 1.0f);
        if (pillarQ < 1.0f) {
            float fadeIn = pillarQ < 0.30f ? pillarQ / 0.30f : 1.0f;
            float fadeOut = FastingDraw.cosineOut(Math.max(0.0f,
                    (pillarQ - 0.30f) / 0.70f));
            float pa = fadeIn * fadeOut * 0.85f * punch;
            float w = this.orbR * (2.2f + 1.6f * MathUtils.sin(pillarQ * MathUtils.PI));
            Draw.glow(sb, this.cx, this.chestY, w * 3.0f, w * 3.0f, 0.0f, main, pa * 0.34f);
            Draw.glow(sb, this.cx, this.chestY, w * 1.5f, w * 1.5f, 0.0f, main, pa * 0.62f);
            Draw.glow(sb, this.cx, this.chestY, w * 0.7f, w * 0.7f, 0.0f, core, pa * 0.80f);
        }

        for (int i = 0; i < this.count; ++i) {
            float start = i * STAGGER_BASE * this.timeScale;
            float t = this.time - start;
            if (t <= 0.0f) {
                continue;
            }

            float landRad = this.landDeg[i] * MathUtils.degreesToRadians;
            float lx = this.ringX + MathUtils.cos(landRad) * this.rx;
            float ly = this.ringY + MathUtils.sin(landRad) * this.ry;

            if (t < this.formTime) {
                // ---- 起势：在胸口成形，swingOut 从 0.4 弹到 1.0 ----
                float q = t / this.formTime;
                float grow = Interpolation.swingOut.apply(0.4f, 1.0f, q);
                float r = this.orbR * grow;
                Draw.glow(sb, this.cx, this.chestY, r * 6.0f, r * 6.0f, 0.0f,
                        main, 0.24f * q * punch);
                Draw.glow(sb, this.cx, this.chestY, r * 2.4f, r * 2.4f, 0.0f,
                        main, 0.66f * q * punch);
                Draw.glow(sb, this.cx, this.chestY, r * 1.0f, r * 1.0f, 0.0f,
                        core, 0.90f * q);
                continue;
            }

            float riseQ = MathUtils.clamp((t - this.formTime) / this.riseTime, 0.0f, 1.0f);

            if (riseQ < 1.0f) {
                // ---- 上升：沿二次贝塞尔从胸口浮到环上落点 ----
                // pow2In：起手慢、越接近落点越快 —— 读作「被吸进环里」
                float e = Interpolation.pow2In.apply(0.0f, 1.0f, riseQ);
                // 控制点：中点略偏外，让路径拱起
                float ctrlX = (this.cx + lx) * 0.5f
                        + MathUtils.cos(landRad) * this.rx * this.ctrlOff[i];
                float ctrlY = (this.chestY + ly) * 0.5f + this.ry * 0.70f;
                float u = 1.0f - e;
                this.cur[0] = u * u * this.cx + 2.0f * u * e * ctrlX + e * e * lx;
                this.cur[1] = u * u * this.chestY + 2.0f * u * e * ctrlY + e * e * ly;

                float px = this.cur[0];
                float py = this.cur[1];
                // 越飞越小、越亮
                float r = this.orbR * (1.0f - 0.20f * e);
                Draw.glow(sb, px, py, r * 6.2f, r * 6.2f, 0.0f, main,
                        0.22f * punch * (0.6f + 0.4f * e));
                Draw.glow(sb, px, py, r * 2.6f, r * 2.6f, 0.0f, main,
                        0.66f * punch);
                Draw.glow(sb, px, py, r * 1.1f, r * 1.1f, 0.0f, core, 0.92f);

                // 拖尾：读作「从体内一路被抽上去」
                for (int j = 1; j <= 3; ++j) {
                    float back = 1.0f - j * 0.055f;
                    if (back <= 0.0f) {
                        continue;
                    }
                    // 沿路径回退一点（近似：把 e 往回拉）
                    float be = MathUtils.clamp(e - j * 0.045f, 0.0f, 1.0f);
                    float bu = 1.0f - be;
                    float bx = bu * bu * this.cx + 2.0f * bu * be * ctrlX + be * be * lx;
                    float by = bu * bu * this.chestY + 2.0f * bu * be * ctrlY + be * be * ly;
                    float br = r * (1.0f - j * 0.22f);
                    Draw.glow(sb, bx, by, br * 3.0f, br * 3.0f, 0.0f, main,
                            0.22f * (1.0f - j * 0.26f));
                }
                continue;
            }

            // ---- 并入之后：落点持续闪亮 + 沿轨道滑走的余辉 ----
            float after = t - this.formTime - this.riseTime;
            float total = Math.max(0.001f, this.life - start - this.formTime - this.riseTime);
            float q = MathUtils.clamp(after / total, 0.0f, 1.0f);
            // 末端斜率为 0：余弦淡出，不硬切
            float fade = FastingDraw.cosineOut(q);
            if (fade <= 0.008f) {
                continue;
            }

            // (a) 落点：先炸一下，然后维持一颗中等亮度的刻度光直到结束
            float hit = q < 0.12f ? q / 0.12f : 1.0f;
            float hold = 1.0f - 0.35f * q;
            Draw.glow(sb, lx, ly, this.orbR * 6.4f, this.orbR * 6.4f, 0.0f,
                    main, fade * 0.30f * punch * hit);
            Draw.glow(sb, lx, ly, this.orbR * 2.4f, this.orbR * 2.4f, 0.0f,
                    main, fade * 0.80f * punch * hold);
            Draw.glow(sb, lx, ly, this.orbR * 1.0f, this.orbR * 1.0f, 0.0f,
                    core, fade * 0.95f * hold);

            // (b) 余辉：沿轨道向前滑一小段（正方向 = 顺时针，与常驻自转同向）
            float slide = q * 30.0f;
            float sRad = (this.landDeg[i] + slide) * MathUtils.degreesToRadians;
            float sx = this.ringX + MathUtils.cos(sRad) * this.rx;
            float sy = this.ringY + MathUtils.sin(sRad) * this.ry;
            Draw.glow(sb, sx, sy, this.orbR * 4.4f, this.orbR * 4.4f, 0.0f,
                    main, fade * 0.24f * punch);
            Draw.glow(sb, sx, sy, this.orbR * 1.6f, this.orbR * 1.6f, 0.0f,
                    main, fade * 0.52f * punch);
        }

        // 收尾的一道竖向余痕：光点都收进去之后，胸口还在微微发光
        if (this.time > this.formTime + this.riseTime) {
            float tailQ = MathUtils.clamp((this.time - this.formTime - this.riseTime)
                    / Math.max(0.001f, TAIL_BASE * this.timeScale), 0.0f, 1.0f);
            float ta = FastingDraw.cosineOut(tailQ) * 0.42f;
            float h = this.ringY - this.footY;
            if (ta > 0.01f && h > 1.0f) {
                Draw.glow(sb, this.cx, this.footY + h * 0.5f,
                        this.orbR * 2.6f, h * 1.15f, 0.0f, main, ta * 0.30f);
                Draw.glow(sb, this.cx, this.footY + h * 0.62f,
                        this.orbR * 1.1f, h * 0.65f, 0.0f, core, ta * 0.22f);
            }
        }

        sb.setBlendFunction(src, dst);
    }

    @Override
    public void dispose() {
    }
}
