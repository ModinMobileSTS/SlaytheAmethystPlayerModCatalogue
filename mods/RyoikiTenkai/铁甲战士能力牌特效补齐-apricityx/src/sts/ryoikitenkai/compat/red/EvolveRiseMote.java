package sts.ryoikitenkai.compat.red;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 从角色脚边升起的一颗光点。
 * 出生位置限定在脚下范围内，上升距离不到角色膝盖，叠加多颗也不会碰到上半身。
 */
public class EvolveRiseMote extends AbstractGameEffect {
    private static final TextureAtlas.AtlasRegion IMG = ImageMaster.GLOW_SPARK_2;
    private static final Color MOTE = new Color(0.55f, 1.0f, 0.78f, 0.0f);

    private final float x;
    private final float y;
    private final float riseSpeed;
    private final float drift;

    private float curY;

    public EvolveRiseMote(AbstractCreature owner) {
        this.duration = MathUtils.random(0.5f, 0.9f);
        this.startingDuration = this.duration;
        this.scale = MathUtils.random(0.42f, 0.70f) * Settings.scale;
        float cx = owner != null ? owner.hb.cX : Settings.WIDTH * 0.25f;
        float gy = owner != null ? owner.hb.y : Settings.HEIGHT * 0.25f;
        float w = owner != null ? owner.hb.width : 120.0f * Settings.scale;
        this.x = cx + MathUtils.random(-w * 0.48f, w * 0.48f);
        this.y = gy + MathUtils.random(-6.0f, 6.0f) * Settings.scale;
        this.curY = this.y;
        this.riseSpeed = MathUtils.random(52.0f, 104.0f) * Settings.scale;
        this.drift = MathUtils.random(-16.0f, 16.0f) * Settings.scale;
        this.rotation = MathUtils.random(0.0f, 360.0f);
        this.renderBehind = true;
    }

    @Override
    public void update() {
        float dt = Gdx.graphics.getDeltaTime();
        this.duration -= dt;
        if (this.duration <= 0.0f) {
            this.isDone = true;
            return;
        }
        this.curY += this.riseSpeed * dt;
        this.rotation += 40.0f * dt;
    }

    @Override
    public void render(SpriteBatch sb) {
        float t = 1.0f - this.duration / this.startingDuration;
        float alpha = t < 0.2f
                ? Interpolation.fade.apply(0.0f, 1.0f, t / 0.2f)
                : Interpolation.fade.apply(1.0f, 0.0f, (t - 0.2f) / 0.8f);
        if (alpha <= 0.01f) {
            return;
        }
        float drawX = this.x + this.drift * t;
        sb.setBlendFunction(770, 1);
        MOTE.a = alpha;
        sb.setColor(MOTE);
        sb.draw(IMG, drawX - IMG.packedWidth * 0.5f, this.curY - IMG.packedHeight * 0.5f,
                IMG.packedWidth * 0.5f, IMG.packedHeight * 0.5f,
                IMG.packedWidth, IMG.packedHeight,
                this.scale, this.scale, this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}
