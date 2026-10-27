package sts.ryoikitenkai.compat.green;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;

/**
 * 「无限刀刃」的常驻表现：几道绿色刀影绕着身体缓慢盘旋。
 * <p>
 * 卡面就是一片飞刀，实际效果是每回合往手里塞一把小刀，所以常驻部分用「身体周围一直有刀在转」来表达，
 * 比单纯的光点更具体。刀身用原版 {@code THICK_3D_LINE} 拉长压薄，刀尖补一点柔光，
 * 再补两道更淡的拖影，避免看起来像一根会转的直线。
 * <p>
 * 刀数随层数增加（上限 5），层数越多画面越密。
 */
final class BladesAuraEffect extends PowerAuraEffect {
    private static final TextureAtlas.AtlasRegion BLADE = ImageMaster.THICK_3D_LINE;
    private static final TextureAtlas.AtlasRegion GLOW = ImageMaster.GLOW_SPARK_2;
    /** 盘旋角速度（弧度/秒）。 */
    private static final float SPIN = 0.85f;

    private final Color scratch = new Color();
    private float timer;

    BladesAuraEffect(AbstractCreature owner) {
        super(owner, GreenEffectRegistry.BLADES_CARD, GreenEffectRegistry.BLADES_ID);
        this.timer = MathUtils.random(0.3f, 0.8f);
    }

    @Override
    protected void tick(float dt) {
        this.timer -= dt;
        if (this.timer <= 0.0f) {
            this.timer = MathUtils.random(0.55f, 1.10f);
            GreenEffectRegistry.spawnGlints(this.owner, 1, 1.0f);
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.fade <= 0.004f || this.owner == null) {
            return;
        }
        float b = breath(1.9f);
        float cx = BodyFx.centerX(this.owner);
        float cy = BodyFx.chestY(this.owner) - BodyFx.height(this.owner) * 0.06f;
        float rx = BodyFx.halfWidth(this.owner) * 1.30f;
        float ry = rx * 0.38f;
        float len = BodyFx.height(this.owner) * 0.26f;
        float thick = Math.max(2.0f, 2.6f * Settings.scale);

        int count = Math.min(5, 2 + GreenEffectRegistry.stacksOf(this.owner, GreenEffectRegistry.BLADES_ID));
        float alphaBase = this.fade * (0.30f + 0.16f * b);

        sb.setBlendFunction(770, 1);
        for (int i = 0; i < count; ++i) {
            float angle = this.time * SPIN + MathUtils.PI2 * i / count;
            // 拖影：同一段圆弧上前几帧的位置，越远越淡
            for (int k = 2; k >= 1; --k) {
                float trailAngle = angle - 0.13f * k;
                drawBlade(sb, cx, cy, rx, ry, trailAngle, len, thick, alphaBase * (k == 1 ? 0.45f : 0.22f));
            }
            drawBlade(sb, cx, cy, rx, ry, angle, len, thick, alphaBase);
        }
        sb.setBlendFunction(770, 771);
    }

    private void drawBlade(SpriteBatch sb, float cx, float cy, float rx, float ry,
                           float angle, float len, float thick, float alpha) {
        if (alpha <= 0.004f) {
            return;
        }
        float px = cx + rx * MathUtils.cos(angle);
        float py = cy + ry * MathUtils.sin(angle);
        // 刀身沿运动切线方向摆放
        float rotation = MathUtils.atan2(ry * MathUtils.cos(angle), -rx * MathUtils.sin(angle)) * MathUtils.radiansToDegrees;

        this.scratch.set(GreenEffectRegistry.silent());
        this.scratch.a = alpha;
        sb.setColor(this.scratch);
        sb.draw(BLADE, px - len * 0.5f, py - thick * 0.5f,
                len * 0.5f, thick * 0.5f, len, thick,
                1.0f, 1.0f, rotation);

        // 刀尖的柔光
        float tipX = px + MathUtils.cos(rotation * MathUtils.degreesToRadians) * len * 0.5f;
        float tipY = py + MathUtils.sin(rotation * MathUtils.degreesToRadians) * len * 0.5f;
        float s = Math.max(4.0f, 7.0f * Settings.scale);
        this.scratch.a = Math.min(1.0f, alpha * 1.4f);
        sb.setColor(this.scratch);
        sb.draw(GLOW, tipX - s, tipY - s, s * 2.0f, s * 2.0f);
    }
}
