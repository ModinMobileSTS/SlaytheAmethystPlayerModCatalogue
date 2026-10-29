package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 「确立基础」<b>回合末结算</b>时的一次性演出：<b>刻线依次点亮，每张被降费的牌各收一道金光</b>。
 * <p>
 * <h2>这个动作对应什么</h2>
 * 原版 {@code EstablishmentPowerAction.update()} 做的事是：
 * <pre>
 * for (AbstractCard c : AbstractDungeon.player.hand.group) {
 *     if (!c.selfRetain &amp;&amp; !c.retain) continue;
 *     c.modifyCostForCombat(-this.discountAmount);
 * }
 * </pre>
 * 也就是「<b>把手里每一张保留牌的费用压下去</b>」。所以画面分两段、方向相反：
 * <ol>
 *   <li><b>压实</b>：碑往下一沉，砸出地面冲击环 ——「基础又夯了一遍」</li>
 *   <li><b>下行</b>：金光从碑面刻线渗出，<b>每张被降费的牌各飞一道</b>，
 *       落到牌上时牌自己亮一下 ——「这笔便宜送到了<b>这几张</b>上」</li>
 * </ol>
 * <h2>本轮重做：金光不再飞向一个死点</h2>
 * 旧实现把终点写成 {@code handX()/handY()}（屏幕水平中点偏下）。三个问题：
 * <ul>
 *   <li>手牌是<b>扇形排布</b>的，中点那张未必是保留牌，甚至可能没有牌</li>
 *   <li>被降费的牌有具体的 N 张、具体位置，而所有金光都飞向同一点</li>
 *   <li>于是「<b>哪几张</b>变便宜了」这条最重要的信息完全没传达</li>
 * </ul>
 * 现在每张牌各收一道，终点读 {@code card.current_x / card.current_y}
 * （每张牌自己的实时位置），数量与位置都由机制决定（规范 §2.7）。
 * <p>
 * 并且<b>落点自己也要亮一下</b>：只有「光飞过去 + 牌亮了」两件事都发生，
 * 玩家才能确定<b>是这一张</b>变便宜了。
 * <h2>强度由机制决定（规范 §2.9）</h2>
 * <ul>
 *   <li><b>有保留牌</b>：完整的压实 + 逐牌金光 + 碎石 + 刻线依次点亮</li>
 *   <li><b>没有保留牌</b>：只空敲一下 —— 碑微沉、极淡一圈，
 *       <b>刻线不亮、金光完全不发</b>。因为原版此刻确实什么都没发生</li>
 * </ul>
 * <h2>时间线（有保留牌时总长 1.40s，{@code FAST_MODE} × 0.72）</h2>
 * <pre>
 *   0.00–0.34  压实   碑下沉 + 地面冲击环外扩
 *   0.12–0.62  迸屑   从碑基向外甩出石屑
 *   0.20–0.78  刻度   碑面刻线自上而下依次点亮
 *   0.34–1.20  下行   每张牌各一道金光沿弧线飞过去，落点亮一下
 *   0.90–1.40  余韵   刻线金光缓慢回落到常态亮度
 * </pre>
 * <h2>时间轴自洽（规范 §三 补充章节）</h2>
 * 本类用<b>绝对秒</b>驱动，所以所有阶段时长都乘同一个 {@code timeScale}，
 * 且 {@code life} 由时间轴<b>反推</b>（最后一道金光飞完 + 余韵），不是拍脑袋给的。
 * <p>
 * 旧实现写的是 {@code Settings.FAST_MODE ? base * 0.58f : base} ——
 * 只缩放了总时长，各阶段阈值却用没缩放的绝对秒，属于规范里明确点出的**时间轴错位**坑。
 * 现在统一走 {@code timeScale}。
 */
final class FoundationBurstEffect extends AbstractGameEffect {
    /** 有保留牌时的主时长（标准模式）。 */
    private static final float LIFE_HIT_BASE = 1.40f;
    /** 没有保留牌时的「空敲」时长，明显更短更淡。 */
    private static final float LIFE_MISS_BASE = 0.58f;
    /** 单道金光飞完的时长。 */
    private static final float FLOW_BASE = 0.52f;
    /** 相邻两道金光之间的错开。 */
    private static final float FLOW_STEP_BASE = 0.055f;
    /** 刻线依次点亮的总跨度。 */
    private static final float MARK_SPAN_BASE = 0.58f;
    /** 落点闪光的时长（标准模式）。 */
    private static final float HIT_BASE = 0.46f;

    private final AbstractCreature owner;
    /** 真正被降费的牌数：0 表示这次没生效。 */
    private final int hits;
    /** 降费点数 = 刻线道数。 */
    private final int marks;
    /** 0 = 完全没生效，1 = 有牌被降费。 */
    private final float strength;
    /** 被降费的那几张牌的实时坐标（扁平数组）。 */
    private final float[] targets;
    private final float life;
    private final float timeScale;
    private final float flowTime;
    private final float flowStep;
    private final float markSpan;
    private final float hitTime;

    private float time;

    FoundationBurstEffect(AbstractCreature owner, int hits, int marks) {
        this.owner = owner;
        this.hits = Math.max(0, hits);
        this.marks = MathUtils.clamp(marks, 1, FoundationFx.MAX_MARKS);
        this.strength = this.hits <= 0
                ? 0.0f
                : MathUtils.clamp(0.55f + 0.15f * this.hits, 0.0f, 1.0f);

        // 一次性把落点快照下来：结算过程中手牌顺序会变，
        // 但「这次被降费的是哪几张」在这一刻已经确定
        this.targets = this.hits <= 0 ? new float[0] : FoundationFx.retainedX();

        this.timeScale = Settings.FAST_MODE ? 0.72f : 1.0f;
        this.flowTime = FLOW_BASE * this.timeScale;
        this.flowStep = FLOW_STEP_BASE * this.timeScale;
        this.markSpan = MARK_SPAN_BASE * this.timeScale;
        this.hitTime = HIT_BASE * this.timeScale;

        float base = (this.hits <= 0 ? LIFE_MISS_BASE : LIFE_HIT_BASE) * this.timeScale;
        // life 由时间轴反推：最后一道金光必须飞完，再加落点闪光的尾巴
        if (this.hits > 0) {
            int n = Math.max(1, this.targets.length / 2);
            float lastStart = 0.34f * this.timeScale + (n - 1) * this.flowStep;
            float needed = lastStart + this.flowTime + this.hitTime * 0.6f;
            base = Math.max(base, needed);
        }
        this.life = base;
        this.startingDuration = this.duration = this.life;
        this.renderBehind = false;
    }

    @Override
    public void update() {
        float dt = Gdx.graphics.getDeltaTime();
        this.time += dt;
        this.duration -= dt;
        if (this.duration <= 0.0f) {
            this.isDone = true;
        }
    }

    private float progress() {
        return MathUtils.clamp(1.0f - this.duration / Math.max(0.001f, this.life),
                0.0f, 1.0f);
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.duration <= 0.0f || this.owner == null) {
            return;
        }
        float p = this.progress();
        float punch = 0.85f + 0.30f * (1.0f - p);

        // 碑本体的一次性演出锚点：与常驻共用同一组几何
        float bx = FoundationFx.monoX(this.owner);
        float baseY = FoundationFx.monoBaseY(this.owner);
        float bh = FoundationFx.monoHeight(this.owner);
        float bw = FoundationFx.monoWidth(this.owner);

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        // ================= 1. 压实：碑下沉 + 地面冲击环 =================
        // 下沉量：有牌时 3.5% 碑高，空敲时只有 1.2%
        float sink = Interpolation.pow2Out.apply(0.0f, 1.0f,
                MathUtils.clamp(p / 0.55f, 0.0f, 1.0f))
                * bh * (this.hits > 0 ? 0.035f : 0.012f);
        // 冲击环从碑基向外扩，越扩越淡（pow2In 收尾，不是线性）
        float ringQ = MathUtils.clamp(p / 0.58f, 0.0f, 1.0f);
        if (ringQ > 0.0f && ringQ < 1.0f) {
            float ringEase = Interpolation.pow3Out.apply(ringQ);
            float rr = bw * MathUtils.lerp(2.6f, 8.0f, ringEase);
            float ra = (1.0f - Interpolation.pow2In.apply(0.0f, 1.0f, ringQ))
                    * (0.10f + 0.62f * this.strength) * punch;
            Draw.glow(sb, bx, baseY - sink, rr * 1.9f, rr * 0.55f, 0.0f,
                    FoundationFx.STONE_DARK, ra * 0.55f);
            Draw.arc(sb, bx, baseY - sink, rr, rr * 0.30f, 0.0f, 360.0f,
                    Math.max(5.0f * Settings.scale, MathUtils.lerp(3.4f, 1.2f, ringQ)),
                    FoundationFx.STONE, ra, 26);
        }
        // 碑基的「压痕」：一圈暗色，读作被砸实了
        if (this.hits > 0) {
            float dentQ = MathUtils.clamp((p - 0.12f) / 0.55f, 0.0f, 1.0f);
            if (dentQ > 0.0f && dentQ < 1.0f) {
                float da = (1.0f - Interpolation.pow2In.apply(0.0f, 1.0f, dentQ))
                        * this.strength * 0.50f * punch;
                Draw.arc(sb, bx, baseY - sink, bw * 2.0f, bw * 0.58f,
                        0.0f, 360.0f,
                        Math.max(5.0f * Settings.scale, bw * 0.34f),
                        FoundationFx.STONE_DARK, da, 22);
            }
        }

        // ================= 2. 迸屑 =================
        if (this.hits > 0) {
            float q = MathUtils.clamp((p - 0.12f) / 0.50f, 0.0f, 1.0f);
            if (q > 0.0f) {
                FoundationDraw.debris(sb, bx, baseY,
                        bw * 6.4f,
                        (1.0f - q * 0.4f) * this.strength * punch, q,
                        7 + this.hits * 2, 2.3f + this.hits);
            }
        }

        // ================= 3. 刻线自上而下依次点亮 =================
        // 这里是「降几费」的核心读数：一道一道亮起来
        if (this.hits > 0) {
            float q = MathUtils.clamp((p - 0.10f) / Math.max(0.001f,
                    this.markSpan / Math.max(0.001f, this.life)), 0.0f, 1.0f);
            // 已亮道数：按进度铺开，读作「一道一道压下去」
            int lit = MathUtils.clamp(
                    (int) Math.ceil(q * this.marks - 0.001f), 0, this.marks);
            // 点亮瞬间整组刻线过曝，之后回落到常态
            float glowNow = MathUtils.clamp(
                    0.35f + 0.65f * (1.0f - Interpolation.pow2In.apply(0.0f, 1.0f,
                            MathUtils.clamp(p / 0.85f, 0.0f, 1.0f))), 0.0f, 1.0f);
            FoundationDraw.marks(sb, bx, baseY - sink, bh, bw,
                    baseY - sink + bh, this.strength * punch,
                    glowNow, lit, this.marks, 1.0f);
        }

        // ================= 4. 下行：每张被降费的牌各收一道金光 =================
        if (this.hits > 0 && this.targets.length >= 2) {
            // 碑面刻线所在高度（金光从这里出发）
            float fromY = baseY - sink + bh * 0.52f;
            int n = this.targets.length / 2;
            float startAt = 0.34f * this.timeScale;
            for (int i = 0; i < n; ++i) {
                float t0 = startAt + i * this.flowStep;
                float local = this.time - t0;
                if (local <= 0.0f) {
                    continue;
                }
                float q = MathUtils.clamp(local / Math.max(0.001f, this.flowTime),
                        0.0f, 1.0f);
                float tx = this.targets[i * 2];
                float ty = this.targets[i * 2 + 1];

                // 飞行段：先快后慢，读作「流过去」
                float flowEase = Interpolation.pow2Out.apply(0.0f, 1.0f, q);
                float flowA = this.strength * punch
                        * (1.0f - Interpolation.pow2In.apply(0.0f, 1.0f,
                        MathUtils.clamp(q * 0.75f, 0.0f, 1.0f)));
                FoundationDraw.tollFlow(sb, bx, fromY, tx, ty,
                        flowA, flowEase, 3);

                // 落点闪光：牌自己被点亮一下
                float hitQ = MathUtils.clamp((local - this.flowTime)
                        / Math.max(0.001f, this.hitTime), 0.0f, 1.0f);
                if (hitQ > 0.0f && hitQ < 1.0f) {
                    FoundationDraw.tollHit(sb, tx, ty,
                            Math.max(18.0f * Settings.scale, bw * 2.2f),
                            this.strength * punch, hitQ);
                }
            }
        }

        sb.setBlendFunction(src, dst);
    }

    @Override
    public void dispose() {
    }
}
