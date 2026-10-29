package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 「确立基础」<b>被打出</b>时的演出：<b>一座方尖碑破土升起，刻线逐道点亮</b>。
 * <p>
 * <h2>这一段是补上去的</h2>
 * 旧实现里 {@code cast(CARD_ESTABLISHMENT)} 是<b>直接 {@code return}</b>，
 * 理由是「降费在回合末，施放时不演压实」。
 * <p>
 * 那个理由只对了一半 —— <b>不该演「压实」，但必须演「碑立起来了」</b>。
 * 而且 {@code Establishment.use()} 原版<b>一个特效都没有</b>，
 * 所以打出这张牌时画面上完全没有任何变化（与研习、涅槃同一类缺口）。
 * <p>
 * 更关键的是：常驻的碑在<b>角色左侧</b>，位置本身就是信息。
 * 如果不演「从地里升起来」，玩家不会注意到那里多了一个东西，
 * 更不会知道<b>碑面上的刻线</b>是「降几费」。
 * <h2>时间线（总长 1.32s，{@code FAST_MODE} × 0.72）</h2>
 * <pre>
 *   0.00–0.22  起势   碑基处地面裂开一圈，暗影压下
 *   0.14–0.94  升起   碑从地里往上长（pow2Out），升到三成迸碎石
 *   0.60–1.06  刻线   刻线自下而上逐道点亮
 *   0.94–1.24  过曝   碑顶一道白热闪过，读作「立稳了」
 *   1.24–1.32  交棒   常驻接管（坐标取自同一组函数，不跳位）
 * </pre>
 * <h2>动画四件套（规范 §三）</h2>
 * <ol>
 *   <li><b>出现</b>：{@code pow2Out} 升起，前快后慢，读作「顶上来后被卡住」</li>
 *   <li><b>消失</b>：本段不消失 —— 结尾交给常驻，重叠 0.1s 避免跳变</li>
 *   <li><b>位移</b>：碑基位置固定，可用高度随进度增长（从地里往上露）</li>
 *   <li><b>状态</b>：{@code excite} 带阻尼，控制过曝与刻线亮度</li>
 * </ol>
 * <h2>为什么刻线要「自下而上」点亮</h2>
 * 与回合末的「自上而下」<b>相反</b>。原因：
 * <ul>
 *   <li><b>施放</b>是「一层层垒起来」（从地基往上）→ 自下而上</li>
 *   <li><b>结算</b>是「压下去」（压力从上面来）→ 自上而下</li>
 * </ul>
 * 同一个元素、两种方向，正好把两件事区分开（规范 §2.10「双向要三个维度反向」）。
 * <h2>声音与震屏</h2>
 * 一局只几次 —— 允许轻震屏 + 音效（原版一点反馈都没有）。
 */
final class FoundationRiseEffect extends AbstractGameEffect {
    /** 起势时长（标准模式，会乘 timeScale）。 */
    private static final float FORM_BASE = 0.22f;
    /** 升起时长。 */
    private static final float RISE_BASE = 0.80f;
    /** 刻线自下而上点亮的跨度。 */
    private static final float MARK_BASE = 0.46f;
    /** 碑顶过曝时长。 */
    private static final float FLASH_BASE = 0.30f;
    /** 交棒重叠时长。 */
    private static final float HANDOFF_BASE = 0.14f;
    /** 碎石持续时间。 */
    private static final float DEBRIS_BASE = 0.72f;

    private final float cx;
    private final float baseY;
    private final float h;
    private final float w;
    private final int marks;
    private final float life;
    private final float timeScale;
    private final float formTime;
    private final float riseTime;
    private final float markTime;
    private final float flashTime;

    private float time;
    private float excite;

    FoundationRiseEffect(AbstractCreature owner, int marks) {
        this.timeScale = Settings.FAST_MODE ? 0.72f : 1.0f;
        this.formTime = FORM_BASE * this.timeScale;
        this.riseTime = RISE_BASE * this.timeScale;
        this.markTime = MARK_BASE * this.timeScale;
        this.flashTime = FLASH_BASE * this.timeScale;

        // 几何完全取自 FoundationFx：与常驻交接时坐标一致，不跳位
        this.cx = FoundationFx.monoX(owner);
        this.baseY = FoundationFx.monoBaseY(owner);
        this.h = FoundationFx.monoHeight(owner);
        this.w = FoundationFx.monoWidth(owner);
        this.marks = MathUtils.clamp(marks, 1, FoundationFx.MAX_MARKS);

        this.life = this.formTime + this.riseTime + this.markTime
                + this.flashTime + HANDOFF_BASE * this.timeScale;
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
            return;
        }
        // 状态：带阻尼。升起期间拉满，之后缓慢回落。
        float target = this.time < this.formTime + this.riseTime ? 1.0f : 0.25f;
        float speed = target > this.excite ? 14.0f : 2.2f;
        this.excite += (target - this.excite) * MathUtils.clamp(dt * speed, 0.0f, 1.0f);
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.duration <= 0.0f) {
            return;
        }
        float ex = MathUtils.clamp(this.excite, 0.0f, 1.0f);
        float punch = 0.88f + 0.26f * ex;

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        // ---- 起势：地面裂开一圈，暗影压下 ----
        if (this.time < this.formTime) {
            float q = MathUtils.clamp(this.time / Math.max(0.001f, this.formTime),
                    0.0f, 1.0f);
            float grow = Interpolation.pow2Out.apply(0.0f, 1.0f, q);
            Draw.glow(sb, this.cx, this.baseY, this.w * (3.0f + 3.0f * grow),
                    this.w * (0.9f + 0.9f * grow), 0.0f,
                    FoundationFx.STONE_DARK, 0.42f * q * punch);
            Draw.glow(sb, this.cx, this.baseY, this.w * 1.6f, this.w * 0.5f, 0.0f,
                    FoundationFx.STONE, 0.20f * q);
        }

        // ---- 升起 / 立稳：碑本体 ----
        if (this.time >= this.formTime) {
            float local = this.time - this.formTime;
            float riseQ = MathUtils.clamp(local / Math.max(0.001f, this.riseTime),
                    0.0f, 1.0f);
            // pow2Out：起手快、末端柔，读作「顶上来后被卡住」
            float show = Interpolation.pow2Out.apply(0.0f, 1.0f, riseQ);
            // 基座略微下沉，模拟「从地里往上露」
            float by = this.baseY - this.h * (1.0f - show) * 0.28f;

            // 刻线：自下而上逐道点亮（与回合末的自上而下相反）
            float markQ = MathUtils.clamp(
                    (local - this.riseTime * 0.55f) / Math.max(0.001f, this.markTime),
                    0.0f, 1.0f);
            int lit = MathUtils.clamp(
                    (int) Math.ceil(markQ * this.marks - 0.001f), 0, this.marks);

            FoundationDraw.monolith(sb, this.cx, by, this.h, this.w,
                    punch, 0.55f + 0.45f * markQ, lit, this.marks, show);

            // ---- 升起结束时的碑顶过曝：读作「立稳了」 ----
            float flashQ = MathUtils.clamp(
                    (local - this.riseTime) / Math.max(0.001f, this.flashTime),
                    0.0f, 1.0f);
            if (local > this.riseTime && flashQ < 1.0f) {
                float fa = (1.0f - flashQ) * (1.0f - flashQ) * 0.55f;
                Draw.glow(sb, this.cx, this.baseY + this.h * 0.5f,
                        this.w * 4.2f, this.h * 1.05f, 0.0f,
                        FoundationFx.CORE, fa * punch * 0.55f);
                Draw.glow(sb, this.cx, this.baseY + this.h * 0.35f,
                        this.w * 2.2f, this.h * 0.5f, 0.0f,
                        FoundationFx.GOLD, fa * punch * 0.45f);
                // 碑基一圈扩散环：读作「扎根」
                Draw.arc(sb, this.cx, this.baseY,
                        this.w * MathUtils.lerp(2.4f, 5.0f, flashQ),
                        this.w * MathUtils.lerp(0.7f, 1.5f, flashQ),
                        0.0f, 360.0f,
                        Math.max(5.0f * Settings.scale, this.w * 0.34f),
                        FoundationFx.STONE, fa * punch * 0.70f, 24);
            }

            // ---- 破土碎石：升到三成时迸一次 ----
            if (riseQ > 0.28f) {
                float debQ = MathUtils.clamp(
                        (local - this.riseTime * 0.28f)
                                / Math.max(0.001f, DEBRIS_BASE * this.timeScale),
                        0.0f, 1.0f);
                if (debQ > 0.0f && debQ < 1.0f) {
                    FoundationDraw.debris(sb, this.cx, this.baseY,
                            this.w * 7.0f,
                            (1.0f - debQ * 0.4f) * punch, debQ, 12, 1.7f);
                }
            }
        }

        sb.setBlendFunction(src, dst);
    }

    @Override
    public void dispose() {
    }
}
