package sts.ryoikitenkai.compat.green;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

import java.util.ArrayList;
import java.util.List;

/**
 * 「精准」的<b>常驻细曲线供能连线</b>。
 * <h2>本轮重做（上一版是最大败笔）</h2>
 * 截图里那几条粗米褐色带子横穿半个屏幕、压过角色与血量条，读作「电缆」。
 * 三个根因：
 * <ol>
 *   <li><b>被画笔下限撑粗</b>：{@code GreenDraw.stroke} 有 {@code MIN_LINE = 7px}
 *       的强下限（为了短刻线可见），而且内部还叠三层 —— 画长线时接近 20px</li>
 *   <li><b>几乎不弯</b>：控制点贴着连线放</li>
 *   <li><b>颜色偏脏</b>：暗金加色后是土褐色</li>
 * </ol>
 * 现在：细线（1.6~2.6px，不走下限）+ <b>两端亮中间淡</b>的包络 + 明显弯曲 +
 * 暖白色，最多 3 条。视觉上是「一张牌被一根细光牵着」，而不是满屏绳索。
 * <h2>层次</h2>
 * 走 {@code effectsQueue}（{@code renderBehind = true}）—— 手牌之下，
 * 所以即便穿过某张牌也只会被牌盖住，<b>永远不会挡住卡牌</b>。
 * <h2>性能</h2>
 * 每帧最多 3 条曲线 × 40 段：路径数组静态复用，牌列表复用，<b>不 new 对象</b>。
 */
final class AccuracyLinkEffect extends AbstractGameEffect {
    /** 连线光点游动一周的时间（秒）。 */
    private static final float MOTE_PERIOD = 2.6f;
    /** 基础不透明度。 */
    private static final float BASE_ALPHA = 0.80f;
    /** 出刀提亮的衰减速率（每秒）与幅度。 */
    private static final float FLASH_FALL = 2.9f;
    private static final float FLASH_GAIN = 0.30f;

    /** 复用的牌列表：每帧 clear + 重新收集，不分配。 */
    private final List<AbstractCard> shivs = new ArrayList<>();

    private final Color link = new Color();

    private float time;
    /** 出刀提亮强度 0~1，随时间衰减。 */
    private float flash;

    /** 由登记表置位。 */
    boolean isEnd;

    AccuracyLinkEffect() {
        this.startingDuration = 1.0f;
        this.duration = 1.0f;
        // 画在角色层：手牌之下 → 永远不会挡住卡牌
        this.renderBehind = true;
    }

    /** 打出一把小刀时调用：整组连线提亮一档。 */
    void flash() {
        this.flash = 1.0f;
    }

    @Override
    public void update() {
        float dt = Gdx.graphics.getDeltaTime();
        this.time += dt;
        if (this.flash > 0.0f) {
            this.flash = Math.max(0.0f, this.flash - dt * FLASH_FALL);
        }
        if (this.isEnd) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.isDone) {
            return;
        }
        // 没有层数就没有「供能」，什么都不画（规范 §2.9）
        if (GreenEffectRegistry.stacks(AbstractDungeon.player) <= 0) {
            return;
        }
        int n = AccuracyFx.collectShivs(this.shivs, AccuracyFx.MAX_LINKS);
        if (n <= 0) {
            // 手里没有小刀 → 没有可连的对象
            return;
        }

        // 标靶位置：与常驻标靶同一组几何函数（含轻浮相位）
        float srcX = AccuracyFx.targetX(AbstractDungeon.player);
        float srcY = AccuracyFx.targetY(AbstractDungeon.player)
                + AccuracyFx.bobOffset(this.time);
        float unit = AccuracyFx.radius(AbstractDungeon.player);
        float alpha = MathUtils.clamp(BASE_ALPHA + FLASH_GAIN * this.flash, 0.0f, 1.0f);

        this.link.set(AccuracyFx.LINK);

        int blendSrc = sb.getBlendSrcFunc();
        int blendDst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        for (int i = 0; i < n; ++i) {
            AbstractCard c = this.shivs.get(i);
            if (c == null) {
                continue;
            }
            // 每条线的光点错相，避免所有线同步
            float phase = (this.time / MOTE_PERIOD + i * 0.23f) % 1.0f;
            AccuracyLinkDraw.link(sb, srcX, srcY, c, unit, alpha, phase, this.link);
        }

        sb.setBlendFunction(blendSrc, blendDst);
    }

    @Override
    public void dispose() {
    }
}
