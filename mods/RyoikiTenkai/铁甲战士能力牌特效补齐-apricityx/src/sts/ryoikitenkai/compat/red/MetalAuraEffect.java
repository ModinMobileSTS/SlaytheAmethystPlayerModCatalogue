package sts.ryoikitenkai.compat.red;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 「金属化」的常驻表现：<b>腰肋处环绕身体的分片护甲带</b>。
 * <p>
 * <h2>它到底是什么</h2>
 * 不是光环，是<b> amount 片甲</b>（原版 3 / 升级 4）：
 * <ul>
 *   <li>一片甲 = 每回合 1 点格挡 —— 一一对应（规范 §2.7）</li>
 *   <li>环绕在腰肋（{@code height × 0.50}），按椭圆做透视</li>
 *   <li>远侧先画更暗更小、近侧后画更亮更大：<b>按深度排序</b>，
 *       这是「绕成一圈」的唯一来源</li>
 * </ul>
 * <h2>与「进化」的四条区分</h2>
 * <table border="1">
 *   <tr><td></td><td>金属化</td><td>进化</td></tr>
 *   <tr><td>几何</td><td><b>横向的分片环带</b></td><td>竖向的三叉光芽</td></tr>
 *   <tr><td>位置</td><td><b>腰肋、环住身体（前方）</b></td><td>背后、从腰到头</td></tr>
 *   <tr><td>动静</td><td><b>呼吸 + 高光沿环扫过 + 合拢</b></td><td>生长、尖端摆动</td></tr>
 *   <tr><td>材质</td><td><b>金属（实心、硬边、反光）</b></td><td>生物（半透、发光）</td></tr>
 * </table>
 * <h2>混合模式：甲体普通、接缝加色</h2>
 * 甲片用<b>普通混合</b>（{@code 770 × 771}）画：只有它能遮住身体，
 * 读成「穿在身上的甲」。用加色混合画出来的甲片只能是一根发亮的蓝条。
 * 接缝亮起是光，所以那一段切到<b>加色混合</b>（{@code 770 × 1}），画完还原。
 * <p>
 * 还原用 {@code getBlendSrcFunc/getBlendDstFunc}，不写死 —— 写死会把别处的状态冲掉。
 * <h2>动画四件套（规范 §三）</h2>
 * <ol>
 *   <li><b>出现</b>：alpha 走 smoothstep（0.55s），半径从 {@code ×1.035} 收到 {@code ×1.0}
 *       —— 甲片「沉到位」</li>
 *   <li><b>消失</b>：alpha 走 smoothstep 反向，半径向<b>外</b>漂到 {@code ×1.10}（0.78s）
 *       —— 护甲松开、卸下。消失比出现长（规范 §一基准）</li>
 *   <li><b>位移</b>：锚点用 {@code FOLLOW_LERP} 缓动追赶角色</li>
 *   <li><b>状态</b>：{@code clank}（合拢强度）由「获得格挡」顶到 1 后衰减；
 *       {@code sweep}（扫光相位）恒速推进；{@code breath}（呼吸）由时间驱动</li>
 * </ol>
 * <h2>为什么常驻只有「呼吸 + 扫光」两件事</h2>
 * 金属化是被动能力，每回合自动给格挡，不该抢注意力。
 * <b>扫光</b>是金属特有的静止状态：一道窄高光沿环慢慢走一圈（3.4s），
 * 甲片依次亮一下，比其他角色那几张的「呼吸放大」更像金属，也更省。
 * <h2>不产生任何子特效</h2>
 * 旧实现每隔 0.3~0.6s 生成一颗反光点（{@code spawnMetalGlint}）。
 * 现在高光由扫光相位在<b>同一帧内</b>算出来，<b>一个子特效都不生成</b>：
 * 没有队列开销，也不会在 effectList 遍历时派生对象。
 */
final class MetalAuraEffect extends AbstractGameEffect {
    /** 出现时长（秒）。 */
    private static final float APPEAR_TIME = 0.55f;
    /** 消失时长（秒）。比出现长。 */
    private static final float VANISH_TIME = 0.78f;
    /** 锚点缓动速率。 */
    private static final float FOLLOW_LERP = 8.0f;
    /** 合拢衰减时长（秒）。 */
    private static final float CLANK_TIME = 0.42f;
    /** 呼吸周期（秒）。 */
    private static final float BREATH_PERIOD = 2.9f;
    /** 扫光绕环一周的时间（秒）。 */
    private static final float SWEEP_PERIOD = 3.4f;

    private final AbstractCreature owner;

    private float time;
    private float fade;
    /** 合拢强度：获得格挡时置 1，随后衰减。 */
    private float clank;
    /** 扫光相位 0~1。 */
    private float sweep;
    /** 锚点当前位置（已是平滑值）。 */
    private float followX;
    private float followY;
    private boolean anchored;
    /** 本帧是否仍处于「生效」状态（决定 alpha 用哪条曲线）。 */
    private boolean activeNow;

    /** 由登记表置位：能力被移除 / 玩家关掉特效时淡出。 */
    boolean isEnd;

    MetalAuraEffect(AbstractCreature owner) {
        this.owner = owner;
        this.startingDuration = 1.0f;
        this.duration = 1.0f;
        // 甲片在身上，必须画在人前面才读作「穿着甲」
        this.renderBehind = false;
        this.clank = 0.0f;
        this.sweep = 0.0f;
        this.followX = BodyFx.centerX(owner);
        this.followY = BodyFx.groundY(owner);
        this.anchored = owner != null;
        this.activeNow = true;
    }

    /** 获得格挡时由登记表调用：整圈甲片合拢一下。 */
    void clank() {
        this.clank = 1.0f;
    }

    @Override
    public void update() {
        float dt = Gdx.graphics.getDeltaTime();
        this.time += dt;

        this.activeNow = !this.isEnd
                && RedEffectRegistry.isOn(RedEffectRegistry.METALLICIZE_ID)
                && this.owner != null
                && !this.owner.isDeadOrEscaped()
                && this.owner.hasPower(RedEffectRegistry.METALLICIZE_ID);

        if (!this.activeNow) {
            // ---- 消失：alpha 收到 0，甲片向外松脱 ----
            this.fade = Math.max(0.0f, this.fade - dt / VANISH_TIME);
            if (this.fade <= 0.001f) {
                this.fade = 0.0f;
                this.isDone = true;
            }
            return;
        }

        // ---- 出现 ----
        this.fade = Math.min(1.0f, this.fade + dt / APPEAR_TIME);

        // ---- 合拢衰减（带阻尼的最后一段，不硬切） ----
        if (this.clank > 0.0f) {
            this.clank = Math.max(0.0f, this.clank - dt / CLANK_TIME);
        }

        // ---- 扫光：恒速绕环 ----
        this.sweep = (this.sweep + dt / SWEEP_PERIOD) % 1.0f;

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
        if (this.fade <= 0.004f || this.owner == null) {
            return;
        }
        // smoothstep：两端斜率都是 0，出现与消失都不会硬切
        float a = this.fade * this.fade * (3.0f - 2.0f * this.fade);
        float breath = 0.5f + 0.5f * MathUtils.sin(
                this.time * MathUtils.PI2 / BREATH_PERIOD);

        // 出现时甲片从外往内「沉到位」；消失时向外松脱
        float drift = this.activeNow
                ? 1.0f + 0.035f * (1.0f - a)
                : 1.0f + 0.10f * (1.0f - this.fade);

        float dx = this.followX - BodyFx.centerX(this.owner);
        float dy = this.followY - BodyFx.groundY(this.owner);
        float cx = BodyFx.centerX(this.owner) + dx;
        float cy = MetalFx.bandY(this.owner) + dy;
        float rx = MetalFx.bandRx(this.owner) * drift;
        float ry = MetalFx.bandRy(this.owner) * drift;
        int n = MetalFx.plates(this.owner);
        float thick = MetalFx.plateThick(this.owner);

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();

        // ---- 甲体：普通混合（实心、遮住身体）----
        sb.setBlendFunction(770, 771);
        MetalDraw.band(sb, cx, cy, rx, ry, n, thick, breath,
                this.sweep, this.clank, a);

        // ---- 接缝亮起：加色混合（它是光，不是材质）----
        if (this.clank > 0.02f) {
            float cq = MathUtils.clamp(this.clank, 0.0f, 1.0f);
            sb.setBlendFunction(770, 1);
            MetalDraw.seamFlash(sb, cx, cy, rx, ry, thick,
                    0.95f * cq * cq * a, n);
        }

        sb.setBlendFunction(src, dst);
    }

    @Override
    public void dispose() {
    }
}
