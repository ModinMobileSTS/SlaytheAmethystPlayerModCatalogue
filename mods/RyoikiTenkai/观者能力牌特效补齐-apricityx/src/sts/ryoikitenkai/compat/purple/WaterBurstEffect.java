package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 「如水」的爆发：水面冲起一道水柱，顶端散开水珠、底部压出水面亮斑。
 * <p>
 * 施放与「平静结束回合真正拿到格挡」都用它。刻意不用环状扩散 ——
 * 那是本补丁里其它能力共用的通用语言，用水柱才能让如水有自己的形态。
 * <p>
 * 坐标只来自 hitbox（{@link BodyFx}），不绑定骨骼。
 */
final class WaterBurstEffect extends AbstractGameEffect {
    private static final Color WATER = new Color(0.44f, 0.84f, 1.0f, 1.0f);

    private final float cx;
    private final float baseY;
    private final float height;
    private final float width;
    private final float delay;
    private final float life;
    private final float[] beadAngle = new float[7];
    private final float[] beadDist = new float[7];

    WaterBurstEffect(AbstractCreature owner, float power) {
        float hw = BodyFx.halfWidth(owner);
        float h = BodyFx.height(owner);
        this.cx = BodyFx.centerX(owner);
        this.baseY = BodyFx.groundY(owner) + 3.0f * Settings.scale;
        this.height = h * (0.42f + 0.22f * MathUtils.clamp(power, 0.0f, 1.4f));
        this.width = hw * 0.19f;
        this.delay = 0.0f;
        this.life = 0.72f;
        this.startingDuration = this.duration = this.life;
        this.renderBehind = false;
        for (int i = 0; i < this.beadAngle.length; ++i) {
            // 只往上半圈散，读作「溅上来」而不是「四面开花」
            this.beadAngle[i] = MathUtils.random(0.18f, 0.82f) * MathUtils.PI;
            this.beadDist[i] = MathUtils.random(0.35f, 1.0f);
        }
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
        if (this.duration > this.life || this.duration <= 0.0f) {
            return;
        }
        float p = MathUtils.clamp(1.0f - this.duration / this.life, 0.0f, 1.0f);
        // 冲起先快后慢（撞到空气阻力），后半段淡出
        float rise = Interpolation.pow2Out.apply(0.0f, 1.0f, Math.min(1.0f, p / 0.62f));
        float alpha = p < 0.10f ? p / 0.10f : 1.0f - (p - 0.10f) / 0.90f;
        alpha = MathUtils.clamp(alpha, 0.0f, 1.0f);
        if (alpha <= 0.01f) {
            return;
        }

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        // 主体：向上收窄的一束
        float topY = this.baseY + this.height * rise;
        int segs = 10;
        for (int i = 0; i < segs; ++i) {
            float t0 = (float) i / segs;
            float t1 = (float) (i + 1) / segs;
            float y0 = this.baseY + (topY - this.baseY) * t0;
            float y1 = this.baseY + (topY - this.baseY) * t1;
            float w = this.width * (1.0f - 0.72f * t0) * (1.0f - 0.35f * p);
            if (w <= 0.5f) {
                continue;
            }
            // 顶端那一段更亮，像水头
            Draw.glow(sb, this.cx, (y0 + y1) * 0.5f, w * 2.0f, (y1 - y0) + 3.0f, 0.0f,
                    WATER, alpha * (0.38f + 0.34f * t0));
        }

        // 顶端散开的水珠
        float beadA = alpha * (1.0f - rise * 0.35f);
        for (int i = 0; i < this.beadAngle.length; ++i) {
            float spread = this.beadDist[i] * (0.35f + 0.75f * rise) * this.height * 0.38f;
            float bx = this.cx + MathUtils.cos(this.beadAngle[i]) * spread;
            float by = topY + MathUtils.sin(this.beadAngle[i]) * spread * 0.75f;
            float s = Math.max(2.4f, this.width * 1.1f * this.beadDist[i] * (1.0f - 0.30f * p));
            Draw.glow(sb, bx, by, s, s, 0.0f, WATER, beadA * (0.45f + 0.45f * this.beadDist[i]));
        }

        // 底部水面亮斑
        float footRx = this.width * (5.2f + 3.2f * (1.0f - rise));
        Draw.glow(sb, this.cx, this.baseY, footRx, footRx * 0.26f, 0.0f, WATER, alpha * 0.42f);

        sb.setBlendFunction(src, dst);
    }

    @Override
    public void dispose() {
    }
}
