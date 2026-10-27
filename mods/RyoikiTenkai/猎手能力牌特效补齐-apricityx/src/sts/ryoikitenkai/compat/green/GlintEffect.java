package sts.ryoikitenkai.compat.green;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.Bone;
import com.esotericsoftware.spine.Skeleton;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 一点绿色反光：十字星芒 + 一小团柔光。
 * <p>
 * 位置可以绑在一根骨头上（骨骼动画形变会让反光贴着身体移动），
 * 也可以给固定坐标作兜底。生命很短，读起来就是「光闪了一下」。
 */
final class GlintEffect extends AbstractGameEffect {
    private static final TextureAtlas.AtlasRegion STAR = ImageMaster.TINY_STAR;
    private static final TextureAtlas.AtlasRegion GLOW = ImageMaster.GLOW_SPARK_2;

    private final Skeleton skeleton;
    private final Bone bone;
    private final float fallbackX;
    private final float fallbackY;
    private final Color tint;
    private final float size;

    private float spin;

    GlintEffect(Skeleton skeleton, Bone bone, float fallbackX, float fallbackY, Color color, float size) {
        this.skeleton = bone != null ? skeleton : null;
        this.bone = this.skeleton != null ? bone : null;
        this.fallbackX = fallbackX;
        this.fallbackY = fallbackY;
        this.tint = new Color(color.r, color.g, color.b, 1.0f);
        this.size = Math.max(1.0f, size);
        this.spin = MathUtils.random(0.0f, 360.0f);
        this.startingDuration = this.duration = MathUtils.random(0.36f, 0.58f);
        this.renderBehind = false;
    }

    private float x() {
        return this.bone != null ? this.bone.getWorldX() + this.skeleton.getX() : this.fallbackX;
    }

    private float y() {
        return this.bone != null ? this.bone.getWorldY() + this.skeleton.getY() : this.fallbackY;
    }

    @Override
    public void update() {
        float dt = Gdx.graphics.getDeltaTime();
        this.spin += dt * 120.0f;
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
        float alpha = MathUtils.sin(progress * MathUtils.PI);
        if (alpha <= 0.004f) {
            return;
        }
        float s = this.size * (0.70f + 0.55f * progress);
        float cx = x();
        float cy = y();

        sb.setBlendFunction(770, 1);
        this.tint.a = alpha * 0.30f;
        sb.setColor(this.tint);
        sb.draw(GLOW, cx - s * 1.9f, cy - s * 1.9f, s * 3.8f, s * 3.8f);
        this.tint.a = alpha;
        sb.setColor(this.tint);
        sb.draw(STAR, cx - s, cy - s, s, s, s * 2.0f, s * 2.0f, 1.0f, 1.0f, this.spin);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}
