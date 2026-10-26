package sts.ryoikitenkai.compat.red;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 从角色脚下向外扩散的一圈地面环。
 * <p>
 * 贴在水平面上画成椭圆，基准线用 owner.hb.y —— 判定框底边就是脚底线，
 * 所以环永远在脚下，不会浮到身上。
 * 扩散时环由粗变细、由亮转淡，并带一圈光晕一起外扩。
 * <p>
 * 环本身用 {@link VfxDraw#line}（实心贴图）绘制，线条连续无断口。
 */
public class EvolveGroundRing extends AbstractGameEffect {
    private static final Color RING = new Color(0.50f, 1.0f, 0.76f, 0.0f);
    private static final Color FILL = new Color(0.34f, 0.98f, 0.66f, 0.0f);
    private static final Color SCRATCH = new Color();
    private static final TextureAtlas.AtlasRegion GLOW = ImageMaster.GLOW_SPARK_2;

    private final AbstractCreature owner;
    private final float fallbackCx;
    private final float fallbackY;

    private final float startRx;
    private final float endRx;
    private final float baseAlpha;
    private final float baseThickness;

    /** 延迟启动秒数，用来让多圈环依次扩散。 */
    private float delay;

    public EvolveGroundRing(AbstractCreature owner, float startRx, float endRx,
                            float lifetime, float baseAlpha, float baseThickness) {
        this(owner, startRx, endRx, lifetime, baseAlpha, baseThickness, 0.0f);
    }

    public EvolveGroundRing(AbstractCreature owner, float startRx, float endRx,
                            float lifetime, float baseAlpha, float baseThickness, float delay) {
        this.renderBehind = true;
        this.owner = owner;
        this.startRx = startRx;
        this.endRx = endRx;
        this.duration = lifetime;
        this.startingDuration = lifetime;
        this.baseAlpha = baseAlpha;
        this.baseThickness = baseThickness;
        this.delay = delay;
        this.fallbackCx = owner != null ? owner.hb.cX : Settings.WIDTH * 0.25f;
        this.fallbackY = owner != null ? owner.hb.y : Settings.HEIGHT * 0.25f;
    }

    private float cx() {
        return this.owner != null ? this.owner.hb.cX : this.fallbackCx;
    }

    private float groundY() {
        return this.owner != null ? this.owner.hb.y : this.fallbackY;
    }

    @Override
    public void update() {
        float dt = Gdx.graphics.getDeltaTime();
        if (this.delay > 0.0f) {
            this.delay -= dt;
            return;
        }
        this.duration -= dt;
        if (this.duration <= 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.delay > 0.0f) {
            return;
        }
        float t = 1.0f - this.duration / this.startingDuration;
        if (t <= 0.0f || t >= 1.0f) {
            return;
        }
        float grow = Interpolation.pow2Out.apply(0.0f, 1.0f, t);
        float rx = this.startRx + (this.endRx - this.startRx) * grow;
        float ry = rx * 0.32f;
        // 出现极快，之后随扩散淡出
        float alpha = this.baseAlpha * (t < 0.08f ? t / 0.08f : 1.0f - (t - 0.08f) / 0.92f);
        float thickness = this.baseThickness * (1.0f - 0.50f * grow);

        sb.setBlendFunction(770, 1);

        // 光晕随环一起外扩
        SCRATCH.set(FILL);
        SCRATCH.a = alpha * 0.38f;
        sb.setColor(SCRATCH);
        sb.draw(GLOW, cx() - rx, groundY() - ry,
                GLOW.packedWidth * 0.5f, GLOW.packedHeight * 0.5f,
                GLOW.packedWidth, GLOW.packedHeight,
                (rx * 2.0f) / GLOW.packedWidth, (ry * 2.0f) / GLOW.packedHeight, 0.0f);

        // 实心线画环：连续无断口
        VfxDraw.ellipseRing(sb, cx(), groundY(), rx, ry, 48, RING, alpha, thickness, 0.0f, 0.0f, true);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}
