package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;

/**
 * 「确立基础」的常驻表现：<b>立在角色左侧的一座计量方尖碑</b>。
 * <p>
 * <h2>这块碑回答什么</h2>
 * <b>一条刻线 = 每张保留牌每回合降 1 费</b>。所以碑面竖刻线的<b>亮起道数</b>
 * 就是 {@code EstablishmentPower.amount}（原版 1，叠加两张才是 2）。
 * <p>
 * 旧实现是「一块碑 = 一层能力」，用 {@code amount} 当碑数。那个映射是错的：
 * <ul>
 *   <li>{@code amount} 是<b>降费点数</b>，不是层数。原版永远是 1，
 *       所以正常游戏里玩家只看到一块碑，「效果有多强」完全没表达</li>
 *   <li>叠加到 2 时画两块碑，读者会理解成「有两层能力」，
 *       而真实含义是「降 2 费」。<b>数字对上、语义对不上</b></li>
 * </ul>
 * 现在改成「一座碑 + {@code amount} 道刻线」：强度增加是<b>刻线多一道</b>，
 * 读作「压得更狠了」，而不是「多了块石头」。
 * <h2>为什么保留「碑」这个母题</h2>
 * 石碑是本系列里唯一「单侧、竖向、实心、贴地」的形态，四个维度都独立，
 * 没有撞任何其它牌。要改的是它承载的<b>信息</b>，不是形态本身
 * （规范 §2.6「状态耦合优先于叠加新元素」）。
 * <h2>动画四件套（规范 §三）</h2>
 * <ol>
 *   <li><b>出现</b>：碑从地里<b>升起</b>（{@code pow2Out}，0.75s），
 *       升到三成时迸一次碎石 —— 慢 + 新事件 = 看得见</li>
 *   <li><b>消失</b>：<b>沉回地下</b>（{@code pow2In}，0.95s），石头不往上飘</li>
 *   <li><b>位移</b>：锚点用 {@code FOLLOW_LERP} 缓动追赶角色</li>
 *   <li><b>状态</b>：{@code glow} 带阻尼，控制刻线金光；
 *       回合末结算时被顶起，之后缓慢回落</li>
 * </ol>
 * <h2>刻线道数变化不跳位</h2>
 * 第 {@code k} 条刻线的位置由 {@code markRatio(k, total)} 决定 ——
 * <b>它依赖 total</b>。这是刻意与「碑数」相反的选择：
 * <ul>
 *   <li><b>碑</b>是独立实体（一个东西），数量从 1 变 2 时新增一块，
 *       所以位置必须只由索引决定</li>
 *   <li><b>刻线</b>是同一块碑面上的<b>刻度</b>（一把尺），数量变化时整组要重新均分，
 *       否则会出现「1 道在中间、2 道挤在上半」的断裂感</li>
 * </ul>
 * 尺子的刻度就该跟着尺子重新分度。
 * <h2>不绑定骨骼（§1.1）与性能（§3）</h2>
 * 坐标只来自 {@link BodyFx}；没有子特效；每帧不 new；混合函数 get/set 还原。
 */
final class FoundationAuraEffect extends PowerAuraEffect {
    /** 碑升起时长。刻意比常规出现动画长，让「新碑出现」看得见。 */
    private static final float RISE_TIME = 0.75f;
    /** 退场（沉回地下）时长。重物的动作慢了才像有质量。 */
    private static final float SINK_TIME = 0.95f;
    /** 升起后过曝持续多久（秒）。 */
    private static final float FLASH_TIME = 1.10f;
    /** 锚点缓动速率。 */
    private static final float FOLLOW_LERP = 7.0f;
    /** 刻线金光的上升 / 回落（带阻尼）。 */
    private static final float GLOW_RISE = 5.0f;
    private static final float GLOW_FALL = 1.8f;
    /** 平时刻线的基础光强。 */
    private static final float GLOW_IDLE = 0.26f;
    /** 破土碎石的持续时间。 */
    private static final float DEBRIS_TIME = 0.80f;
    /**
     * 刻线点亮的平滑速率。
     * <p>
     * 叠加 / 失去能力时 {@code amount} 会跳变，刻线亮起道数也跳。
     * 用小数 + 阻尼把「点亮道数」平滑掉，读作「一道一道依次亮起」。
     */
    private static final float LIT_LERP = 7.0f;

    private final float[] spot = new float[2];

    /** 碑的升起进度 0→1。 */
    private float rise;
    /** 升起后的过曝剩余时间（秒）。 */
    private float flash;
    /** 是否已经迸过碎石。 */
    private boolean debrisDone;
    /** 破土碎石的剩余时间（秒）。 */
    private float debrisTime;

    /**
     * 平滑后的「已亮刻线道数」（可以是小数）。
     * <p>
     * 用小数是因为 {@code amount} 会跳变，直接取整会「啪」地多一道；
     * 小数版本能让新刻线<b>渐亮</b>。
     */
    private float smoothLit;

    private float appear;
    private float vanish;
    private float glow;
    /**
     * 锚点当前位置：本身已是平滑追踪值，绘制时直接用，
     * <b>不要再叠加滞后量</b>（先见之明踩过这个坑）。
     */
    private float followX;
    private float followY;
    private boolean anchored;

    FoundationAuraEffect(AbstractCreature owner, String cardId, String powerId) {
        super(owner, cardId, powerId);
        // 碑画在人前面，否则会被立绘遮掉；但它在单侧，不挡脸
        this.renderBehind = false;
        this.rise = 0.0f;
        this.flash = 0.0f;
        this.debrisDone = false;
        this.debrisTime = 0.0f;
        this.smoothLit = 0.0f;
        this.glow = GLOW_IDLE;
    }

    /** 回合末结算时被调用：刻线金光顶一下。 */
    @Override
    void pulse() {
        this.glow = 1.0f;
    }

    @Override
    protected void tick(float dt) {
        // ---- 整体出现 ----
        if (this.appear < 1.0f) {
            this.appear = Math.min(1.0f, this.appear + dt / 0.55f);
        }
        // ---- 退场：缓动下沉（pow2In：起手慢、末端快，读作「沉下去」）----
        if (this.isEnd) {
            this.vanish = Math.min(1.0f, this.vanish + dt / SINK_TIME);
        } else if (this.vanish > 0.0f) {
            this.vanish = Math.max(0.0f, this.vanish - dt / SINK_TIME);
        }

        // ---- 状态：带阻尼的刻线光 ----
        float speed = GLOW_IDLE > this.glow ? GLOW_RISE : GLOW_FALL;
        this.glow += (GLOW_IDLE - this.glow) * MathUtils.clamp(dt * speed, 0.0f, 1.0f);

        // ---- 刻线点亮的平滑：amount 跳变时逐道亮起，而不是「啪」地全亮 ----
        float targetLit = FoundationFx.discount(this.owner);
        this.smoothLit += (targetLit - this.smoothLit)
                * MathUtils.clamp(dt * LIT_LERP, 0.0f, 1.0f);

        // ---- 碑升起 ----
        if (this.rise < 1.0f) {
            this.rise = Math.min(1.0f, this.rise + dt / RISE_TIME);
            // 升到三成时迸一次碎石：读作「碑破土而出」
            if (!this.debrisDone && this.rise > 0.30f) {
                this.debrisDone = true;
                this.debrisTime = DEBRIS_TIME;
            }
            // 升起结束时来过曝
            if (this.rise >= 1.0f) {
                this.flash = FLASH_TIME;
            }
        } else if (this.flash > 0.0f) {
            this.flash = Math.max(0.0f, this.flash - dt);
        }
        if (this.debrisTime > 0.0f) {
            this.debrisTime = Math.max(0.0f, this.debrisTime - dt);
        }

        // ---- 锚点：缓动追赶角色 ----
        this.spot[0] = FoundationFx.monoX(this.owner);
        this.spot[1] = FoundationFx.monoBaseY(this.owner);
        if (!this.anchored) {
            this.followX = this.spot[0];
            this.followY = this.spot[1];
            this.anchored = true;
        } else {
            float k = MathUtils.clamp(dt * FOLLOW_LERP, 0.0f, 1.0f);
            this.followX += (this.spot[0] - this.followX) * k;
            this.followY += (this.spot[1] - this.followY) * k;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.owner == null || this.fade <= 0.004f || this.rise <= 0.0f) {
            return;
        }
        float alpha = this.fade * (1.0f - this.vanish);
        if (alpha <= 0.004f) {
            return;
        }
        float groove = MathUtils.clamp(this.glow, 0.0f, 1.0f);

        // 缓动后的退场进度：下沉用 pow2Out，读作「先松动、再落下」
        float vEase = Interpolation.pow2Out.apply(0.0f, 1.0f, this.vanish);
        // 出现：pow2Out 前快后慢
        float show = Interpolation.pow2Out.apply(0.0f, 1.0f, this.rise);
        // 退场：随着 vanish 增长，碑逐步沉入地下
        float sunk = vEase * 1.15f;
        show *= MathUtils.clamp(1.0f - sunk, 0.0f, 1.0f);

        float bx = this.followX;
        // 升起过程中碑从地下往上露：基座略微下沉
        float by = this.followY - FoundationFx.monoHeight(this.owner)
                * (1.0f - show) * 0.28f;

        int marks = FoundationFx.discount(this.owner);
        // 已亮道数：平滑后取整，余数交给"下一道渐亮"用
        int lit = Math.min((int) Math.floor(this.smoothLit + 0.001f), marks);

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        FoundationDraw.monolith(sb, bx, by,
                FoundationFx.monoHeight(this.owner),
                FoundationFx.monoWidth(this.owner),
                alpha, groove, lit, marks, show);

        // ---- 升起后的过曝：额外一层白热叠光，把人眼钉在碑上 ----
        if (this.flash > 0.0f) {
            float f = this.flash / FLASH_TIME;
            float fa = f * f * 0.55f;
            float bh = FoundationFx.monoHeight(this.owner) * show;
            float bw = FoundationFx.monoWidth(this.owner);
            Draw.glow(sb, bx, by + bh * 0.5f, bw * 4.2f, bh * 1.05f, 0.0f,
                    FoundationFx.CORE, alpha * fa * 0.55f);
            Draw.glow(sb, bx, by + bh * 0.35f, bw * 2.2f, bh * 0.5f, 0.0f,
                    FoundationFx.GOLD, alpha * fa * 0.45f);
            Draw.arc(sb, bx, by, bw * MathUtils.lerp(2.2f, 4.6f, 1.0f - f),
                    bw * MathUtils.lerp(0.66f, 1.38f, 1.0f - f),
                    0.0f, 360.0f,
                    Math.max(5.0f * Settings.scale, bw * 0.34f),
                    FoundationFx.STONE, alpha * fa * 0.80f, 22);
        }

        // ---- 破土碎石 ----
        if (this.debrisTime > 0.0f) {
            float q = 1.0f - this.debrisTime / DEBRIS_TIME;
            FoundationDraw.debris(sb, bx, by,
                    FoundationFx.monoWidth(this.owner) * 7.0f,
                    alpha * (1.0f - q * 0.4f), q, 11, 1.7f);
        }

        // ---- 下一道刻线的"将亮"微光：读作「还有一道没压下去」 ----
        float frac = this.smoothLit - (float) Math.floor(this.smoothLit);
        if (frac > 0.02f && lit < marks) {
            float ratio = FoundationFx.markRatio(lit, marks);
            float my = by + FoundationFx.monoHeight(this.owner) * show * ratio;
            Draw.glow(sb, bx, my,
                    FoundationFx.monoWidth(this.owner) * 3.2f,
                    FoundationFx.monoWidth(this.owner) * 1.2f, 0.0f,
                    FoundationFx.GOLD, alpha * frac * 0.20f);
        }

        sb.setBlendFunction(src, dst);
    }
}
