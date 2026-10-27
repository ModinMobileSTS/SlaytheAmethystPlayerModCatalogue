package sts.ryoikitenkai.compat.red;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 「进化」的常驻表现。
 * <p>
 * 卡面与能力图标都是荧光绿，实际效果是「抽到状态牌时额外抽牌」，也就是被状态牌顶一下就更旺盛，
 * 所以常驻部分做成贴着身体向上涌的绿色光点，而不是静态光环：
 * <ol>
 *     <li>脚下一圈绿色光带，作为能量的源头；</li>
 *     <li>光点从脚下沿身体上升、淡出，形成持续的上扬感。</li>
 * </ol>
 * 触发（抽到状态牌）时另有更明显的上冲，在 {@link RedEffectRegistry#drawEvolve} 里播放。
 * <p>
 * 层数越多光点越密。派生光点必须走 {@link RedEffectRegistry#queue}，
 * 否则会在游戏遍历 effectList 时抛并发修改异常。
 */
final class EvolveAuraEffect extends AbstractGameEffect {
    private static final float BREATH_PERIOD = 2.2f;

    private final AbstractCreature owner;

    private float time;
    private float fade;
    private float moteTimer;
    private float glintTimer;

    /** 由登记表置位：能力被移除/玩家关掉特效时淡出。 */
    boolean isEnd;

    EvolveAuraEffect(AbstractCreature owner) {
        this.owner = owner;
        this.startingDuration = 1.0f;
        this.duration = 1.0f;
        this.renderBehind = true;
        this.moteTimer = MathUtils.random(0.04f, 0.20f);
        this.glintTimer = MathUtils.random(0.50f, 1.10f);
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
            this.fade = Math.max(0.0f, this.fade - dt * 2.4f);
            if (this.fade <= 0.001f) {
                this.fade = 0.0f;
                this.isDone = true;
            }
            return;
        }

        this.fade = Math.min(1.0f, this.fade + dt * 2.2f);

        int stacks = RedEffectRegistry.stacksOf(this.owner, RedEffectRegistry.EVOLVE_ID);

        this.moteTimer -= dt;
        if (this.moteTimer <= 0.0f) {
            this.moteTimer = MathUtils.random(0.11f, 0.21f) * (4.0f / (3.0f + stacks));
            RedEffectRegistry.spawnBioMote(this.owner);
        }

        this.glintTimer -= dt;
        if (this.glintTimer <= 0.0f) {
            this.glintTimer = MathUtils.random(0.75f, 1.50f);
            RedEffectRegistry.spawnBioGlint(this.owner);
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.fade <= 0.004f || this.owner == null) {
            return;
        }
        float breath = 0.5f + 0.5f * MathUtils.sin(this.time * MathUtils.PI2 / BREATH_PERIOD);
        float cx = BodyFx.centerX(this.owner);
        float gy = BodyFx.groundY(this.owner);
        float rx = BodyFx.halfWidth(this.owner) * 1.30f * (1.0f + 0.030f * breath);

        sb.setBlendFunction(770, 1);
        GlowRing.ellipse(sb, cx, gy + 5.0f * Settings.scale, rx, rx * 0.26f,
                RedEffectRegistry.bio(), this.fade * (0.10f + 0.08f * breath),
                9.5f * Settings.scale, 44, -this.time * 0.90f, 2.0f);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}
