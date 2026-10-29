package sts.ryoikitenkai.compat.red;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 「进化」的常驻表现：<b>从背后升起、一直在生长的三叉光芽</b>。
 * <p>
 * <h2>为什么从「贴地圆环」改成「背后竖芽」</h2>
 * 旧实现是一圈贴地的绿色同心椭圆环。问题是它和<b>金属化</b>的常驻
 * 在几何（贴地椭圆环）、位置（脚下）、动静（环在转 + 呼吸起伏）
 * <b>三项完全相同</b>，只有颜色不同：
 * <pre>
 *   进化    GlowRing.ellipse(cx, gy+5, rx = hw×1.30, ry = rx×0.26, 绿)
 *   金属化  GlowRing.ellipse(cx, gy+5, rx = hw×1.42, ry = rx×0.24, 钢蓝)
 * </pre>
 * 两张牌同时挂着时，玩家看到两圈几乎重合的环，分不清哪圈是谁的。
 * 这正是规范 §2.2 反复点名的坑：<b>几何不换，颜色换多少次都没用。</b>
 * <h2>现在的四条区分（与金属化）</h2>
 * <table border="1">
 *   <tr><td></td><td>金属化</td><td>进化</td></tr>
 *   <tr><td>几何</td><td>横向环绕的光带</td><td><b>竖向生长的三叉</b></td></tr>
 *   <tr><td>位置</td><td>贴地一圈</td><td><b>背后、从腰到头</b></td></tr>
 *   <tr><td>动静</td><td>环在转、甲面反光</td><td><b>自下而上生长、尖端摆动</b></td></tr>
 *   <tr><td>材质</td><td>金属（冷钢蓝、硬、反光）</td><td><b>生物（荧光绿、软、半透）</b></td></tr>
 * </table>
 * 四条全部不同。关键是<b>一横一竖、一个贴地一个背后</b>。
 * <h2>一个芽 = 一个额外机会</h2>
 * 股数 = 1 + {@code amount}（原版 1 → 2 股，升级 2 → 3 股）。
 * 强度变化是<b>多一股芽</b>，而不是旧实现的「光点更密」——
 * 这与观者那边「一页纸 = 一张洞见」「一根刻度 = 一点真言」是同一套做法
 * （规范 §2.7 一一对应）。
 * <h2>动画四件套（规范 §三）</h2>
 * <ol>
 *   <li><b>出现</b>：各股从根部往上<b>抽出</b>（{@code pow2Out}，1.05s），
 *       逐股错开 0.14s —— 慢，因为「生长」本身就该慢</li>
 *   <li><b>消失</b>：各股<b>缩回根部</b>并淡出（1.10s）。消失比出现长（规范 §一基准）</li>
 *   <li><b>位移</b>：锚点用 {@code FOLLOW_LERP} 缓动追赶角色</li>
 *   <li><b>状态</b>：{@code excite} 带阻尼，由「抽到状态牌」顶起；
 *       它驱动生长脉冲的速度与亮度</li>
 * </ol>
 * <h2>生长脉冲：让它「一直在长」</h2>
 * 每 {@code PULSE_PERIOD}（约 2.2s，兴奋时缩短到 1.1s）有一道生长脉冲
 * 自下而上跑过所有股，尖端随之轻轻摆动。这是常驻里唯一的周期性动作，
 * 不刺眼，但能让人看出「它是活的」。
 * <h2>股位不随总数跳位</h2>
 * 用<b>固定三槽位</b>（左 / 中 / 右）按总数取子集：
 * 2 股取{左, 右}，3 股取{左, 中, 右}。
 * <p>
 * 没有用 {@code k / (n−1)} 均分，因为那样 2 股时的左右两股
 * 在变成 3 股时会被挤到更外侧 —— 原有的芽位置变了，读者的参照点跳了
 * （规范 §三.7）。
 * <h2>不绑定骨骼（§1.1）与性能（§3）</h2>
 * 坐标只来自 {@link BodyFx}；没有子特效；tick 里不 new；
 * 每帧只为颜色建独立副本；混合函数 get/set 还原。
 */
final class EvolveAuraEffect extends AbstractGameEffect {
    /** 出现时长：单股从根部抽到顶。慢，因为「生长」就该慢。 */
    private static final float GROW_TIME = 1.05f;
    /** 逐股错开。 */
    private static final float GROW_STEP = 0.14f;
    /** 消失时长。比出现长（规范 §一基准）。 */
    private static final float RETRACT_TIME = 1.10f;
    /** 锚点缓动速率。 */
    private static final float FOLLOW_LERP = 8.0f;
    /** 兴奋度的上升 / 回落（带阻尼）。 */
    private static final float EXCITE_RISE = 8.0f;
    private static final float EXCITE_FALL = 1.8f;
    /** 平时兴奋度基础值。 */
    private static final float EXCITE_IDLE = 0.16f;
    /** 生长脉冲的周期：平时 / 兴奋时（秒）。 */
    private static final float PULSE_PERIOD_IDLE = 2.20f;
    private static final float PULSE_PERIOD_HOT = 1.10f;
    /** 尖端摆动的角幅度（rad）。 */
    private static final float SWAY_AMP = 0.20f;
    /** 摆动的周期（秒），各股错相。 */
    private static final float SWAY_PERIOD = 3.6f;
    /** 分叉张开完成所需的生长进度之后的部分。 */
    private static final float FORK_FROM = 0.58f;

    private final AbstractCreature owner;

    private float time;
    private float fade;
    /** 带阻尼的兴奋度：抽到状态牌时顶起。 */
    private float excite;
    /** 每道芽的生长进度 0~1。 */
    private final float[] grow = new float[EvolveFx.MAX_SPROUTS];
    /** 每道芽的起动延迟（秒）。 */
    private final float[] delay = new float[EvolveFx.MAX_SPROUTS];
    /** 当前已经启动的芽数（只增不减；能力层数在本局里几乎不降）。 */
    private int shown;
    /** 生长脉冲的累计相位 0~1。 */
    private float pulse;
    /** 锚点当前位置（本身已是平滑值，绘制时直接用）。 */
    private float followX;
    private float followY;
    private boolean anchored;

    /** 由登记表置位：能力被移除 / 玩家关掉特效时开始回落。 */
    boolean isEnd;

    EvolveAuraEffect(AbstractCreature owner) {
        this.owner = owner;
        this.startingDuration = 1.0f;
        this.duration = 1.0f;
        // 芽在背后，画在人后面才读作「从他背后长出来」
        this.renderBehind = true;
        this.excite = EXCITE_IDLE;
        this.pulse = 0.0f;
        for (int i = 0; i < EvolveFx.MAX_SPROUTS; ++i) {
            this.grow[i] = 0.0f;
            this.delay[i] = i * GROW_STEP;
        }
        this.shown = 0;
    }

    /** 抽到状态牌时由登记表调用：兴奋度顶起，芽会窜一截。 */
    void pulse() {
        this.excite = 1.0f;
    }

    @Override
    public void update() {
        float dt = Gdx.graphics.getDeltaTime();
        this.time += dt;

        boolean active = !this.isEnd
                && RedEffectRegistry.isOn(RedEffectRegistry.EVOLVE_ID)
                && this.owner != null
                && !this.owner.isDeadOrEscaped()
                && this.owner.hasPower(RedEffectRegistry.EVOLVE_ID);

        if (!active) {
            // ---- 消失：各股缩回根部 + 淡出 ----
            this.fade = Math.max(0.0f, this.fade - dt / RETRACT_TIME);
            for (int i = 0; i < EvolveFx.MAX_SPROUTS; ++i) {
                this.grow[i] = Math.max(0.0f, this.grow[i] - dt / RETRACT_TIME);
            }
            if (this.fade <= 0.001f) {
                this.fade = 0.0f;
                this.isDone = true;
            }
            return;
        }

        // ---- 出现：时间驱动 + pow2Out（起手快、末端柔和） ----
        this.fade = Math.min(1.0f, this.fade + dt / GROW_TIME);

        // ---- 兴奋度：带阻尼（规范 §三.4）----
        float speed = EXCITE_IDLE > this.excite ? EXCITE_RISE : EXCITE_FALL;
        this.excite += (EXCITE_IDLE - this.excite)
                * MathUtils.clamp(dt * speed, 0.0f, 1.0f);

        // ---- 芽数与能力层数对齐：只增不减 ----
        int want = EvolveFx.sprouts(this.owner);
        if (want > this.shown) {
            for (int i = this.shown; i < want; ++i) {
                this.grow[i] = 0.0f;
                this.delay[i] = 0.0f;
            }
            this.shown = want;
        }

        // ---- 逐股生长 ----
        for (int i = 0; i < this.shown; ++i) {
            if (this.delay[i] > 0.0f) {
                this.delay[i] = Math.max(0.0f, this.delay[i] - dt);
                continue;
            }
            if (this.grow[i] < 1.0f) {
                this.grow[i] = Math.min(1.0f, this.grow[i] + dt / GROW_TIME);
            }
        }

        // ---- 生长脉冲：兴奋时更快 ----
        float period = MathUtils.lerp(PULSE_PERIOD_IDLE, PULSE_PERIOD_HOT,
                MathUtils.clamp(this.excite, 0.0f, 1.0f));
        this.pulse = (this.pulse + dt / Math.max(0.001f, period)) % 1.0f;

        // ---- 锚点：缓动追赶角色 ----
        if (!this.anchored) {
            this.followX = BodyFx.centerX(this.owner);
            this.followY = BodyFx.groundY(this.owner);
            this.anchored = true;
        } else {
            float k = MathUtils.clamp(dt * FOLLOW_LERP, 0.0f, 1.0f);
            this.followX += (BodyFx.centerX(this.owner) - this.followX) * k;
            this.followY += (BodyFx.groundY(this.owner) - this.followY) * k;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.fade <= 0.004f || this.owner == null || this.shown <= 0) {
            return;
        }
        float ex = MathUtils.clamp(this.excite, 0.0f, 1.0f);
        int n = this.shown;
        float h = EvolveFx.sproutHeight(this.owner);
        // 锚点偏移量：让芽跟着缓动后的位置走
        float dx = this.followX - BodyFx.centerX(this.owner);
        float dy = this.followY - BodyFx.groundY(this.owner);

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        // 从后往前画：中间那股（3 股时）最后画，压住两边
        for (int i = 0; i < n; ++i) {
            if (this.grow[i] <= 0.0f) {
                continue;
            }
            float bx = EvolveFx.sproutX(this.owner, i, n) + dx;
            float by = EvolveFx.sproutBaseY(this.owner, i, n) + dy;

            // 生长进度套缓动：pow2Out 前快后慢，读作「顶出来后站稳」
            float g = MathUtils.clamp(this.grow[i], 0.0f, 1.0f);
            float show = (float) (1.0 - Math.pow(1.0 - g, 2.0));

            // 分叉：生长过 FORK_FROM 之后才逐渐张开
            float fork = MathUtils.clamp((show - FORK_FROM) / (1.0f - FORK_FROM),
                    0.0f, 1.0f);
            // 兴奋时叉张得更开（读作「被喂饱了」）
            fork = MathUtils.clamp(fork * (1.0f + 0.35f * ex), 0.0f, 1.0f);

            // 尖端摆动：各股错相，兴奋时幅度更大
            float sway = MathUtils.sin(this.time * MathUtils.PI2 / SWAY_PERIOD
                    + i * 1.7f) * SWAY_AMP * (0.6f + 0.4f * ex);
            // 生长脉冲：让芽在这个相位上稍微「鼓一下」
            float pulseLift = 1.0f + 0.06f * MathUtils.sin(
                    (this.pulse - i * 0.12f) * MathUtils.PI2);

            float lean = EvolveFx.sproutLean(i, n);
            // 亮度：兴奋顶起，并叠加生长脉冲带来的轻微起伏
            float bright = (0.78f + 0.22f * ex) * pulseLift;

            // 中间那股稍亮一档，读作「主芽」
            if (n >= 3 && i == 1) {
                bright *= 1.10f;
            }

            EvolveDraw.sprout(sb, bx, by, h * pulseLift, lean,
                    show, fork, this.fade * bright, sway);
        }

        sb.setBlendFunction(src, dst);
    }

    @Override
    public void dispose() {
    }
}
