package sts.ryoikitenkai.compat.green;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.helpers.ImageMaster;

/**
 * 画一张「发光的卡影」。
 * <p>
 * 用的是原版 {@code CARD_SKILL_BG_SILHOUETTE} —— 也就是 {@code CardFlashVfx} 给卡牌加闪光时用的那张
 * 卡牌背景剪影，尺寸与偏移量都按卡牌的原生比例处理，所以形状就是卡牌本身，不需要自己拼矩形。
 * <p>
 * 「工具贸易」每回合抽一张弃一张、「蓄势待发」回合末留一张牌，两个能力的效果都是围绕卡牌转的，
 * 用卡影来表现比用抽象光点更贴合。
 */
final class CardGlyph {
    private static final TextureAtlas.AtlasRegion IMG = ImageMaster.CARD_SKILL_BG_SILHOUETTE;
    private static final Color SCRATCH = new Color();

    private CardGlyph() {
    }

    /**
     * @param cx        卡影中心 X
     * @param cy        卡影中心 Y
     * @param scale     缩放（调用方自己乘 Settings.scale）
     * @param rotation  旋转角度
     * @param alpha     中心亮度
     */
    static void draw(SpriteBatch sb, float cx, float cy, float scale, float rotation, Color color, float alpha) {
        if (alpha <= 0.004f || scale <= 0.0f) {
            return;
        }
        SCRATCH.set(color);
        // 外层柔光：把卡影托起来，避免看起来像贴纸
        SCRATCH.a = Math.min(1.0f, alpha * 0.45f);
        sb.setColor(SCRATCH);
        drawShape(sb, cx, cy, scale * 1.12f, rotation);
        // 内层卡影本体
        SCRATCH.a = Math.min(1.0f, alpha);
        sb.setColor(SCRATCH);
        drawShape(sb, cx, cy, scale, rotation);
    }

    private static void drawShape(SpriteBatch sb, float cx, float cy, float scale, float rotation) {
        sb.draw(IMG,
                cx + IMG.offsetX - IMG.originalWidth / 2.0f,
                cy + IMG.offsetY - IMG.originalHeight / 2.0f,
                IMG.originalWidth / 2.0f - IMG.offsetX,
                IMG.originalHeight / 2.0f - IMG.offsetY,
                IMG.packedWidth, IMG.packedHeight,
                scale, scale, rotation);
    }
}
