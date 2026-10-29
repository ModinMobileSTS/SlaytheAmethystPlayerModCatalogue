package sts.ryoikitenkai.compat.red;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 「进化」的一次性演出：<b>芽从背后抽出、逐股长出、芽尖绽开光苞</b>。
 * <p>
 * <h2>同一段代码，两种强度</h2>
 * <ul>
 *   <li><b>施放</b>（{@code strength = 1.0}）：原版 {@code Evolve.use()}
 *       <b>没有任何 VFX</b>，所以必须完整演：根部先亮一小片，
 *       然后各股芽依次从背后抽上来，最后芽尖绽开光苞并抛向抽牌堆</li>
 *   <li><b>抽到状态牌</b>（{@code strength = 0.72}）：芽<b>向上窜一截</b>、
 *       分叉张开、绽出光苞抛向抽牌堆。
 *       比施放弱，因为这件事一回合可能发生好几次（规范 §2.11）</li>
 * </ul>
 * 两种用同一套画笔与同一条时间轴，只靠 {@code strength} 区分强度 ——
 * 这样不会出现「两处各写一遍常量」的老坑（规范 §六「共享常量」）。
 * <h2>时间线（施放，总长 1.72s，{@code FAST_MODE} × 0.72）</h2>
 * <pre>
 *   0.00–0.20  起势   根部一团暗绿光斑亮起（「要长了」）
 *   0.16–1.24  抽长   各股芽依次从根部抽到顶（逐股错开 0.14s）
 *   0.90–1.72  兑现   芽尖绽出光苞，抛向右下角的抽牌堆，余弦淡出
 * </pre>
 * <h2>抛出数量 = 额外抽牌数</h2>
 * 光苞总数严格等于 {@code amount}（<b>一枚光苞 = 一张额外抽到的牌</b>），
 * 而不是「每股各抛 amount 枚」—— 后者会变成 {@code 股数 × amount}，
 * 与机制对不上（规范 §2.7）。分配方式是按 {@code k % 股数} 轮流从各股芽尖抛出，
 * 这样 2 枚光苞在 3 股芽上会从其中两股出发，总量仍然准确。
 * <h2>为什么「抛出」目标读 {@code CardGroup.DRAW_PILE_X/Y}</h2>
 * 额外抽的牌确实来自抽牌堆，所以光苞往抽牌堆方向飞，
 * 而不是往「手牌区中心」那种死点飞。这是从观者「确立基础」那次返工学到的
 * （规范 §2.18）：<b>光要落到「效果作用的具体对象」上。</b>
 * <h2>队列</h2>
 * 走 {@code queueTop}：抛出段会飞到屏幕角落，且整段贴着角色与手牌区，
 * 走 {@code effectsQueue} 会被手牌盖住。
 */
final class EvolveGrowthEffect extends AbstractGameEffect {
    /** 起势时长（标准模式，会乘 timeScale）。 */
    private static final float FORM_BASE = 0.20f;
    /** 单股抽长时长。 */
    private static final float GROW_BASE = 0.94f;
    /** 逐股错开。 */
    private static final float STEP_BASE = 0.14f;
    /** 光苞飞行时长。 */
    private static final float TOSS_BASE = 0.52f;

    private final AbstractCreature owner;
    private final int sprouts;
    private final int tossCount;
    private final float strength;
    private final float life;
    private final float formTime;
    private final float growTime;
    private final float step;
    private final float tossTime;

    /** 每股芽尖的 X / Y，绘制时填充，抛出段复用。 */
    private final float[] tipX = new float[EvolveFx.MAX_SPROUTS];
    private final float[] tipY = new float[EvolveFx.MAX_SPROUTS];

    private float time;

    /**
     * @param owner    能力持有者
     * @param sprouts  要画几股芽
     * @param toss     要抛出几枚光苞（= 额外抽牌数，总数严格等于它）
     * @param strength 1.0 = 施放；0.72 = 触发
     */
    EvolveGrowthEffect(AbstractCreature owner, int sprouts, int toss, float strength) {
        this.owner = owner;
        this.sprouts = MathUtils.clamp(sprouts, 2, EvolveFx.MAX_SPROUTS);
        this.tossCount = MathUtils.clamp(toss, 1, 4);
        this.strength = MathUtils.clamp(strength, 0.2f, 1.2f);

        float timeScale = Settings.FAST_MODE ? 0.72f : 1.0f;
        this.formTime = FORM_BASE * timeScale;
        this.growTime = GROW_BASE * timeScale;
        this.step = STEP_BASE * timeScale;
        this.tossTime = TOSS_BASE * timeScale;

        // life 由时间轴反推：最后一股芽长完 + 最后一枚光苞飞完，不留硬切
        float lastGrowStart = this.formTime + (this.sprouts - 1) * this.step;
        this.life = lastGrowStart + this.growTime + this.tossTime;
        this.startingDuration = this.duration = this.life;
        this.renderBehind = true;
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
        float punch = 0.88f + 0.26f * (1.0f - this.progress()) * this.strength;
        float h = EvolveFx.sproutHeight(this.owner);
        int n = this.sprouts;

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        // ---- 起势：根部一团暗绿光斑 ----
        if (this.time < this.formTime) {
            float q = MathUtils.clamp(this.time / Math.max(0.001f, this.formTime),
                    0.0f, 1.0f);
            float grow = Interpolation.pow2Out.apply(0.0f, 1.0f, q);
            for (int i = 0; i < n; ++i) {
                float bx = EvolveFx.sproutX(this.owner, i, n);
                float by = EvolveFx.sproutBaseY(this.owner, i, n);
                Draw.glow(sb, bx, by,
                        EvolveFx.px(38.0f) * grow, EvolveFx.px(20.0f) * grow,
                        0.0f, EvolveFx.DEEP, 0.34f * q * punch);
                Draw.glow(sb, bx, by,
                        EvolveFx.px(16.0f) * grow, EvolveFx.px(10.0f) * grow,
                        0.0f, EvolveFx.BIO, 0.22f * q * punch);
            }
        }

        // ---- 逐股抽长，并记下每股的芽尖 ----
        for (int i = 0; i < n; ++i) {
            this.tipX[i] = 0.0f;
            this.tipY[i] = -1.0f;

            float t0 = this.formTime + i * this.step;
            if (this.time < t0) {
                continue;
            }
            float local = this.time - t0;
            float q = MathUtils.clamp(local / Math.max(0.001f, this.growTime),
                    0.0f, 1.0f);
            // pow2Out：前快后慢，读作「顶出来后站稳」
            float show = Interpolation.pow2Out.apply(0.0f, 1.0f, q);

            // 分叉在抽长的后段才张开
            float fork = MathUtils.clamp((show - 0.58f) / 0.42f, 0.0f, 1.0f);

            // 收尾淡出：长完之后一起淡掉，不会硬切
            float tailQ = MathUtils.clamp(
                    (local - this.growTime) / Math.max(0.001f, this.tossTime),
                    0.0f, 1.0f);
            float fade = tailQ <= 0.0f ? 1.0f : EvolveDraw.cosineOut(tailQ);
            if (fade <= 0.006f) {
                continue;
            }

            float bx = EvolveFx.sproutX(this.owner, i, n);
            float by = EvolveFx.sproutBaseY(this.owner, i, n);
            float lean = EvolveFx.sproutLean(i, n);
            float sway = MathUtils.sin(this.time * 2.4f + i * 1.7f) * 0.12f;

            EvolveDraw.sprout(sb, bx, by, h, lean, show, fork,
                    fade * punch, sway);

            // 芽尖：与画笔里的弯曲公式一致（bend = lean×0.42 + sway，横向 × height×0.34）
            this.tipX[i] = bx + (lean * 0.42f + sway) * h * 0.34f;
            this.tipY[i] = by + h * show;
        }

        // ---- 兑现：光苞总数 = 额外抽牌数，轮流从各股芽尖抛出 ----
        for (int k = 0; k < this.tossCount; ++k) {
            int i = k % n;
            if (this.tipY[i] < 0.0f) {
                continue;
            }
            float tk = this.time - (this.formTime + i * this.step)
                    - this.growTime - k * 0.09f * (Settings.FAST_MODE ? 0.72f : 1.0f);
            if (tk <= 0.0f) {
                continue;
            }
            float tq = MathUtils.clamp(tk / Math.max(0.001f, this.tossTime),
                    0.0f, 1.0f);
            if (tq >= 1.0f) {
                continue;
            }
            EvolveDraw.tossed(sb, this.tipX[i], this.tipY[i],
                    EvolveFx.drawPileX(), EvolveFx.drawPileY(),
                    punch, tq, this.time * 260.0f + k * 110.0f);
        }

        sb.setBlendFunction(src, dst);
    }

    @Override
    public void dispose() {
    }
}
