package sts.ryoikitenkai.compat.green;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 常驻特效的公共骨架（本补丁四张猎手能力牌共用）。
 * <p>
 * 生命周期只有一件事：能力在 → 淡入并持续播；能力消失 / 玩家在设置里关掉 / 生物死亡 → 淡出并结束。
 * <h2>淡入淡出用时间驱动的缓动</h2>
 * <pre>
 *   fadeIn  0.75s，{@link GreenDraw#easeOut} —— 起手快、末端柔和，读作「铺开」
 *   fadeOut 0.95s，{@link GreenDraw#easeIn}  —— 起手慢、末端快，读作「散了」；且比淡入长
 * </pre>
 * 子类只读 {@link #fade}（0~1）当不透明度乘数。子类自己的<b>形态</b>变化
 * （缩放 / 上浮 / 缩扁 / 收拢）在 {@code tick} 里按 {@link #vanish()} 单独做 ——
 * 只降 alpha 会读成「被关掉」，而不是「它散了」。
 * <h2>出现缓动：本轮修掉的「不流畅」根因</h2>
 * 旧 {@link #swing} 用的是 {@code Interpolation.swingOut} 再 {@code clamp(0,1)}。
 * swingOut 会<b>冲到 1 以上</b>再回落，一 clamp 就把那段过冲切掉，
 * 末尾于是留下一段<b>死平</b>：看起来是「卡了一下才停」。
 * <p>
 * 现在 {@link #swing} 改走单调的 {@link GreenDraw#easeOut}（pow2Out，两端收敛），
 * 全程有变化、不会死平。需要「弹一下」的地方请用 {@link #overshoot}，
 * 它给出一个<b>有意的</b>过冲曲线（只回冲一次、不 clamp）。
 * <h2>子类在 {@code tick()} 里派生出来的特效必须走 {@link GreenEffectRegistry#queue}</h2>
 * 否则会在游戏遍历 {@code effectList} 时抛 {@code ConcurrentModificationException}。
 * <p>
 * 基类另外提供三件所有常驻都要用的东西：带阻尼的 {@link #smooth}、
 * 出现缓动 {@link #swing}/{@link #overshoot}、以及 {@link #pulse()} 供触发点推动常驻自己动一下。
 */
abstract class PowerAuraEffect extends AbstractGameEffect {
    /** 淡入时长（秒）。 */
    private static final float FADE_IN_TIME = 0.75f;
    /** 淡出时长（秒）。必须比淡入长：出现是「建立注意力」，消失是「让注意力跟得上松开」。 */
    private static final float FADE_OUT_TIME = 0.95f;
    /** 判定「已经完全消失」的阈值。 */
    private static final float DONE_EPS = 0.002f;

    protected final AbstractCreature owner;
    /** 父模组设置面板里的开关用的卡牌 ID。 */
    protected final String cardId;
    /** 能力的 ID（与 {@code AbstractPower.ID} 完全一致，不是卡牌 ID）。 */
    protected final String powerId;

    protected float time;
    /** 当前不透明度 0~1；子类直接拿它当乘数用。 */
    protected float fade;
    /** 淡入进度 0~1（线性时间），实际透明度由它套缓动得出。 */
    private float fadeInT;
    /** 淡出进度 0~1（线性时间）。 */
    private float fadeOutT;

    /** 由登记表置位：能力被移除 / 玩家关掉特效时淡出。 */
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

    /**
     * 消失进度 0~1，供子类做形态变化（缩放 / 上浮 / 缩扁 / 收拢）。
     * 只在淡出阶段增长，淡入阶段恒为 0，不会误触发。
     */
    protected float vanish() {
        return this.fadeOutT;
    }

    /** 出现进度 0~1，供子类的「张开 / 铺开」类动画使用。 */
    protected float appearT() {
        return this.fadeInT;
    }

    /** 角色是否还在场（死亡 / 逃走后不该继续画）。 */
    protected boolean alive() {
        return this.owner != null && !this.owner.isDeadOrEscaped();
    }

    private boolean active() {
        return !this.isEnd
                && GreenEffectRegistry.isOn(this.cardId)
                && this.owner != null
                && !this.owner.isDeadOrEscaped()
                && this.owner.hasPower(this.powerId);
    }

    @Override
    public void update() {
        float dt = com.badlogic.gdx.Gdx.graphics.getDeltaTime();
        this.time += dt;

        if (active()) {
            // 中途重新激活时把淡出进度一起退回去，避免从半透明突然跳回满亮
            if (this.fadeOutT > 0.0f) {
                this.fadeOutT = Math.max(0.0f, this.fadeOutT - dt / FADE_OUT_TIME);
            }
            if (this.fadeInT < 1.0f) {
                this.fadeInT = Math.min(1.0f, this.fadeInT + dt / FADE_IN_TIME);
            }
            this.fade = GreenDraw.easeOut(this.fadeInT);
            tick(dt);
            return;
        }

        if (this.fadeInT > 0.0f) {
            this.fadeInT = Math.max(0.0f, this.fadeInT - dt / FADE_IN_TIME);
        }
        if (this.fadeOutT < 1.0f) {
            this.fadeOutT = Math.min(1.0f, this.fadeOutT + dt / FADE_OUT_TIME);
        }
        this.fade = MathUtils.clamp(1.0f - GreenDraw.easeIn(this.fadeOutT), 0.0f, 1.0f);
        if (this.fade <= DONE_EPS && this.fadeOutT >= 1.0f) {
            this.fade = 0.0f;
            this.isDone = true;
        }
    }

    /** 每帧的额外逻辑（子类形态插值、状态量等）。 */
    protected void tick(float dt) {
    }

    /** 呼吸值 0~1。 */
    protected float breath(float period) {
        return 0.5f + 0.5f * MathUtils.sin(this.time * MathUtils.PI2 / period);
    }

    /** 带阻尼的平滑逼近（规范 §三.4：所有连续量都要平滑，不能直接取整）。 */
    protected static float smooth(float current, float target, float rise, float fall, float dt) {
        float speed = target > current ? rise : fall;
        return current + (target - current) * MathUtils.clamp(dt * speed, 0.0f, 1.0f);
    }

    /**
     * 出现缓动：<b>单调</b>的 pow2Out（起手快、末端慢，两端都有变化）。
     * <p>
     * 旧实现用 {@code swingOut} + {@code clamp(0,1)}，过冲被切掉后末尾死平 ——
     * 那正是「动画曲线不流畅」的来源。这里换成不会过冲的单调曲线。
     */
    protected static float swing(float t) {
        return GreenDraw.easeOut(t);
    }

    /**
     * 有意的过冲曲线：0 → 1 的过程中冲到约 1.05 再回到 1。只用于真正要「弹一下」的地方。
     * <p>
     * <b>不要</b>对它再 {@code clamp(0,1)}，否则又变成死平。
     */
    protected static float overshoot(float t) {
        float x = MathUtils.clamp(t, 0.0f, 1.0f);
        // 1 − (1−x)² 再叠一个一次性的 sin 过冲，收尾回到正好 1
        float base = GreenDraw.easeOut(x);
        return base + MathUtils.sin(x * MathUtils.PI) * 0.05f * (1.0f - x);
    }

    /** 屏幕缩放：固定像素一律乘它，否则真机上会整体缩水一圈。 */
    protected static float sc() {
        return Settings.scale;
    }

    @Override
    public abstract void render(SpriteBatch sb);

    @Override
    public void dispose() {
    }

    /** 供子类复用的贴图别名（本补丁只用「等宽实线」与「径向柔光」两种笔）。 */
    static final com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion LINE_IMG =
            ImageMaster.THICK_3D_LINE;
    static final com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion GLOW_IMG =
            ImageMaster.GLOW_SPARK_2;

    /** 子类需要一份私有颜色副本时的工厂（不要持有 {@link GreenDraw#tint} 的共享对象）。 */
    protected static Color color(float r, float g, float b) {
        return new Color(r, g, b, 1.0f);
    }
}
