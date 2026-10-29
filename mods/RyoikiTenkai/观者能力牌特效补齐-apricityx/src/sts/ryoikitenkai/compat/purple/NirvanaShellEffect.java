package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 「涅槃」<b>获得格挡</b>时的演出：<b>一层新护壁从体内顶出来</b>。
 * <p>
 * <h2>这个动作对应什么</h2>
 * 原版 {@code NirvanaPower.onScry()} 做的事是：
 * <pre>
 * this.flash();
 * this.addToBot(new GainBlockAction(this.owner, this.amount));
 * </pre>
 * 也就是「<b>占卜了一次，于是多了一层护住自己的东西</b>」。
 * 所以画面是<b>六边形从身体内侧向外撑开、棱上扫过一圈亮环</b>。
 * <h2>为什么只演「多了一层」，不演「占卜」</h2>
 * {@code ScryAction.update()} 会遍历所有 power 调 {@code onScry()}，
 * 所以涅槃与<b>先见之明同一帧触发</b>。先见之明演的是「一列光框被扫描显影」，
 * 如果涅槃也演占卜类的图案，两套会在同一位置叠起来。
 * <p>
 * 分工（见 {@link NirvanaFx} 类注释）：
 * <ul>
 *   <li>先见之明 → 演「看见了什么」</li>
 *   <li>涅槃 → 演「因此护住了自己」</li>
 * </ul>
 * 所以这里<b>只画护壁，不画任何预视/占卜的语义</b>。
 * <h2>时间线（总长 0.86s，{@code FAST_MODE} × 0.72）</h2>
 * <pre>
 *   0.00–0.28  顶出   六边形从 0.55 撑到 1.08（略微冲出）
 *   0.16–0.50  过曝   棱与顶点一起亮到最高
 *   0.50–0.86  收敛   回到 1.0 倍，余弦淡出把控制权交给常驻
 * </pre>
 * <h2>动画四件套（规范 §三）</h2>
 * <ol>
 *   <li><b>出现</b>：{@code swingOut} 撑开，带轻微过冲，读作「被顶出来」</li>
 *   <li><b>消失</b>：转为「亮环扫过棱」的余辉，余弦淡出，不硬切</li>
 *   <li><b>位移</b>：半径从 0.55 扩到 1.08 再回到 1.0</li>
 *   <li><b>状态</b>：{@code excite} 带阻尼，控制亮度</li>
 * </ol>
 * <h2>频次</h2>
 * 一次占卜触发一次，可能一回合好几次 —— <b>完全静音、不震屏</b>。
 * 走 {@code queueTop} 与常驻保持一致，避免两者错位。
 */
final class NirvanaShellEffect extends AbstractGameEffect {
    /** 顶出时长（标准模式，会乘 timeScale）。 */
    private static final float RISE_BASE = 0.28f;
    /** 过曝峰值出现的时刻。 */
    private static final float PEAK_AT = 0.16f;
    /** 收敛 + 余韵总时长。 */
    private static final float FADE_BASE = 0.58f;
    /** 撑开时冲出的最大倍数。 */
    private static final float OVERSHOOT = 1.08f;

    private final float cx;
    private final float cy;
    private final float baseR;
    /** 画出哪一层：取「刚新增的那一层」的索引。 */
    private final int layer;
    private final float life;
    private final float timeScale;
    private final float riseTime;
    private final float power;

    private float time;
    private float excite;

    /**
     * @param owner 能力持有者（玩家）
     * @param per   每层面的值（用于决定这层画在哪一格）
     * @param power 0.75~1.15 的强度系数：升级版每次 4 点，比 3 点更"实"
     */
    NirvanaShellEffect(AbstractCreature owner, int per, float power) {
        this.timeScale = Settings.FAST_MODE ? 0.72f : 1.0f;
        this.riseTime = RISE_BASE * this.timeScale;
        this.power = MathUtils.clamp(power, 0.5f, 1.2f);

        this.cx = NirvanaFx.centerX(owner);
        this.cy = NirvanaFx.centerY(owner);

        // 这一层画在「刚刚多出来的那一层」的位置上。
        // 用 layers() 得到的当前层数减 1 作为索引，所以新层总是最外那层。
        int now = NirvanaFx.layers(owner);
        this.layer = MathUtils.clamp(now - 1, 0, NirvanaFx.MAX_LAYERS - 1);
        this.baseR = NirvanaFx.radius(owner, this.layer);

        this.life = this.riseTime + FADE_BASE * this.timeScale;
        this.startingDuration = this.duration = this.life;
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
        // 状态：带阻尼。顶出瞬间拉满，之后缓慢回落。
        float target = this.time < this.riseTime + 0.12f ? 1.0f : 0.25f;
        float speed = target > this.excite ? 18.0f : 2.2f;
        this.excite += (target - this.excite) * MathUtils.clamp(dt * speed, 0.0f, 1.0f);
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.duration <= 0.0f || this.baseR <= 2.0f) {
            return;
        }
        float ex = MathUtils.clamp(this.excite, 0.0f, 1.0f);
        float punch = (0.88f + 0.26f * ex) * this.power;

        // 半径：swingOut 冲到 OVERSHOOT 再回到 1.0
        float riseQ = MathUtils.clamp(this.time / Math.max(0.001f, this.riseTime),
                0.0f, 1.0f);
        float riseEase = Interpolation.swingOut.apply(0.0f, 1.0f, riseQ);

        // 余韵：顶出完成之后整体淡出
        float tailQ = MathUtils.clamp((this.time - this.riseTime)
                / Math.max(0.001f, FADE_BASE * this.timeScale), 0.0f, 1.0f);
        float fade = this.time <= this.riseTime ? 1.0f
                : FastingDraw.cosineOut(tailQ);

        // 半径从 0.55 扩到 OVERSHOOT，再缓慢收敛回 1.0
        float scaleIn = MathUtils.lerp(0.55f, OVERSHOOT, riseEase);
        float settle = tailQ > 0.0f
                ? MathUtils.lerp(OVERSHOOT, 1.0f,
                        Interpolation.pow2Out.apply(0.0f, 1.0f, tailQ))
                : scaleIn;
        float scale = this.time <= this.riseTime ? scaleIn : settle;

        float rx = this.baseR * scale;
        float ry = rx * NirvanaFx.flatten(this.layer);
        if (rx <= 2.0f) {
            return;
        }

        float a = MathUtils.clamp(fade * punch, 0.0f, 1.0f);
        if (a <= 0.006f) {
            return;
        }

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        // 过曝：在顶出的前段最亮（读作「刚成形、还在发烫」）
        float sincePeak = this.time - PEAK_AT * this.timeScale;
        float over = sincePeak <= 0.0f
                ? 1.0f - Math.abs(sincePeak) / Math.max(0.001f,
                        PEAK_AT * this.timeScale)
                : FastingDraw.cosineOut(MathUtils.clamp(sincePeak
                        / Math.max(0.001f, RISE_BASE * this.timeScale), 0.0f, 1.0f));
        over = MathUtils.clamp(over, 0.0f, 1.0f);

        // 护壁本体：内衬 + 棱 + 亮棱 + 顶点，全部走同一个画笔
        NirvanaDraw.shell(sb, this.cx, this.cy, rx, ry, 0.0f,
                1.0f, over, a);

        // 沿棱扫过的亮环：读作「这一层刚刚被顶到位」
        if (tailQ > 0.0f && tailQ < 1.0f) {
            NirvanaDraw.ring(sb, this.cx, this.cy, rx, ry, 0.0f,
                    tailQ, a * 0.95f);
        }

        // 中心一小团暖光：让「多了一层」有个能量来源
        if (riseQ < 1.0f) {
            float r = this.baseR * 0.55f * (0.4f + 0.6f * riseQ);
            Draw.glow(sb, this.cx, this.cy, r * 2.4f, r * 2.2f, 0.0f,
                    NirvanaFx.JADE, a * 0.20f * (1.0f - riseQ));
            Draw.glow(sb, this.cx, this.cy, r * 1.1f, r * 1.0f, 0.0f,
                    NirvanaFx.EDGE_LIGHT, a * 0.26f * (1.0f - riseQ));
        }

        sb.setBlendFunction(src, dst);
    }

    @Override
    public void dispose() {
    }
}
