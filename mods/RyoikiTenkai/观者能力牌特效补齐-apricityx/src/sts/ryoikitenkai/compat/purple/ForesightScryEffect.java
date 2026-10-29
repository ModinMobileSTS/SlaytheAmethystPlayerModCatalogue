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
 * 「先见之明」<b>占卜那一刻</b>的演出：<b>一列光框被依次扫描显影</b>。
 * <p>
 * <h2>这个动作对应什么</h2>
 * 原版 {@code ForesightPower.atStartOfTurn()} 每回合做的事是：
 * <pre>
 * this.flash();
 * addToBot(new ScryAction(this.amount));   // 摊开 amount 张，可弃掉任意张
 * </pre>
 * 也就是「<b>把还没抽到的 N 张牌摊开看一遍</b>」。所以画面是
 * <b>一列光框依次被一道冷白光扫过、框内随之显出内容，然后退回未显影</b>。
 * 顺序扫描是关键 —— 它读作「一张一张翻开」，而不是「一起亮一下」。
 * <h2>为什么是「横向扫描」而不是「竖向生长」</h2>
 * 竖向生长在「确立基础」里已经用过了（石碑从地里升起）。
 * 占卜的动作是「把盖着的东西翻开」，对应的是一道<b>扫过的光</b>，
 * 所以用横向光带自上而下扫过 —— 动静性质完全不同（一个是生长，一个是掠过）。
 * <h2>时间线（总长 1.30s，{@code FAST_MODE} × 0.72）</h2>
 * <pre>
 *   0.00–0.16  起势     整列先亮一档，读作「开始看了」
 *   0.16–0.85  逐框扫描  每框一道光带自上而下扫过，框间错开 0.085s
 *   0.85–1.30  余韵     各框残留一点显影，余弦淡出
 * </pre>
 * <h2>时间轴自洽（规范 §三 补充章节）</h2>
 * 本类用<b>绝对秒</b>驱动，所以：
 * <ol>
 *   <li>所有阶段时长都乘同一个 {@code timeScale}</li>
 *   <li>框间错开与「单框扫描时长」都显式参与 {@code life} 的计算</li>
 *   <li>{@code life} 由「起势 + (n−1) × 错开 + 单框扫描 + 余韵」<b>反推</b>，
 *       不是拍脑袋给的</li>
 * </ol>
 * <h2>动画四件套（规范 §三）</h2>
 * <ol>
 *   <li><b>出现</b>：框随扫描迅速撑开，用 {@code pow2Out}（起手快、末端柔）</li>
 *   <li><b>消失</b>：余韵用 {@code cosineOut}（末端斜率为 0），不硬切</li>
 *   <li><b>位移</b>：光带自上而下<b>匀速</b>扫过 ——
 *       扫描本身就该匀速，用缓动会读成「加速下落」</li>
 *   <li><b>状态</b>：{@code excite} 带阻尼，控制整列亮度</li>
 * </ol>
 * <h2>频次与队列</h2>
 * 每回合都会发生 —— <b>不震屏、不音效</b>。
 * 走<b>普通队列</b>：{@code ScryAction} 会在同一帧打开选牌界面，
 * 顶层特效会压在选牌界面上；光框位置在头顶上方，本来也不会被手牌遮住。
 * <h2>施放时也复用这个类</h2>
 * {@link ForesightFx#onCast} 用同一个类补一记「成形」弹动
 * （构造函数带 {@code strength} 系数，施放时用 0.75 的弱强度）。
 * 两处共用一套画笔与时间轴，不会出现「两处各算一遍常量」的旧坑（规范 §六）。
 */
final class ForesightScryEffect extends AbstractGameEffect {
    /** 起势时长（标准模式，会乘 timeScale）。 */
    private static final float LEAD_BASE = 0.16f;
    /** 单个框被光带扫完的时长。 */
    private static final float SCAN_BASE = 0.42f;
    /** 框与框之间错开的间隔。 */
    private static final float STAGGER_BASE = 0.085f;
    /** 余韵时长。 */
    private static final float TAIL_BASE = 0.56f;

    private final float gx;
    private final float gy;
    private final float w;
    private final float h;
    private final float sp;
    private final float tilt;
    private final int count;
    private final float life;
    private final float timeScale;
    private final float leadTime;
    private final float scanTime;
    private final float stagger;
    private final float tailTime;
    /** 强度系数：1.0 = 每回合占卜，0.75 = 施放时的成形弹动。 */
    private final float strength;

    private float time;
    private float excite;

    ForesightScryEffect(AbstractCreature owner, int count, float strength) {
        this.timeScale = Settings.FAST_MODE ? 0.72f : 1.0f;
        this.leadTime = LEAD_BASE * this.timeScale;
        this.scanTime = SCAN_BASE * this.timeScale;
        this.stagger = STAGGER_BASE * this.timeScale;
        this.tailTime = TAIL_BASE * this.timeScale;
        this.strength = MathUtils.clamp(strength, 0.0f, 1.0f);

        this.gx = ForesightFx.groupX(owner);
        this.gy = ForesightFx.groupY(owner);
        this.w = ForesightFx.frameW(owner);
        this.h = ForesightFx.frameH(owner);
        this.sp = ForesightFx.spacing(owner);
        this.tilt = ForesightFx.tilt();
        this.count = MathUtils.clamp(count, 1, ForesightFx.MAX_FRAMES);

        // life 由时间轴反推：最后一个框的扫描必须走完，再加余韵
        this.life = this.leadTime
                + (this.count - 1) * this.stagger
                + this.scanTime
                + this.tailTime;
        this.startingDuration = this.duration = this.life;
        // 光框在头顶上方，与手牌区不重叠
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
        // 状态：带阻尼。扫描期间拉满，之后回落。
        float target = this.time < this.leadTime + this.scanTime * 1.5f ? 1.0f : 0.28f;
        float speed = target > this.excite ? 16.0f : 2.3f;
        this.excite += (target - this.excite) * MathUtils.clamp(dt * speed, 0.0f, 1.0f);
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.duration <= 0.0f) {
            return;
        }
        float ex = MathUtils.clamp(this.excite, 0.0f, 1.0f);
        float punch = (0.85f + 0.30f * ex) * (0.70f + 0.30f * this.strength);

        Color edge = new Color(ForesightFx.VIOLET);
        Color core = new Color(ForesightFx.ICE);

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        float rad = 13.0f * MathUtils.degreesToRadians;

        for (int i = 0; i < this.count; ++i) {
            float t0 = this.leadTime + i * this.stagger;
            if (this.time < t0) {
                continue;
            }
            float u = i - (this.count - 1) * 0.5f;
            float cx = this.gx + MathUtils.cos(rad) * this.sp * u;
            float cy = this.gy + MathUtils.sin(rad) * this.sp * u;

            float local = this.time - t0;                 // 该框自己的时间
            float scanQ = MathUtils.clamp(local / Math.max(0.001f, this.scanTime),
                    0.0f, 1.0f);
            boolean scanning = local <= this.scanTime;

            // ---- 余韵：扫描完之后残留一点显影，余弦淡出到 0 ----
            float tailQ = MathUtils.clamp(
                    (local - this.scanTime) / Math.max(0.001f, this.tailTime), 0.0f, 1.0f);
            float tailFade = scanning ? 1.0f : FastingDraw.cosineOut(tailQ);

            // 显影程度：扫描中迅速升到 1，之后按余韵回落
            float reveal = scanning
                    ? Interpolation.pow2Out.apply(0.0f, 1.0f, scanQ)
                    : tailFade;

            float a = tailFade * punch;
            if (a <= 0.006f) {
                continue;
            }

            // 框随扫描撑开：从 0.55 长到 1.0，读作「这一张被翻开了」
            float grow = scanning
                    ? MathUtils.lerp(0.55f, 1.0f,
                            Interpolation.pow2Out.apply(0.0f, 1.0f, scanQ))
                    : MathUtils.lerp(0.92f, 1.0f, 1.0f - tailQ);
            float fw = this.w * grow;
            float fh = this.h * MathUtils.lerp(0.75f, 1.0f, grow);

            // 框本身（显影时边会加粗、内部会亮）
            ForesightDraw.frame(sb, cx, cy, fw, fh, this.tilt,
                    a, reveal, edge, core, ForesightFx.DORMANT);

            // 扫描光带：只在扫描阶段出现
            if (scanning) {
                // 进入前有一小段淡入，避免「凭空出现」
                float inA = MathUtils.clamp(scanQ / 0.18f, 0.0f, 1.0f);
                ForesightDraw.scan(sb, cx, cy, fw, fh, this.tilt,
                        scanQ, a * inA, edge, core);
            }

            // 显影到最亮时，整框外面炸一下（读作「看清了这一张」）
            if (scanning && scanQ > 0.30f && scanQ < 0.92f) {
                float peak = 1.0f - Math.abs((scanQ - 0.61f) / 0.31f);
                if (peak > 0.0f) {
                    Draw.glow(sb, cx, cy, fw * 1.9f, fh * 1.9f, this.tilt,
                            core, a * 0.26f * peak);
                }
            }
        }

        sb.setBlendFunction(src, dst);
    }

    @Override
    public void dispose() {
    }
}
