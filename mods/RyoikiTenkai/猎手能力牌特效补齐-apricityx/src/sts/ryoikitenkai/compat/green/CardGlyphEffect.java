package sts.ryoikitenkai.compat.green;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 一次性的卡影：从起点飘向终点并淡出。
 * <p>
 * 「工具贸易」的抽/弃、「蓄势待发」的留牌都只是「一张牌在某处进/出」，
 * 所以做成一张卡影沿固定方向飘过，配合 {@link RingPulseEffect} 就是完整的一次反馈。
 */
final class CardGlyphEffect extends AbstractGameEffect {
    private final float startX;
    private final float startY;
    private final float driftX;
    private final float driftY;
    private final float baseScale;
    private final float baseRotation;
    private final float spin;
    private final float peakAlpha;
    private final Color tint;

    CardGlyphEffect(float startX, float startY, float driftX, float driftY, float baseScale,
                    float baseRotation, float spin, Color color, float alpha, float duration) {
        this.startX = startX;
        this.startY = startY;
        this.driftX = driftX;
        this.driftY = driftY;
        this.baseScale = Math.max(0.01f, baseScale);
        this.baseRotation = baseRotation;
        this.spin = spin;
        this.tint = new Color(color.r, color.g, color.b, 1.0f);
        this.peakAlpha = MathUtils.clamp(alpha, 0.0f, 1.0f);
        this.startingDuration = this.duration = Math.max(0.12f, duration);
        this.renderBehind = false;
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
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
        float alpha = MathUtils.sin(progress * MathUtils.PI) * this.peakAlpha;
        if (alpha <= 0.004f) {
            return;
        }
        float x = this.startX + this.driftX * progress;
        float y = this.startY + this.driftY * progress;
        float s = this.baseScale * (0.85f + 0.25f * progress);

        sb.setBlendFunction(770, 1);
        CardGlyph.draw(sb, x, y, s, this.baseRotation + this.spin * progress, this.tint, alpha);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}
