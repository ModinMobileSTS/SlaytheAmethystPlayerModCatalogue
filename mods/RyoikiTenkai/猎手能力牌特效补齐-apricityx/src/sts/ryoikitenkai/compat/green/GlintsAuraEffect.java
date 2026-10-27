package sts.ryoikitenkai.compat.green;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;

/**
 * 「精准」的常驻表现。
 * <p>
 * 精准本身没有持续动作，只是让手里的小刀更锋利，所以常驻部分刻意做得很轻：
 * 身体上零星闪过的绿色反光点（绑在骨骼上），加胸口一小团几乎看不见的绿光作为能力来源。
 * 真正「小刀命中」的反馈见 {@link GreenEffectRegistry#shivUsed}。
 */
final class GlintsAuraEffect extends PowerAuraEffect {
    private static final TextureAtlas.AtlasRegion GLOW = ImageMaster.GLOW_SPARK_2;

    private final Color scratch = new Color();
    private float timer;

    GlintsAuraEffect(AbstractCreature owner) {
        super(owner, GreenEffectRegistry.ACCURACY_CARD, GreenEffectRegistry.ACCURACY_ID);
        this.timer = MathUtils.random(0.05f, 0.25f);
    }

    @Override
    protected void tick(float dt) {
        this.timer -= dt;
        if (this.timer <= 0.0f) {
            this.timer = MathUtils.random(0.26f, 0.50f);
            GreenEffectRegistry.spawnGlints(this.owner, 1, 0.85f);
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.fade <= 0.004f || this.owner == null) {
            return;
        }
        float b = breath(2.4f);
        float cx = BodyFx.centerX(this.owner);
        float cy = BodyFx.chestY(this.owner);
        float s = Math.max(10.0f, 16.0f * Settings.scale) * (0.90f + 0.15f * b);

        sb.setBlendFunction(770, 1);
        this.scratch.set(GreenEffectRegistry.silent());
        this.scratch.a = this.fade * (0.10f + 0.06f * b);
        sb.setColor(this.scratch);
        sb.draw(GLOW, cx - s, cy - s, s * 2.0f, s * 2.0f);
        sb.setBlendFunction(770, 771);
    }
}
