package sts.ryoikitenkai.compat.red;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 一道斜掠而过的金属高光。
 * <p>
 * 用原版 {@code STRIKE_LINE_2}（同 {@code UpgradeShineParticleEffect} 用的那张亮条）画两条重叠的细光带，
 * 从身体左上方扫到右下方：读起来就是「光在打磨过的甲面上滑过去」。
 * <p>
 * 刻意不做重力、不做溅射，这样它只服务于「金属」这一个语义，和火花/星芒分工明确。
 */
final class SheenEffect extends AbstractGameEffect {
    private static final TextureAtlas.AtlasRegion STREAK = ImageMaster.STRIKE_LINE_2;
    /** 光带真正滑行的时间；构造里的 delay 只是错峰。 */
    private static final float SWEEP_TIME = 0.46f;

    private final float cx;
    private final float groundY;
    private final float halfWidth;
    private final float bodyHeight;
    private final Color tint;
    private final float delay;
    private final float life;

    SheenEffect(float cx, float groundY, float halfWidth, float bodyHeight, Color color, float delay) {
        this.cx = cx;
        this.groundY = groundY;
        this.halfWidth = Math.max(1.0f, halfWidth);
        this.bodyHeight = Math.max(1.0f, bodyHeight);
        this.tint = new Color(color.r, color.g, color.b, 1.0f);
        this.delay = Math.max(0.0f, delay);
        this.life = SWEEP_TIME;
        this.startingDuration = this.duration = this.life + this.delay;
        this.renderBehind = false;
        // 倾斜得像一道斜光，而不是水平横线
        this.rotation = -62.0f;
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
        if (this.duration > this.life) {
            return;
        }
        float progress = MathUtils.clamp(1.0f - this.duration / this.life, 0.0f, 1.0f);
        float alpha = MathUtils.sin(progress * MathUtils.PI);
        if (alpha <= 0.004f) {
            return;
        }

        float x = MathUtils.lerp(this.cx - this.halfWidth * 1.15f, this.cx + this.halfWidth * 1.15f, progress);
        float y = this.groundY + this.bodyHeight * (0.55f + (0.5f - progress) * 0.22f);
        float len = this.bodyHeight * 0.70f;
        float thick = Math.max(2.4f, len * 0.045f);

        sb.setBlendFunction(770, 1);
        // 外层柔光
        this.tint.a = alpha * 0.40f;
        sb.setColor(this.tint);
        drawStreak(sb, x, y, len, thick);
        // 内层更亮更细，叠出「双高光」
        this.tint.a = alpha * 0.55f;
        sb.setColor(this.tint);
        drawStreak(sb, x - this.halfWidth * 0.10f, y + this.bodyHeight * 0.05f, len * 0.82f, thick * 0.45f);
        sb.setBlendFunction(770, 771);
    }

    private void drawStreak(SpriteBatch sb, float x, float y, float len, float thick) {
        sb.draw(STREAK, x - len * 0.5f, y - thick * 0.5f,
                len * 0.5f, thick * 0.5f, len, thick,
                1.0f, 1.0f, this.rotation);
    }

    @Override
    public void dispose() {
    }
}
