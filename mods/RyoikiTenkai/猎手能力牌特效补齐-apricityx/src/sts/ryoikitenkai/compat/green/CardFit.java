package sts.ryoikitenkai.compat.green;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;

/**
 * 卡牌几何工具：<b>完全照原版卡牌发光的算法</b>，所以永远不会和卡牌脱开。
 * <h2>依据：{@code com.megacrit.cardcrawl.vfx.cardManip.CardGlowBorder}</h2>
 * <pre>
 * // 构造时按卡牌类型选背景剪影图元
 * switch (card.type) {
 *     case POWER:  img = ImageMaster.CARD_POWER_BG_SILHOUETTE;  break;
 *     case ATTACK: img = ImageMaster.CARD_ATTACK_BG_SILHOUETTE; break;
 *     default:     img = ImageMaster.CARD_SKILL_BG_SILHOUETTE;
 * }
 *
 * // update 里算缩放
 * scale = (1.0f + Interpolation.pow2Out.apply(0.03f, 0.11f, ...))
 *         * card.drawScale * Settings.scale;
 *
 * // render 里按同一个原点公式摆放，并用 card.angle 旋转
 * sb.draw(img,
 *         card.current_x + img.offsetX - img.originalWidth / 2f,
 *         card.current_y + img.offsetY - img.originalHeight / 2f,
 *         img.originalWidth / 2f - img.offsetX,
 *         img.originalHeight / 2f - img.offsetY,
 *         img.packedWidth, img.packedHeight,
 *         scale, scale, card.angle);
 * </pre>
 * 四个要点：
 * <ol>
 *   <li>用<b>卡牌类型对应的背景剪影图元</b>，不是写死的卡面尺寸
 *       （攻击 / 技能 / 能力的剪影尺寸各不相同）</li>
 *   <li>缩放 = {@code 倍率 × drawScale × Settings.scale}</li>
 *   <li>原点偏移用 {@code img.offsetX / img.offsetY}</li>
 *   <li>旋转一律用 {@code card.angle}</li>
 * </ol>
 * 之前漏了 {@code Settings.scale}、又用自写矩形估算，所以框会脱开。
 */
final class CardFit {
    /** 卡框相对卡面剪影的默认倍率。 */
    static final float GLOW_SCALE = 1.035f;

    private CardFit() {
    }

    /**
     * 取卡牌类型对应的背景剪影图元 —— 与原版 {@code CardGlowBorder} 完全一致。
     * 这个图元就是「卡牌的外轮廓」，自带 {@code offsetX/offsetY} 与
     * {@code packedWidth/Height}，按它摆放一定贴住卡牌。
     */
    static TextureAtlas.AtlasRegion silhouette(AbstractCard card) {
        if (card == null || card.type == null) {
            return ImageMaster.CARD_SKILL_BG_SILHOUETTE;
        }
        switch (card.type) {
            case POWER:
                return ImageMaster.CARD_POWER_BG_SILHOUETTE;
            case ATTACK:
                return ImageMaster.CARD_ATTACK_BG_SILHOUETTE;
            default:
                return ImageMaster.CARD_SKILL_BG_SILHOUETTE;
        }
    }

    /** 卡牌剪影在屏幕上的缩放 —— 与原版 {@code CardGlowBorder.update} 同一公式。 */
    static float scale(AbstractCard card, float mul) {
        float ds = Math.max(0.05f, card == null ? 0.7f : card.drawScale);
        return ds * Settings.scale * mul;
    }

    /** 卡牌剪影的屏幕半宽（世界单位）。 */
    static float frameHalfW(AbstractCard card) {
        return silhouette(card).packedWidth * scale(card, GLOW_SCALE) * 0.5f;
    }

    /** 卡牌剪影的屏幕半高（世界单位）。 */
    static float frameHalfH(AbstractCard card) {
        return silhouette(card).packedHeight * scale(card, GLOW_SCALE) * 0.5f;
    }

    /**
     * 顺着外法向 {@code (nx, ny)} 打到卡牌边缘上的落点。
     *
     * @param out 长度 2 的复用数组：[0] = x，[1] = y
     */
    static void edgePoint(AbstractCard card, float nx, float ny, float[] out) {
        if (card == null) {
            out[0] = 0.0f;
            out[1] = 0.0f;
            return;
        }
        out[0] = card.current_x + nx * frameHalfW(card);
        out[1] = card.current_y + ny * frameHalfH(card);
    }

    /**
     * 卡牌边缘上距离 {@code (px, py)} 最近的那条边的外法向（已归一化）。
     *
     * @param out 长度 2 的复用数组：[0] = nx，[1] = ny
     */
    static void nearestEdgeNormal(AbstractCard card, float px, float py, float[] out) {
        if (card == null) {
            out[0] = 0.0f;
            out[1] = -1.0f;
            return;
        }
        float hw = Math.max(0.001f, frameHalfW(card));
        float hh = Math.max(0.001f, frameHalfH(card));
        float dx = (px - card.current_x) / hw;
        float dy = (py - card.current_y) / hh;
        if (Math.abs(dx) >= Math.abs(dy)) {
            out[0] = dx >= 0.0f ? 1.0f : -1.0f;
            out[1] = 0.0f;
        } else {
            out[0] = 0.0f;
            out[1] = dy >= 0.0f ? 1.0f : -1.0f;
        }
    }

    /**
     * <b>照原版发光的摆法</b>把一张卡牌剪影图元画出来。
     * <p>
     * 同一个图元、同一个原点偏移、同一个 {@code drawScale × Settings.scale}、
     * 同一个 {@code card.angle} —— <b>不需要调用方旋转画布</b>。
     * 只要给的倍率与不透明度合适，画出来的就是与卡牌严丝合缝的一圈轮廓。
     *
     * @param mul   相对剪影的缩放倍率（1.0 = 与卡牌等大）
     * @param color 颜色副本（调用方自己构造，避免共享对象互相覆盖）
     */
    static void drawSilhouette(SpriteBatch sb, AbstractCard card,
                               float mul, Color color) {
        if (card == null || color == null) {
            return;
        }
        TextureAtlas.AtlasRegion img = silhouette(card);
        float s = scale(card, mul);
        sb.setColor(color);
        sb.draw(img,
                card.current_x + img.offsetX - img.originalWidth / 2.0f,
                card.current_y + img.offsetY - img.originalHeight / 2.0f,
                img.originalWidth / 2.0f - img.offsetX,
                img.originalHeight / 2.0f - img.offsetY,
                img.packedWidth, img.packedHeight,
                s, s, card.angle);
    }

    /**
     * 构造一份带不透明度的颜色副本。
     * <p>
     * <b>每次调用都返回新对象还是复用？</b>这里由调用方传入自己的
     * {@link Color} 来承接，避免两次连续调用互相覆盖（共享副本的经典坑）。
     */
    static Color tintInto(Color dst, Color base, float alpha) {
        dst.set(base);
        dst.a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        return dst;
    }
}
