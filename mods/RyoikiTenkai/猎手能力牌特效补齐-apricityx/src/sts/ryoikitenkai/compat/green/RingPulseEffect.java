package sts.ryoikitenkai.compat.green;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 一圈光带的扩散 / 收束。
 * <p>
 * 由 {@code fromRx} 与 {@code toRx} 的大小关系决定方向，所以扩散（能力起势）
 * 和收束（护住 / 收牌）共用一个类。
 * <h2>环不再用「密排小光点」拼</h2>
 * 旧实现靠 {@code GlowRing} 排一圈小圆点去<b>近似</b>一条线：
 * 点距一大就露出珠子感，点距一小又几十个 draw call，两头不讨好。
 * <p>
 * 现在改走 {@link GreenDraw#stroke}（柔光外晕 + 均匀内芯 + 高光）：
 * 环是真正连续的，而且是「一条有截面的光带」，不是一串珠子。
 * <h2>缓动</h2>
 * 扩散用 {@link GreenDraw#easeOut}（单调 pow2Out）；
 * 透明度用 {@code sin} 包络（进出两端都归零，不会硬切）。
 */
final class RingPulseEffect extends AbstractGameEffect {
    /** 环的分段数：32 段在 60~120px 半径上已看不出折线。 */
    private static final int SEG = 32;

    // 复用的路径点（不每次分配）
    private static final float[] XS = new float[SEG + 2];
    private static final float[] YS = new float[SEG + 2];

    private final float cx;
    private final float cy;
    private final float fromRx;
    private final float toRx;
    private final float squash;
    private final float dotSize;
    private final Color tint;
    private final float peakAlpha;
    private final float life;

    private float time;

    RingPulseEffect(float cx, float cy, float fromRx, float toRx, float squash,
                    Color color, float alpha, float dotSize, int count,
                    float delay, float life) {
        this.cx = cx;
        this.cy = cy;
        this.fromRx = fromRx;
        this.toRx = toRx;
        this.squash = MathUtils.clamp(squash, 0.05f, 1.0f);
        this.tint = new Color(color.r, color.g, color.b, 1.0f);
        this.peakAlpha = MathUtils.clamp(alpha, 0.0f, 1.0f);
        // dotSize 现在当作「环的厚度」用（保留参数名以免改所有调用点）
        this.dotSize = Math.max(1.0f, dotSize);
        this.life = Math.max(0.12f, life);
        this.startingDuration = this.duration = this.life + Math.max(0.0f, delay);
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
        if (this.duration > this.life) {
            return;
        }
        float progress = MathUtils.clamp(1.0f - this.duration / this.life, 0.0f, 1.0f);
        // sin 包络：0 → 峰值 → 0，两端都归零
        float alpha = MathUtils.sin(progress * MathUtils.PI) * this.peakAlpha;
        if (alpha <= 0.004f) {
            return;
        }
        float rx = MathUtils.lerp(this.fromRx, this.toRx, GreenDraw.easeOut(progress));
        float ry = rx * this.squash;
        if (rx <= 1.0f || ry <= 0.6f) {
            return;
        }

        // 生成闭合椭圆路径
        for (int i = 0; i <= SEG; ++i) {
            float ang = MathUtils.PI2 * i / SEG;
            XS[i] = this.cx + MathUtils.cos(ang) * rx;
            YS[i] = this.cy + MathUtils.sin(ang) * ry;
        }

        int blendSrc = sb.getBlendSrcFunc();
        int blendDst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);
        // dotSize 作为厚度：细环也要有体积
        GreenDraw.stroke(sb, XS, YS, SEG + 1,
                Math.max(GreenDraw.MIN_LINE * 1.6f, this.dotSize * 1.4f),
                this.tint, alpha);
        sb.setBlendFunction(blendSrc, blendDst);
    }

    @Override
    public void dispose() {
    }
}
