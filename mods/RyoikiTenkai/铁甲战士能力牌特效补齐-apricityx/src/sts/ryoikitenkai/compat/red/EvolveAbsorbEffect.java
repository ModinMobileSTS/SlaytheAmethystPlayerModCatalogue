package sts.ryoikitenkai.compat.red;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 「进化」<b>抽到状态牌</b>时的演出：<b>脏牌被吸走，换成额外抽牌</b>。
 * <p>
 * <h2>这个动作对应什么</h2>
 * 原版 {@code EvolvePower.onCardDraw} 做的事是：
 * <pre>
 * if (card.type == STATUS &amp;&amp; !owner.hasPower("No Draw")) {
 *     this.flash();
 *     addToBot(new DrawCardAction(owner, this.amount));
 * }
 * </pre>
 * 也就是「<b>抽到一张脏牌，它反而让我多抽几张</b>」。所以画面是一条完整因果链：
 * <ol>
 *   <li><b>标记</b>：那张状态牌周围箍上一圈绿弧 ——「是这一张」</li>
 *   <li><b>吸收</b>：从它拉一道上弧汇进芽尖 ——「被吃进去」</li>
 *   <li><b>转化</b>：芽窜一截、分叉张开（由 {@link EvolveGrowthEffect} 负责）</li>
 *   <li><b>兑现</b>：芽尖绽出光苞并抛向抽牌堆 ——「换成了额外抽牌」</li>
 * </ol>
 * 本类只负责 1 和 2，3 与 4 在 {@link EvolveGrowthEffect} 里，
 * 两者由 {@link EvolveFx#onStatusDrawn} 同时投递。
 * <h2>关键：标记必须跟着那张牌</h2>
 * {@code AbstractPlayer.draw(int)} 的顺序（已按字节码核实）：
 * <pre>
 * c.current_x = CardGroup.DRAW_PILE_X;    // ← 先摆到抽牌堆位置
 * c.current_y = CardGroup.DRAW_PILE_Y;
 * c.drawScale = 0.12f;                    // ← 此时还很小，正在飞向手牌
 * c.triggerWhenDrawn();
 * this.hand.addToHand(c);
 * this.drawPile.removeTopCard();
 * for (AbstractPower p : this.powers) p.onCardDraw(c);   // ← 我们挂在这里
 * </pre>
 * 若只按「钩子被调用那一刻的 {@code current_x/current_y}」画一圈，
 * 那张牌会在约 0.5s 内飞到手上，而标记会<b>留在抽牌堆角落</b>，
 * 玩家看到的是「牌堆响了一下」，而不是「这一张牌被吃掉了」。
 * <p>
 * 所以这里<b>持有 {@link AbstractCard} 的引用，每帧读它的 {@code current_x/current_y}
 * 与 {@code drawScale}</b>：标记会随着牌一起飞，尺寸也随 {@code drawScale}
 * 从 0.12 长到 0.75，圈的大小与牌同步。
 * <p>
 * 这是规范 §2.18 的直接应用：<b>光要落在「效果作用的那个具体对象」上，
 * 而且那个对象移动时，光要跟着移动。</b>
 * <h2>为什么终点读芽尖而不是「身体上方」</h2>
 * 旧写法用 {@code groundY + height × 0.80} 当落点，那个点与真实的芽尖
 * 差了一截（芽尖 = 根部 0.30×身高 + 芽高 0.56×身高 = 0.86×身高），
 * 光会停在芽上方或下方。现在统一走 {@link EvolveFx#tipX} / {@link EvolveFx#tipY}，
 * 与常驻、施放共用同一个几何函数（规范 §六「共享常量」）。
 * <h2>落点兑现</h2>
 * 光点汇入芽尖后，尖端<b>溅出 {@code amount} 枚小光苞</b>，方向向上散开。
 * 数量 = 额外抽几张：<b>一枚光苞 = 一张额外抽到的牌</b>（规范 §2.7 一一对应）。
 * <h2>时间线（总长 0.86s，{@code FAST_MODE} × 0.72）</h2>
 * <pre>
 *   0.00–0.62  标记   绿弧箍紧目标牌，跟着它飞
 *   0.10–0.86  吸收   一串光点沿上弧汇进芽尖
 *   0.72–0.86  兑现   芽尖溅出 amount 枚光苞
 * </pre>
 * <h2>频次</h2>
 * 一回合可能好几次 —— <b>完全静音、不震屏</b>（规范 §2.11）。
 */
final class EvolveAbsorbEffect extends AbstractGameEffect {
    /** 标记闪动的时长（标准模式，会乘 timeScale）。 */
    private static final float MARK_BASE = 0.62f;
    /** 吸收弧的飞行时长。 */
    private static final float FLY_BASE = 0.76f;
    /** 吸收弧相对标记的起动延迟。 */
    private static final float FLY_DELAY_BASE = 0.10f;
    /** 光点汇入后开始溅出的进度。 */
    private static final float BURST_FROM = 0.72f;
    /** 标记环相对牌宽的半径比例。 */
    private static final float RING_RATIO = 0.62f;

    private final AbstractCard card;
    private final float fallbackX;
    private final float fallbackY;
    private final float toX;
    private final float toY;
    /** 要溅出几枚光苞（= 额外抽牌数）。 */
    private final int count;
    private final float life;
    private final float markTime;
    private final float flyTime;
    private final float flyDelay;

    private float time;

    /**
     * @param owner 能力持有者（用于兜底坐标，避免 owner 被移除后弧线跳到屏幕上）
     * @param card  刚被抽到的那张状态牌；每帧读它的实时位置
     * @param toX   芽尖 X
     * @param toY   芽尖 Y
     * @param count 要溅出几枚光苞
     */
    EvolveAbsorbEffect(com.megacrit.cardcrawl.core.AbstractCreature owner,
                       AbstractCard card, float toX, float toY, int count) {
        this.card = card;
        this.fallbackX = owner != null ? BodyFx.centerX(owner) : toX;
        this.fallbackY = owner != null ? EvolveFx.baseY(owner) : toY;
        this.toX = toX;
        this.toY = toY;
        this.count = MathUtils.clamp(count, 1, 4);

        this.timeScale = Settings.FAST_MODE ? 0.72f : 1.0f;
        this.markTime = MARK_BASE * this.timeScale;
        this.flyTime = FLY_BASE * this.timeScale;
        this.flyDelay = FLY_DELAY_BASE * this.timeScale;

        // life 由时间轴反推：吸收弧必须飞完，标记也要走完
        this.life = Math.max(this.markTime, this.flyDelay + this.flyTime);
        this.startingDuration = this.duration = this.life;
        // 弧线会横跨屏幕下半部分，必须画在手牌之上
        this.renderBehind = false;
    }

    private float timeScale;

    @Override
    public void update() {
        float dt = Gdx.graphics.getDeltaTime();
        this.time += dt;
        this.duration -= dt;
        if (this.duration <= 0.0f) {
            this.isDone = true;
        }
    }

    /** 那张牌此刻的位置：牌飞向手牌时跟着它。 */
    private float fromX() {
        return this.card != null ? this.card.current_x : this.fallbackX;
    }

    private float fromY() {
        return this.card != null ? this.card.current_y : this.fallbackY;
    }

    /**
     * 标记环的半径基准：<b>牌此刻的实际宽度</b>。
     * <p>
     * 牌从 {@code drawScale = 0.12} 长到 0.75，环也随之长大 ——
     * 这样「箍住这一张」在任何时刻都成立，不会出现环比牌大十倍的情况。
     */
    private float ringSize() {
        float cardW = this.card != null
                ? AbstractCard.IMG_WIDTH * this.card.drawScale
                : EvolveFx.px(36.0f);
        return MathUtils.clamp(cardW * RING_RATIO, EvolveFx.px(16.0f), EvolveFx.px(190.0f));
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.duration <= 0.0f) {
            return;
        }
        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        float fx = fromX();
        float fy = fromY();

        // ---- 1. 标记：那张状态牌周围箍一圈，跟着它飞 ----
        if (this.time < this.markTime) {
            float q = MathUtils.clamp(this.time / Math.max(0.001f, this.markTime),
                    0.0f, 1.0f);
            EvolveDraw.markCard(sb, fx, fy, ringSize(), 1.0f, q);
        }

        // ---- 2. 吸收：从牌位置拉一道上弧汇进芽尖 ----
        float local = this.time - this.flyDelay;
        if (local > 0.0f && local < this.flyTime) {
            float q = MathUtils.clamp(local / Math.max(0.001f, this.flyTime),
                    0.0f, 1.0f);
            EvolveDraw.absorb(sb, fx, fy, this.toX, this.toY, 1.0f, q);

            // ---- 落点兑现：汇入芽尖时溅出 count 枚小光苞 ----
            if (q > BURST_FROM) {
                float hit = (q - BURST_FROM) / (1.0f - BURST_FROM);
                for (int i = 0; i < this.count; ++i) {
                    float ang = -MathUtils.PI * 0.5f
                            + (i - (this.count - 1) * 0.5f) * 0.34f;
                    float dist = EvolveFx.px(26.0f)
                            * Interpolation.pow2Out.apply(0.0f, 1.0f, hit);
                    float sx = this.toX + MathUtils.cos(ang) * dist;
                    float sy = this.toY + MathUtils.sin(ang) * dist;
                    float s = Math.max(EvolveFx.px(6.0f), EvolveFx.px(4.5f))
                            * (1.0f - 0.4f * hit);
                    Draw.glow(sb, sx, sy, s * 2.8f, s * 2.8f, 0.0f,
                            EvolveFx.BIO, 0.34f * (1.0f - hit));
                    Draw.glow(sb, sx, sy, s * 1.1f, s * 1.1f, 0.0f,
                            EvolveFx.CORE, 0.70f * (1.0f - hit));
                }
            }
        }

        sb.setBlendFunction(src, dst);
    }

    @Override
    public void dispose() {
    }
}
