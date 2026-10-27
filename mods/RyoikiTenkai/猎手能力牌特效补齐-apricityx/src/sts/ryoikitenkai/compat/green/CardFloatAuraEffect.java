package sts.ryoikitenkai.compat.green;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;

/**
 * 「工具贸易」与「蓄势待发」的常驻表现：身边浮着的绿色卡影。
 * <p>
 * 两个能力的实际效果都围绕卡牌：
 * <ul>
 *     <li>工具贸易：每回合抽一张、弃一张 → 身体两侧各浮着一张卡影，缓慢上下浮动，像手上一直在过牌；</li>
 *     <li>蓄势待发：回合末留一张牌 → 身体一侧浮着唯一一张卡影，缓慢呼吸，像被特意留住的那张牌。</li>
 * </ul>
 * 卡影用 {@link CardGlyph}（原版卡牌背景剪影），所以形状就是卡牌本身。
 * 卡影都放在身体侧面，避免被角色挡住。
 */
final class CardFloatAuraEffect extends PowerAuraEffect {
    /** true = 工具贸易，false = 蓄势待发。 */
    private final boolean tools;

    CardFloatAuraEffect(AbstractCreature owner, boolean tools) {
        super(owner,
                tools ? GreenEffectRegistry.TRADE_CARD : GreenEffectRegistry.RETAIN_CARD,
                tools ? GreenEffectRegistry.TRADE_ID : GreenEffectRegistry.RETAIN_ID);
        this.tools = tools;
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.fade <= 0.004f || this.owner == null) {
            return;
        }
        float b = breath(2.6f);
        float cx = BodyFx.centerX(this.owner);
        float cy = BodyFx.chestY(this.owner);
        float hw = BodyFx.halfWidth(this.owner);
        float scale = 0.34f * Settings.scale;

        sb.setBlendFunction(770, 1);
        if (this.tools) {
            // 两张卡影在两侧各浮一张：一张代表抽到的牌，一张代表要弃的牌
            float left = cy - BodyFx.height(this.owner) * 0.10f + MathUtils.sin(this.time * 1.4f) * 7.0f * Settings.scale;
            float right = cy - BodyFx.height(this.owner) * 0.04f + MathUtils.sin(this.time * 1.4f + 1.7f) * 7.0f * Settings.scale;
            CardGlyph.draw(sb, cx - hw * 1.15f, left, scale, -7.0f + 3.0f * b,
                    GreenEffectRegistry.silent(), this.fade * (0.24f + 0.10f * b));
            CardGlyph.draw(sb, cx + hw * 1.15f, right, scale, 7.0f - 3.0f * b,
                    GreenEffectRegistry.silentBright(), this.fade * (0.22f + 0.10f * b));
        } else {
            // 只留一张：贴着身体一侧，缓慢呼吸
            float y = cy - BodyFx.height(this.owner) * 0.08f + MathUtils.sin(this.time * 1.1f) * 6.0f * Settings.scale;
            CardGlyph.draw(sb, cx + hw * 0.78f, y, scale * 1.15f, MathUtils.sin(this.time * 0.8f) * 5.0f,
                    GreenEffectRegistry.silentBright(), this.fade * (0.34f + 0.16f * b));
        }
        sb.setBlendFunction(770, 771);
    }
}
