package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.helpers.ImageMaster;

/**
 * 「如水」的一滴落水。
 * <p>
 * 从身体上部按重力加速落向脚下水面。位置全部来自 hitbox 的比例采样（{@link BodyFx}），
 * 不涉及骨骼，因此与观者的待机动作、换肤、自定义角色都无关。
 */
final class WaterDropEffect extends AbstractGameEffect {
    private final float x;
    private final float fromY;
    private final float toY;
    private final float size;
    private final float stretch;
    private final Color tint;

    WaterDropEffect(AbstractCreature owner) {
        this.x = BodyFx.bodyX(owner, 0.66f);
        this.fromY = BodyFx.bodyY(owner, 0.62f, 0.98f);
        this.toY = BodyFx.groundY(owner) + 3.0f * Settings.scale;
        this.size = MathUtils.random(2.0f, 3.4f) * Settings.scale;
        this.stretch = MathUtils.random(1.6f, 2.4f);
        this.tint = new Color(0.44f, 0.84f, 1.0f, 1.0f);
        // 下落很短，读起来是「一滴」而不是「一束」
        this.startingDuration = this.duration = MathUtils.random(0.26f, 0.40f);
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
        // 加速下落，像真的被重力拉下去
        float fall = Interpolation.pow2In.apply(0.0f, 1.0f, progress);
        float y = this.fromY + (this.toY - this.fromY) * fall;
        float alpha = progress > 0.82f ? (1.0f - progress) / 0.18f : 1.0f;
        if (alpha <= 0.01f) {
            return;
        }
        // 越落越细长
        float w = this.size * (1.0f - 0.28f * progress);
        float h = this.size * this.stretch * (0.55f + 0.75f * progress);

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);
        Draw.glow(sb, this.x, y + h, w * 2.0f, h * 2.0f, 0.0f, this.tint, alpha * 0.85f);
        sb.setBlendFunction(src, dst);
    }

    @Override
    public void dispose() {
    }
}
