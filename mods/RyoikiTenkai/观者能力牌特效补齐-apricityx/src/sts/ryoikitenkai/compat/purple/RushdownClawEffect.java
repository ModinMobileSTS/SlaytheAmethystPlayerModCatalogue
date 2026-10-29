package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 「猛虎下山」在<b>愤怒中打出攻击牌</b>时，<b>把抓痕刻在被打的敌人身上</b>。
 * <p>
 * <h2>画布是固定的 boss 尺度</h2>
 * 抓痕长度固定为 {@link RushdownFx#canvasSize()}（屏幕高度 × 0.30，约等于 boss 体型），
 * <b>所有敌人身上都是同一道醒目的裂口</b>。锚点每帧重读 {@code target.hb}，
 * 所以黏在会动、会被击退的敌人身上。
 * <h2>本轮修正：放慢（用户反馈「太快、消失不丝滑」）</h2>
 * <ul>
 *   <li>总时长 0.86s → <b>1.30s</b>（重击 0.98s → <b>1.50s</b>）</li>
 *   <li>停留段延长：从 0.36–0.70 改为 <b>0.34–0.62</b>，占时比例更小但绝对时间更长</li>
 *   <li>消退段的淡出走 {@code pow2In}，并<b>把反向收刀与上浮的幅度都加大</b>，
 *       让「伤口淡去」有明确的过程</li>
 *   <li>着肉时的每道错拍从 0.050 减到 0.042（总时长变长，相对间隔自然拉大）</li>
 * </ul>
 * <h2>禁止细线条</h2>
 * 抓痕的白热内芯、分叉、光渣全部是<b>有厚度的锥形光块 / 圆形光点</b>，
 * 最小宽度 4px 为硬底线。全文没有任何 {@code Draw.line}。
 * <h2>为什么要有「停留」这一段</h2>
 * 只有「出现 → 立刻消失」的话，抓痕在几十毫秒内就没了，玩家根本看不到伤口。
 * 停留之后，玩家才会读到「这只怪身上现在还留着三道爪印」。
 * <h2>目标中途死亡</h2>
 * 敌人可能在特效播放期间就死了，那时 {@code hb} 会随死亡动画移走，
 * 继续按它画会看到抓痕「飘到半空」。所以检测到亡就立刻淡出收掉。
 * <h2>克制</h2>
 * 不震屏、不放边框闪光 —— 一回合能打好几张攻击牌，喧闹会疲劳。
 */
final class RushdownClawEffect extends AbstractGameEffect {
    /** 最多几道。 */
    private static final int MAX_SCARS = 4;
    /** 单道裂开占总时长的比例。 */
    private static final float OPEN_SPAN = 0.20f;
    /** 每道之间的启动错拍（归一化）。 */
    private static final float STEP = 0.042f;
    /** 停留段的终点：之后开始消退。 */
    private static final float HOLD_TO = 0.62f;

    /** 受击目标：每帧读它的 hb，所以抓痕永远黏在身上。 */
    private final AbstractCreature target;
    /** 目标判定框的半宽 / 高，只用来算锚点偏移，不参与尺寸缩放。 */
    private final float hw;
    private final float h;
    /** 固定的 boss 尺度画布边长。 */
    private final float canvas;
    private final float life;
    private final int scars;
    private final boolean heavy;
    private final float seed;

    /** 抓痕主方向（弧度）：构造时定死。 */
    private final float angle;
    /** 每道的法向偏移 / 长度系数 / 角度微差 / 形状种子。 */
    private final float[] offs = new float[MAX_SCARS];
    private final float[] lenMul = new float[MAX_SCARS];
    private final float[] jit = new float[MAX_SCARS];
    private final float[] seeds = new float[MAX_SCARS];

    private float time;
    private float excite;
    /** 目标已死时的额外消退量 0→1。 */
    private float abort;

    RushdownClawEffect(AbstractCreature from, AbstractCreature target, boolean heavy) {
        this.target = target;
        this.heavy = heavy;
        AbstractCreature canvasOwner = target != null ? target : from;
        this.hw = Math.max(30.0f * Settings.scale, BodyFx.halfWidth(canvasOwner));
        this.h = Math.max(40.0f * Settings.scale, BodyFx.height(canvasOwner));
        // 固定 boss 尺度：不论打谁，抓痕都一样大
        this.canvas = RushdownFx.canvasSize();
        this.scars = heavy ? MAX_SCARS : MAX_SCARS - 1;
        this.seed = MathUtils.random(-3.0f, 3.0f);
        // 走向随机在「左上→右下」与「左下→右上」之间取
        this.angle = (MathUtils.randomBoolean() ? -1.0f : 1.0f)
                * MathUtils.random(0.28f, 0.62f);

        for (int i = 0; i < MAX_SCARS; ++i) {
            float k = i - (this.scars - 1) * 0.5f;
            // 法向间距 = 画布的 0.19，保证「三道各自可读」
            this.offs[i] = k * this.canvas * 0.19f;
            this.lenMul[i] = 1.0f - 0.09f * Math.abs(k);
            this.jit[i] = MathUtils.random(-4.0f, 4.0f);
            this.seeds[i] = MathUtils.random(0.0f, MathUtils.PI2);
        }

        // 放慢：0.86 → 1.30 / 0.98 → 1.50
        float base = heavy ? 1.50f : 1.30f;
        this.life = Settings.FAST_MODE ? base * 0.60f : base;
        this.startingDuration = this.duration = this.life;
        // 抓痕必须盖在敌人立绘之上
        this.renderBehind = false;
    }

    @Override
    public void update() {
        float dt = Gdx.graphics.getDeltaTime();
        this.time += dt;
        this.duration -= dt;
        // 目标死了就快速收掉，避免抓痕跟着死亡动画飘到半空
        if (this.target != null && this.target.isDeadOrEscaped()) {
            this.abort = Math.min(1.0f, this.abort + dt / 0.22f);
        }
        if (this.duration <= 0.0f || this.abort >= 1.0f) {
            this.isDone = true;
            return;
        }
        // 状态：带阻尼（规范 §三.4）。着肉瞬间拉满，之后缓慢回落。
        float target = this.progress() < HOLD_TO ? 1.0f : 0.30f;
        float speed = target > this.excite ? 15.0f : 2.6f;
        this.excite += (target - this.excite) * MathUtils.clamp(dt * speed, 0.0f, 1.0f);
    }

    private float progress() {
        return MathUtils.clamp(1.0f - this.duration / this.life, 0.0f, 1.0f);
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.duration <= 0.0f || this.target == null) {
            return;
        }
        float sc = Settings.scale;
        float p = this.progress();
        float ex = MathUtils.clamp(this.excite, 0.0f, 1.0f);

        // 整体透明度与上浮：0.62 之后走 pow2In 缓动淡出
        float alpha;
        float lift = 0.0f;
        if (p < HOLD_TO) {
            alpha = 1.0f;
        } else {
            float q = (p - HOLD_TO) / (1.0f - HOLD_TO);
            float out = Interpolation.pow2In.apply(0.0f, 1.0f, q);
            alpha = 1.0f - out;
            // 上浮幅度加大：读作「爪气散去」
            lift = out * this.h * 0.22f;
        }
        alpha *= (1.0f - this.abort);
        if (alpha <= 0.008f) {
            return;
        }

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        // 锚点：目标判定框中心，每帧重读 —— 敌人动了抓痕跟着动
        float cx = this.target.hb.cX;
        float cy = this.target.hb.cY - lift;

        float nx = -MathUtils.sin(this.angle);
        float ny = MathUtils.cos(this.angle);

        // ================= 0. 来势：一枚光斑掠向目标（不是细线） =================
        if (p < 0.14f) {
            float q = p / 0.14f;
            float fromX = cx - nx * this.canvas * 0.95f;
            float fromY = cy + this.canvas * 0.24f;
            float k = Interpolation.pow3Out.apply(0.0f, 1.0f, q);
            float px = MathUtils.lerp(fromX, cx, k);
            float py = MathUtils.lerp(fromY, cy, k);
            float fade = (1.0f - q) * 0.55f * alpha;
            float r = Math.max(5.0f, this.canvas * 0.05f);
            Draw.glow(sb, px, py, r * 4.4f, r * 4.4f, 0.0f, RushdownFx.FUR, fade * 0.45f);
            Draw.glow(sb, px, py, r * 1.9f, r * 1.9f, 0.0f, RushdownFx.CORE, fade * 0.72f);
        }

        // 抓痕宽度：按固定画布给，所以小怪身上也够粗
        float width = Math.max(7.0f, this.canvas * 0.062f) * (this.heavy ? 1.22f : 1.0f)
                * (0.90f + 0.30f * ex);

        // ================= 1. 三道抓痕依次着肉 =================
        for (int i = 0; i < this.scars; ++i) {
            float start = 0.06f + i * STEP;
            float open = MathUtils.clamp((p - start) / OPEN_SPAN, 0.0f, 1.0f);
            if (open <= 0.0f) {
                continue;
            }
            // 出现用 pow3Out（起手极快）；消退时反向展开（从尾端往回收）
            float sweep;
            if (p < HOLD_TO) {
                sweep = Interpolation.pow3Out.apply(open);
            } else {
                float q = (p - HOLD_TO) / (1.0f - HOLD_TO);
                // 收刀也走缓动：先慢后快
                sweep = MathUtils.clamp(open - Interpolation.pow2In.apply(0.0f, 1.0f, q) * 0.95f,
                        0.0f, 1.0f);
            }
            if (sweep <= 0.0f) {
                continue;
            }

            float k = i - (this.scars - 1) * 0.5f;
            float off = this.offs[i] * (1.0f + 0.10f * ex);
            // 每道角度略有差异，像真的有三根爪尖
            float a2 = this.angle + this.jit[i] * MathUtils.degreesToRadians;
            float ux2 = MathUtils.cos(a2);
            float uy2 = MathUtils.sin(a2);

            float mx = cx + nx * off;
            float my = cy + ny * off;
            float half = this.canvas * 0.5f * this.lenMul[i];
            float x0 = mx - ux2 * half;
            float y0 = my - uy2 * half;
            float x1 = mx + ux2 * half;
            float y1 = my + uy2 * half;

            // 流动高光：沿痕走的一枚亮点，三道相位错开
            float flow = MathUtils.clamp(((p * 1.8f) + i * 0.30f) % 1.0f, 0.0f, 1.0f);

            RushdownDraw.claw(sb, x0, y0, x1, y1,
                    width * (1.0f - 0.13f * Math.abs(k)), sc,
                    RushdownFx.FUR, RushdownFx.CORE,
                    alpha * 0.96f, sweep, this.seeds[i], flow, true);
        }

        // ================= 2. 伤口处：锥形裂纹 + 圆点光渣 =================
        float q2 = MathUtils.clamp((p - 0.08f) / 0.62f, 0.0f, 1.0f);
        if (q2 > 0.0f) {
            float fade = 1.0f - Interpolation.pow2In.apply(0.0f, 1.0f, q2);
            float burst = Interpolation.pow3Out.apply(MathUtils.clamp(q2 / 0.55f, 0.0f, 1.0f));

            // 中心白热：读到「肉被撕开」
            float core = this.canvas * 0.14f * burst * (this.heavy ? 1.25f : 1.0f);
            Draw.glow(sb, cx, cy, core * 2.4f, core * 2.1f, 0.0f,
                    RushdownFx.FUR, fade * 0.55f * alpha);
            Draw.glow(sb, cx, cy, core * 1.1f, core * 0.95f, 0.0f,
                    RushdownFx.CORE, fade * 0.80f * alpha);

            // 裂纹：锥形光块，不是放射细线
            RushdownDraw.cracks(sb, cx, cy, this.canvas * 0.28f, sc,
                    RushdownFx.FUR, RushdownFx.CORE, fade * 0.85f * alpha, burst,
                    this.heavy ? 5 : 4, this.seed + 2.7f);

            // 光渣：圆形光点
            RushdownDraw.sparks(sb, cx, cy, this.canvas * 0.24f, sc,
                    RushdownFx.FUR, RushdownFx.CORE, fade * 0.75f * alpha, burst,
                    this.heavy ? 8 : 5, this.seed + 5.3f);
        }

        sb.setBlendFunction(src, dst);
    }

    @Override
    public void dispose() {
    }
}
