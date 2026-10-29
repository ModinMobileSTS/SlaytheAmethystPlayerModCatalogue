package sts.ryoikitenkai.compat.green;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;

/**
 * 「精准」的常驻标靶：<b>浮在人物左侧的暗红标靶</b>。
 * <h2>它是什么</h2>
 * <pre>
 *     ◎╌╌╌   外环（暗红主光带）+ 内侧更暗的细环
 *    ╱ ⊕ ╲   靶心 3 层同心环 + 砖红实芯
 *     ╲┼╱    十字准星（靶的符号，四段极短刻）
 *      ╌     内环（暖白，清晰的结构线）
 * </pre>
 * 标靶上<b>没有任何向外辐射的横条</b>（细齿、径向刻痕、符文弧都已删除）。
 * 读数由卡牌上的附魔边框表达 —— 每张小刀都有一圈，一眼就知道在生效。
 * <h2>两处动态</h2>
 * <ul>
 *   <li>外环<b>扫光</b>：匀速沿环走一圈（是光点，不是横条）</li>
 *   <li>整靶<b>呼吸</b>：0.84~1.0，幅度小</li>
 * </ul>
 * <h2>充能不由它负责</h2>
 * 供能连线由 {@link AccuracyLinkEffect} 画在手牌之下；
 * 卡牌附魔边框由 {@link AccuracyCardRenderPatch} 画在卡牌自己的渲染流程里。
 */
final class AccuracyEdgeAuraEffect extends PowerAuraEffect {
    /** 锚点缓动速率。 */
    private static final float FOLLOW_LERP = 8.5f;
    /** 命中脉动：上升快、回落慢。 */
    private static final float PULSE_RISE = 18.0f;
    private static final float PULSE_FALL = 2.6f;
    /** 消失时的上抽量与缩小比例。 */
    private static final float VANISH_RISE = 18.0f;
    private static final float VANISH_SHRINK = 0.40f;

    /** 类私有颜色副本。 */
    private final Color ring = new Color();
    private final Color ringHi = new Color();
    private final Color core = new Color();
    private final Color link = new Color();

    private float followX;
    private float followY;
    private boolean anchored;

    /** 命中脉动 0~1。 */
    private float pulse;

    AccuracyEdgeAuraEffect(AbstractCreature owner) {
        super(owner, GreenEffectRegistry.ACCURACY_CARD, GreenEffectRegistry.ACCURACY_ID);
        // 浮在人物左侧身外：画在人前面，避免被立绘或怪物吃掉
        this.renderBehind = false;
    }

    /** 打出一把小刀时调用：标靶脉动一下。 */
    @Override
    void pulse() {
        this.pulse = 1.0f;
    }

    /** 当前累计时间（供能连线的起飞点要跟着轻浮，两处用同一相位）。 */
    float currentTime() {
        return this.time;
    }

    @Override
    protected void tick(float dt) {
        this.pulse = smooth(this.pulse, 0.0f, PULSE_RISE, PULSE_FALL, dt);

        float tx = AccuracyFx.targetX(this.owner);
        float ty = AccuracyFx.targetY(this.owner);
        if (!this.anchored) {
            this.followX = tx;
            this.followY = ty;
            this.anchored = true;
        } else {
            float k = MathUtils.clamp(dt * FOLLOW_LERP, 0.0f, 1.0f);
            this.followX += (tx - this.followX) * k;
            this.followY += (ty - this.followY) * k;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.fade <= 0.004f || !alive()) {
            return;
        }

        // ---- 出现 / 消失的形态变化（swing 已是单调曲线，不会死平）----
        float grown = swing(appearT());
        float v = vanish();
        float vEase = GreenDraw.easeIn(v);
        float grow = grown * (1.0f - VANISH_SHRINK * vEase);
        if (grow <= 0.004f) {
            return;
        }
        float alphaMul = this.fade * grown;

        // 偏移量：锚点缓动滞后 + 轻浮 + 消失上抽
        float offX = this.followX - AccuracyFx.targetX(this.owner);
        float offY = this.followY - AccuracyFx.targetY(this.owner)
                + AccuracyFx.bobOffset(this.time)
                + vEase * VANISH_RISE * sc();

        this.ring.set(AccuracyFx.RING);
        this.ringHi.set(AccuracyFx.RING_HI);
        this.core.set(AccuracyFx.CORE);
        this.link.set(AccuracyFx.LINK);

        int blendSrc = sb.getBlendSrcFunc();
        int blendDst = sb.getBlendDstFunc();
        // 全部是光：加色混合
        sb.setBlendFunction(770, 1);
        AccuracyDraw.target(sb, this.owner, offX, offY, alphaMul,
                grow, AccuracyFx.overdrive(GreenEffectRegistry.stacks(owner)),
                this.time, this.pulse,
                this.ring, this.ringHi, this.core, this.link);
        sb.setBlendFunction(blendSrc, blendDst);
    }
}
