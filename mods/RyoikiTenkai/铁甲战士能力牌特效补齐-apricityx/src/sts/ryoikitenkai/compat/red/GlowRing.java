package sts.ryoikitenkai.compat.red;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.helpers.ImageMaster;

/**
 * 用柔光点串出的一圈光带。
 * <p>
 * 游戏没有现成的「椭圆环」图元。直接拿一张柔光贴图拉长一圈会留下接缝，
 * 所以这里沿椭圆排布足够密的柔光点：相邻点互相重叠，整圈看起来是连续的光带，
 * 而且天然带柔边，比硬线更贴合原版特效的质感。
 * <p>
 * sweepPhase / sweepStrength 用来做「绕圈的高光」：与相位同向的点更亮，
 * 看起来像一道反光在金属环上跑动。
 */
final class GlowRing {
    private static final TextureAtlas.AtlasRegion DOT = ImageMaster.GLOW_SPARK_2;
    private static final Color SCRATCH = new Color();

    private GlowRing() {
    }

    /**
     * @param dotSize       单个柔光点的直径（像素，未乘 scale 的调用方自己处理）
     * @param count         点数，越多越连续
     * @param sweepPhase    高光当前所在的弧度
     * @param sweepStrength 高光强度，0 表示均匀
     */
    static void ellipse(SpriteBatch sb, float cx, float cy, float rx, float ry,
                        Color color, float alpha, float dotSize, int count,
                        float sweepPhase, float sweepStrength) {
        if (alpha <= 0.004f || count <= 0 || rx <= 0.5f || ry <= 0.5f) {
            return;
        }
        SCRATCH.set(color);
        for (int i = 0; i < count; ++i) {
            float angle = MathUtils.PI2 * i / count;
            float x = cx + rx * MathUtils.cos(angle);
            float y = cy + ry * MathUtils.sin(angle);
            float weight = 1.0f;
            if (sweepStrength > 0.0f) {
                float d = MathUtils.cos(angle - sweepPhase);
                if (d > 0.0f) {
                    float d2 = d * d;
                    weight += sweepStrength * d2 * d2;
                }
            }
            SCRATCH.a = Math.min(1.0f, alpha * weight);
            sb.setColor(SCRATCH);
            sb.draw(DOT, x - dotSize * 0.5f, y - dotSize * 0.5f, dotSize, dotSize);
        }
    }
}
