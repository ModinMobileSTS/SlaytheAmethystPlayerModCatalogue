package sts.ryoikitenkai.compat.red;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 一颗粒子：从身体下方向上涌出的荧光绿光点。
 * <p>
 * 「进化」的实际效果是抽到状态牌时额外抽牌，卡图是一股向上生长的绿色能量。
 * 所以它的常驻表现不做地面魔法阵，而是贴着身体上升的光点 —— 上升本身就是「成长/进化」的语义。
 * <p>
 * 全程加色混合、出生时快速亮起、到顶前淡出，配合左右轻微摆动，避免看起来像直线上浮的贴图。
 */
final class RisingMoteEffect extends AbstractGameEffect {
    private static final TextureAtlas.AtlasRegion IMG = ImageMaster.GLOW_SPARK_2;

    private final float baseX;
    private final float baseY;
    private final float rise;
    private final float size;
    private final float baseAlpha;
    private final float swayAmp;
    private final float swayFreq;
    private final float swayPhase;
    private final Color tint;

    private float time;

    /**
     * @param x    出生点 X
     * @param y    出生点 Y
     * @param rise 总共上升的高度
     * @param size 光点直径（未乘 scale 的调用方自己处理）
     */
    RisingMoteEffect(float x, float y, float rise, float size, Color color) {
        this.baseX = x;
        this.baseY = y;
        this.rise = Math.max(1.0f, rise);
        this.size = Math.max(1.0f, size);
        this.tint = new Color(color.r, color.g, color.b, 1.0f);
        this.baseAlpha = MathUtils.clamp(color.a, 0.1f, 1.0f);
        this.swayAmp = MathUtils.random(5.0f, 16.0f) * Settings.scale;
        this.swayFreq = MathUtils.random(1.7f, 3.4f);
        this.swayPhase = MathUtils.random(0.0f, MathUtils.PI2);
        this.startingDuration = this.duration = MathUtils.random(0.95f, 1.55f);
        this.renderBehind = true;
    }

    @Override
    public void update() {
        float dt = Gdx.graphics.getDeltaTime();
        this.time += dt;
        this.duration -= dt;
        if (this.duration <= 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.duration <= 0.0f) {
            return;
        }
        float progress = MathUtils.clamp(1.0f - this.duration / this.startingDuration, 0.0f, 1.0f);
        // 出生快速亮起，随后一路淡出（越往上越淡，像能量散进身体里）
        float alpha = Math.min(1.0f, progress * 5.0f) * (1.0f - progress) * (1.0f - progress * 0.25f);
        alpha *= this.baseAlpha;
        if (alpha <= 0.004f) {
            return;
        }
        float x = this.baseX + MathUtils.sin(this.time * this.swayFreq + this.swayPhase) * this.swayAmp * (0.35f + progress);
        float y = this.baseY + this.rise * Interpolation.sine.apply(progress);
        float s = this.size * (0.75f + 0.50f * progress);

        sb.setBlendFunction(770, 1);
        this.tint.a = alpha;
        sb.setColor(this.tint);
        sb.draw(IMG, x - s, y - s, s * 2.0f, s * 2.0f);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}
