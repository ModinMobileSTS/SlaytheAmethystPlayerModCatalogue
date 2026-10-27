package sts.ryoikitenkai.compat.red;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 一圈光带的呼吸/收束。
 * <p>
 * 既能从脚下向外扩散（「能量起势」），也能从外向内收拢（「护甲合拢/格挡」），
 * 由 fromRx 与 toRx 的大小关系决定，所以一个类就够两个能力用。
 * <p>
 * 环本身交给 {@link GlowRing}：密排柔光点组成的环比硬线更贴合原版特效的质感，
 * 还能带上绕圈跑动的高光。
 */
final class RingPulseEffect extends AbstractGameEffect {
    private final float cx;
    private final float cy;
    private final float fromRx;
    private final float toRx;
    /** 椭圆压扁比：地面环用 0.2~0.3，胸口环用 0.9~1.0。 */
    private final float squash;
    private final float dotSize;
    private final int count;
    private final Color tint;
    private final float peakAlpha;
    private final float life;
    /** 高光绕环的速度。 */
    private final float sweepSpeed;

    private float time;

    RingPulseEffect(float cx, float cy, float fromRx, float toRx, float squash,
                    Color color, float alpha, float dotSize, int count, float delay, float life) {
        this.cx = cx;
        this.cy = cy;
        this.fromRx = fromRx;
        this.toRx = toRx;
        this.squash = MathUtils.clamp(squash, 0.05f, 1.0f);
        this.tint = new Color(color.r, color.g, color.b, 1.0f);
        this.peakAlpha = MathUtils.clamp(alpha, 0.0f, 1.0f);
        this.dotSize = Math.max(1.0f, dotSize);
        this.count = Math.max(8, count);
        this.life = Math.max(0.12f, life);
        this.sweepSpeed = 6.0f;
        this.startingDuration = this.duration = this.life + Math.max(0.0f, delay);
        this.renderBehind = true;
    }

    @Override
    public void update() {
        this.time += Gdx.graphics.getDeltaTime();
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration <= 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.duration > this.life) {
            return;
        }
        float progress = MathUtils.clamp(1.0f - this.duration / this.life, 0.0f, 1.0f);
        float alpha = MathUtils.sin(progress * MathUtils.PI) * this.peakAlpha;
        if (alpha <= 0.004f) {
            return;
        }
        float rx = MathUtils.lerp(this.fromRx, this.toRx, Interpolation.pow2Out.apply(progress));

        sb.setBlendFunction(770, 1);
        GlowRing.ellipse(sb, this.cx, this.cy, rx, rx * this.squash,
                this.tint, alpha, this.dotSize, this.count,
                this.time * this.sweepSpeed, 1.4f);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}
