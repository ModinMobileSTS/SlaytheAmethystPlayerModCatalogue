package sts.ryoikitenkai.compat.red;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 「金属化」的常驻表现。
 * <p>
 * 金属化只是被动地每回合末给格挡，不该在画面上抢占注意力，所以常驻部分只有两件事：
 * <ol>
 *     <li>脚下一圈很淡的钢蓝光带（呼吸式起伏 + 绕圈高光），像一层护甲力场；</li>
 *     <li>身体上零星闪过的反光星芒，绑在骨骼上，随动画变形移动。</li>
 * </ol>
 * 真正「获得格挡」的反馈在 {@link RedEffectRegistry#blockMetallicize} 里另外播放。
 * <p>
 * 层数越多，反光越密（间隔按层数缩短），但上限受控，避免满屏闪烁。
 * <p>
 * update() 运行在游戏遍历 effectList 的过程中，所以派生出来的星芒必须走
 * {@link RedEffectRegistry#queue}（effectsQueue）。
 */
final class MetalAuraEffect extends AbstractGameEffect {
    private static final float BREATH_PERIOD = 2.6f;

    private final AbstractCreature owner;

    private float time;
    private float fade;
    private float glintTimer;

    /** 由登记表置位：能力被移除/玩家关掉特效时淡出。 */
    boolean isEnd;

    MetalAuraEffect(AbstractCreature owner) {
        this.owner = owner;
        this.startingDuration = 1.0f;
        this.duration = 1.0f;
        this.renderBehind = true;
        this.glintTimer = MathUtils.random(0.06f, 0.28f);
    }

    @Override
    public void update() {
        float dt = Gdx.graphics.getDeltaTime();
        this.time += dt;

        boolean active = !this.isEnd
                && RedEffectRegistry.isOn(RedEffectRegistry.METALLICIZE_ID)
                && this.owner != null
                && !this.owner.isDeadOrEscaped()
                && this.owner.hasPower(RedEffectRegistry.METALLICIZE_ID);

        if (!active) {
            this.fade = Math.max(0.0f, this.fade - dt * 2.4f);
            if (this.fade <= 0.001f) {
                this.fade = 0.0f;
                this.isDone = true;
            }
            return;
        }

        this.fade = Math.min(1.0f, this.fade + dt * 2.2f);

        this.glintTimer -= dt;
        if (this.glintTimer <= 0.0f) {
            int stacks = RedEffectRegistry.stacksOf(this.owner, RedEffectRegistry.METALLICIZE_ID);
            this.glintTimer = MathUtils.random(0.30f, 0.60f) * (4.0f / (3.0f + stacks));
            RedEffectRegistry.spawnMetalGlint(this.owner);
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
        float rx = BodyFx.halfWidth(this.owner) * 1.42f * (1.0f + 0.018f * breath);

        sb.setBlendFunction(770, 1);
        GlowRing.ellipse(sb, cx, gy + 5.0f * Settings.scale, rx, rx * 0.24f,
                RedEffectRegistry.steel(), this.fade * (0.09f + 0.07f * breath),
                10.0f * Settings.scale, 46, this.time * 0.85f, 1.9f);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}
