package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 常驻特效的公共骨架。
 * <p>
 * 观者这十来张能力牌都需要同一套生命周期：能力存在 → 淡入并持续播；
 * 能力消失/被玩家在设置里关掉/生物死亡 → 淡出并结束。
 * 差别只在「每帧做什么」与「每帧画什么」，所以抽成一层薄基类。
 * <h2>本轮修正：淡入淡出太生硬（用户反馈「人物身上的动画太快」）</h2>
 * 原来用的是<b>线性速度</b>：
 * <pre>
 * this.fade = Math.max(0.0f, this.fade - dt * 2.4f);   // 0.42s 线性掉到 0
 * </pre>
 * 两个问题：
 * <ol>
 *   <li><b>太快</b>：0.42s 从满亮到全无，眼睛来不及跟随</li>
 *   <li><b>不丝滑</b>：线性淡出在末端是「啪」地消失（不透明度骤降的感觉仍在），
 *       而且速度恒定，没有「先慢后快」或「先快后收」的质感</li>
 * </ol>
 * 现在改成<b>时间驱动的缓动</b>：
 * <pre>
 *   fadeIn  0.75s，pow2Out —— 起手快、末端柔和，读作「铺开」
 *   fadeOut 0.95s，pow2In  —— 起手慢、末端快，读作「散了」
 * </pre>
 * 两个方向用不同的曲线：入场要「稳」，退场要「散」。
 * 时长都翻倍以上，所以状态变化看得清清楚楚。
 * <p>
 * 子类仍然只读 {@link #fade}（0~1）当作不透明度乘数，不需要改自己的实现。
 * <p>
 * 子类在 tick() 里派生出来的特效必须走 {@link PurpleEffectRegistry#queue}，
 * 否则会在游戏遍历 effectList 时抛并发修改异常。
 */
abstract class PowerAuraEffect extends AbstractGameEffect {
    /** 淡入时长（秒）。比原来慢一倍，读作「铺开」。 */
    private static final float FADE_IN_TIME = 0.75f;
    /** 淡出时长（秒）。比原来慢一倍，读作「散了」。 */
    private static final float FADE_OUT_TIME = 0.95f;
    /** 判定「已经完全消失」的阈值。 */
    private static final float DONE_EPS = 0.002f;

    protected final AbstractCreature owner;
    /** 父模组设置面板里的开关用的卡牌 ID。 */
    protected final String cardId;
    /** 能力的 ID。 */
    protected final String powerId;

    protected float time;
    /** 当前不透明度 0~1；子类直接拿它当乘数用。 */
    protected float fade;
    /** 淡入进度 0~1（线性时间），实际透明度由它套缓动得到。 */
    private float fadeInT;
    /** 淡出进度 0~1（线性时间）。 */
    private float fadeOutT;

    /** 由登记表置位：能力被移除/玩家关掉特效时淡出。 */
    boolean isEnd;

    PowerAuraEffect(AbstractCreature owner, String cardId, String powerId) {
        this.owner = owner;
        this.cardId = cardId;
        this.powerId = powerId;
        this.startingDuration = 1.0f;
        this.duration = 1.0f;
        this.renderBehind = true;
        this.fade = 0.0f;
        this.fadeInT = 0.0f;
        this.fadeOutT = 0.0f;
    }

    /** 能力真正触发时调用，让常驻表现自己动一下；默认什么都不做。 */
    void pulse() {
    }

    private boolean active() {
        return !this.isEnd
                && PurpleEffectRegistry.isOn(this.cardId)
                && this.owner != null
                && !this.owner.isDeadOrEscaped()
                && this.owner.hasPower(this.powerId);
    }

    @Override
    public void update() {
        float dt = Gdx.graphics.getDeltaTime();
        this.time += dt;

        if (active()) {
            // ---- 淡入：时间驱动 + pow2Out（起手快、末端柔和）----
            // 回退时把 fadeOut 一起复位，这样中途重新激活不会从半透明突然跳回
            if (this.fadeOutT > 0.0f) {
                this.fadeOutT = Math.max(0.0f, this.fadeOutT - dt / FADE_OUT_TIME);
            }
            if (this.fadeInT < 1.0f) {
                this.fadeInT = Math.min(1.0f, this.fadeInT + dt / FADE_IN_TIME);
            }
            this.fade = MathUtils.clamp(
                    Interpolation.pow2Out.apply(0.0f, 1.0f, this.fadeInT), 0.0f, 1.0f);
            tick(dt);
            return;
        }

        // ---- 淡出：时间驱动 + pow2In（起手慢、末端快，读作「散了」）----
        if (this.fadeInT > 0.0f) {
            this.fadeInT = Math.max(0.0f, this.fadeInT - dt / FADE_IN_TIME);
        }
        if (this.fadeOutT < 1.0f) {
            this.fadeOutT = Math.min(1.0f, this.fadeOutT + dt / FADE_OUT_TIME);
        }
        float out = Interpolation.pow2In.apply(0.0f, 1.0f, this.fadeOutT);
        this.fade = MathUtils.clamp(1.0f - out, 0.0f, 1.0f);
        if (this.fade <= DONE_EPS && this.fadeOutT >= 1.0f) {
            this.fade = 0.0f;
            this.isDone = true;
        }
    }

    /** 每帧的额外逻辑（派生粒子等）。 */
    protected void tick(float dt) {
    }

    /** 呼吸值 0~1。 */
    protected float breath(float period) {
        return 0.5f + 0.5f * MathUtils.sin(this.time * MathUtils.PI2 / period);
    }

    @Override
    public abstract void render(SpriteBatch sb);

    @Override
    public void dispose() {
    }
}
