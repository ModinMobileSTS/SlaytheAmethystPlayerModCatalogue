package sts.ryoikitenkai.compat.green;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 常驻光环的公共骨架。
 * <p>
 * 四个能力（精准、无限刀刃、工具贸易、蓄势待发）都需要同一套生命周期：
 * 能力存在 → 淡入并持续播；能力消失/被玩家关掉/生物死亡 → 淡出并结束。
 * 差别只在「每帧做什么」与「每帧画什么」，所以抽成一层薄基类，子类只实现 tick/render。
 * <p>
 * 子类在 tick() 里派生出来的特效必须走 {@link GreenEffectRegistry#queue}，
 * 否则会在游戏遍历 effectList 时抛并发修改异常。
 */
abstract class PowerAuraEffect extends AbstractGameEffect {
    protected final AbstractCreature owner;
    /** 父模组设置面板里的开关用的卡牌 ID。 */
    protected final String cardId;
    /** 能力的 ID。 */
    protected final String powerId;

    protected float time;
    protected float fade;

    /** 由登记表置位：能力被移除/玩家关掉特效时淡出。 */
    boolean isEnd;

    PowerAuraEffect(AbstractCreature owner, String cardId, String powerId) {
        this.owner = owner;
        this.cardId = cardId;
        this.powerId = powerId;
        this.startingDuration = 1.0f;
        this.duration = 1.0f;
        this.renderBehind = true;
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
        float dt = Gdx.graphics.getDeltaTime();
        this.time += dt;
        // 用基类字段做淡入淡出：0~1
        if (!active()) {
            this.fade = Math.max(0.0f, this.fade - dt * 2.4f);
            if (this.fade <= 0.001f) {
                this.fade = 0.0f;
                this.isDone = true;
            }
            return;
        }
        this.fade = Math.min(1.0f, this.fade + dt * 2.2f);
        tick(dt);
    }

    /** 每帧的额外逻辑（派生粒子等）。 */
    protected void tick(float dt) {
    }

    /** 呼吸值 0~1，供子类让亮度/尺寸起伏。 */
    protected float breath(float period) {
        return 0.5f + 0.5f * MathUtils.sin(this.time * MathUtils.PI2 / period);
    }

    @Override
    public abstract void render(SpriteBatch sb);

    @Override
    public void dispose() {
    }
}
