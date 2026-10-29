package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;

/**
 * 「研习」的常驻表现：<b>抽牌堆图标上方扇形摊开的一叠纸页</b>。
 * <p>
 * <h2>为什么挂在抽牌堆而不是挂在角色身上</h2>
 * 这张牌唯一的产物是<b>塞进抽牌堆的牌</b>。所以视觉挂在它真正要去的地方，
 * 而不是挂在身上 —— 「效果发生在哪里，特效就画在哪里」。
 * <p>
 * 这也是全套观者牌里唯一一个<b>不挂在角色身上</b>的常驻特效。
 * 好处是它完全不占用角色周围的视觉空间，与其它牌零冲突；
 * 坏处是必须走 {@code queueTop}（见下）。
 * <h2>一个页 = 一张洞见</h2>
 * 页数 = {@code StudyPower.amount}，与原版
 * {@code MakeTempCardInDrawPileAction(new Insight(), amount, ...)}
 * 的数量参数<b>严格相等</b>（规范 §2.7 一一对应）。
 * <h2>为什么是「扇形」</h2>
 * 这套里已经有：贴地横环（如水）、竖碑成列（确立基础）、等距斜列的空心框（先见之明）、
 * 胸口圆环（虔信）。<b>从一个支点向上摊开的扇面</b>是全新的几何 ——
 * 页与页之间角度不同、彼此有遮挡关系、离支点等距。
 * 这三点同时成立时，它就不会被读成「又一排东西」。
 * <h2>动画四件套（规范 §三）</h2>
 * <ol>
 *   <li><b>出现</b>：页从支点<b>弹开成扇</b>（{@code swingOut}），
 *       并且逐页错开 0.07s —— 读作「一张一张铺开」</li>
 *   <li><b>消失</b>：页<b>向内收拢回支点</b>并淡出（0.90s），
 *       像被收进牌堆；不是整体降不透明度</li>
 *   <li><b>位移</b>：支点用 {@code FOLLOW_LERP} 追抽牌堆图标
 *       （窗口缩放、UI 动画时图标位置会变）</li>
 *   <li><b>状态</b>：{@code excite} 带阻尼，由回合末归档顶起；
 *       它驱动翻页速度与墨光强度</li>
 * </ol>
 * <h2>翻页：每页自己一个相位</h2>
 * 每页按各自的相位绕竖轴来回翻（宽度在 1.0 ↔ 0.18 之间压缩，明暗互换）。
 * 相位由<b>索引 + 累计时间</b>算出，不存随机状态，所以不会闪烁。
 * <h2>队列：必须走顶层</h2>
 * 它停在左下角，而底部 UI（{@code bottomBgPanel}、抽牌堆图标本身）画在
 * {@code effectsQueue} <b>之上</b>。走普通层会被完全盖住 —— 规范 §1.3。
 * 所以这个常驻特效由 {@link PurpleEffectRegistry#queueAura} 用 {@code queueTop} 投递，
 * 与其它走 {@code queue()} 的常驻特效不同。
 */
final class StudyAuraEffect extends PowerAuraEffect {
    /** 出现 / 消失时长。出现比消失短：铺开要利落，收起要留得住。 */
    private static final float APPEAR_TIME = 0.78f;
    private static final float VANISH_TIME = 0.90f;
    /** 逐页错开的铺开间隔（秒）。3 页铺开 0.14s。 */
    private static final float PAGE_STEP = 0.07f;
    /** 支点缓动速率：追抽牌堆图标。 */
    private static final float FOLLOW_LERP = 8.0f;
    /** 兴奋度的上升 / 回落（带阻尼）。 */
    private static final float EXCITE_RISE = 7.0f;
    private static final float EXCITE_FALL = 1.9f;
    /** 平时兴奋度基础值。 */
    private static final float EXCITE_IDLE = 0.18f;
    /** 翻页的基础周期（秒）。兴奋时变快。 */
    private static final float FLIP_PERIOD_IDLE = 3.6f;
    private static final float FLIP_PERIOD_HOT = 1.5f;

    private final float[] spot = new float[2];

    private float appear;
    private float vanish;
    private float excite;
    /**
     * 支点当前位置。
     * <p>
     * 它本身已经是「平滑追踪 {@link StudyFx#stackX}/{@link StudyFx#stackY}」的结果，
     * 绘制时直接用它，<b>不要再叠加滞后量</b> —— 那会把滞后算两遍
     * （先见之明踩过这个坑）。
     */
    private float followX;
    private float followY;
    private boolean anchored;

    StudyAuraEffect(AbstractCreature owner, String cardId, String powerId) {
        super(owner, cardId, powerId);
        // 页堆在左下角，与人物的层次无关；走顶层是为了不被底部 UI 盖住
        this.renderBehind = false;
        this.appear = 0.0f;
        this.vanish = 0.0f;
        this.excite = EXCITE_IDLE;
    }

    /** 回合末归档时调用：翻页变快、墨光亮一档。 */
    @Override
    void pulse() {
        this.excite = 1.0f;
    }

    @Override
    protected void tick(float dt) {
        // ---- 出现 ----
        if (this.appear < 1.0f) {
            this.appear = Math.min(1.0f, this.appear + dt / APPEAR_TIME);
        }
        // ---- 消失：页收拢回支点 ----
        if (this.isEnd) {
            this.vanish = Math.min(1.0f, this.vanish + dt / VANISH_TIME);
        } else if (this.vanish > 0.0f) {
            this.vanish = Math.max(0.0f, this.vanish - dt / VANISH_TIME);
        }

        // ---- 兴奋度：带阻尼（规范 §三.4）----
        float speed = EXCITE_IDLE > this.excite ? EXCITE_RISE : EXCITE_FALL;
        this.excite += (EXCITE_IDLE - this.excite)
                * MathUtils.clamp(dt * speed, 0.0f, 1.0f);

        // ---- 支点：缓动追抽牌堆图标 ----
        this.spot[0] = StudyFx.stackX();
        this.spot[1] = StudyFx.stackY();
        if (!this.anchored) {
            this.followX = this.spot[0];
            this.followY = this.spot[1];
            this.anchored = true;
        } else {
            float k = MathUtils.clamp(dt * FOLLOW_LERP, 0.0f, 1.0f);
            this.followX += (this.spot[0] - this.followX) * k;
            this.followY += (this.spot[1] - this.followY) * k;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.owner == null || this.fade <= 0.004f) {
            return;
        }
        float alpha = this.fade * (1.0f - this.vanish);
        if (alpha <= 0.004f) {
            return;
        }
        float ex = MathUtils.clamp(this.excite, 0.0f, 1.0f);

        // ---- 出现：pow2Out 撑开；消失：收拢回支点 ----
        float growIn = Interpolation.pow2Out.apply(0.0f, 1.0f,
                MathUtils.clamp(this.appear, 0.0f, 1.0f));
        float outEase = Interpolation.pow2In.apply(0.0f, 1.0f,
                MathUtils.clamp(this.vanish, 0.0f, 1.0f));
        // 收拢：扇形半径缩到 0.10，读作「被收回支点」
        float fanScale = growIn * MathUtils.lerp(1.0f, 0.10f, outEase);
        if (fanScale <= 0.02f) {
            return;
        }

        int n = StudyFx.pages(this.owner);
        float pw = StudyFx.pageW();
        float ph = StudyFx.pageH();
        // 扇面半径统一取自 StudyFx，与两个一次性演出共享同一个常量
        float radius = StudyFx.fanRadius() * fanScale;

        float bright = (0.80f + 0.20f * ex) * (0.90f + 0.10f * growIn);

        // 取独立副本：Draw 内部用的是共享 Color
        Color ink = new Color(StudyFx.INK);

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        // 支点上方一层薄光：读作「这里有个位置」
        Draw.glow(sb, this.followX, this.followY, radius * 1.9f, radius * 1.9f,
                0.0f, ink, alpha * 0.07f * bright);

        for (int i = 0; i < n; ++i) {
            // ---- 逐页错开铺开 ----
            float need = (i * PAGE_STEP) / Math.max(0.001f, APPEAR_TIME);
            if (this.appear < need) {
                continue;
            }
            float own = MathUtils.clamp(
                    (this.appear - need) / Math.max(0.001f, 1.0f - need), 0.0f, 1.0f);
            float ownEase = Interpolation.swingOut.apply(0.0f, 1.0f, own);

            // 扇面角度与页心位置
            float rad = StudyFx.fanDeg(i, n) * MathUtils.degreesToRadians;
            float cx = this.followX + MathUtils.cos(rad) * radius;
            float cy = this.followY + MathUtils.sin(rad) * radius;

            // 页自身倾角：扇面中轴附近小、外侧外倾大
            float tilt = StudyFx.pageTilt(i, n);

            // ---- 翻页：每页错相位，兴奋时变快 ----
            float period = MathUtils.lerp(FLIP_PERIOD_IDLE, FLIP_PERIOD_HOT, ex);
            // 相位 = 累计时间 / 周期 + 索引错开；映射到 0~1 的来回摆动
            float phase = this.time / Math.max(0.001f, period) + i * 0.37f;
            float tri = 0.5f + 0.5f * MathUtils.sin(phase * MathUtils.PI2);
            // 不完全翻满（0.06~0.94），避免在极端角度上「突然归零」
            float flip = MathUtils.lerp(0.06f, 0.94f, tri);

            // 页大小随自身铺开进度撑开
            float pageScale = MathUtils.lerp(0.45f, 1.0f, ownEase) * fanScale;
            if (pageScale <= 0.02f) {
                continue;
            }
            StudyDraw.page(sb, cx, cy, pw * pageScale, ph * pageScale,
                    tilt, flip, alpha * bright * ownEase);
        }

        sb.setBlendFunction(src, dst);
    }
}
