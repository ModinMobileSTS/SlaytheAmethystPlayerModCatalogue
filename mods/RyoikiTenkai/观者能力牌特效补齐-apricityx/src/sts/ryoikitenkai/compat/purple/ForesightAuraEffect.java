package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;

/**
 * 「先见之明」的常驻表现：<b>悬在身前上方的一列空心光框</b>。
 * <p>
 * <h2>每个框回答什么</h2>
 * <b>一个框 = 每回合能占卜的一张牌</b>。所以这一列本质上是
 * 「这回合我会摊开几张」的预告，数量与机制一一对应（见 {@link ForesightFx#frames}）。
 * <p>
 * 框是<b>空心的</b> —— 读作「还没显影、还没被看见的那一张」。
 * 这正是这张牌的核心语义：牌在抽牌堆里，你知道它在那儿，但还看不见内容。
 * 占卜时框被依次扫描显影，内容才「浮出来」（见 {@link ForesightScryEffect}）。
 * <h2>为什么不用原来的 FLAME_EYE</h2>
 * 旧实现是「头顶紫红火焰环 + 中心金眼」，两条都不成立：
 * <ul>
 *   <li><b>火是铁甲的语汇</b>。这张牌与燃烧无关，属于「按卡图猜」而非「按机制」</li>
 *   <li><b>画眼是具象物</b>，规范明令禁止；小尺寸下还读不出是眼</li>
 * </ul>
 * <h2>动画四件套（规范 §三）</h2>
 * <ol>
 *   <li><b>出现</b>：框从 0.62 缓动撑开（{@code swingOut}），
 *       并且<b>逐框错开</b> 0.06s —— 读作「一张一张立起来」，不是一起冒出来</li>
 *   <li><b>消失</b>：框<b>向内缩扁</b>并淡出（0.92s），
 *       像一张纸被抽走；不是整体降不透明度（那会读成「被关掉」）</li>
 *   <li><b>位移</b>：整列用 {@code FOLLOW_LERP} 缓动追角色，
 *       各框再叠一层<b>错相位的缓浮</b>，读作「悬在空中」</li>
 *   <li><b>状态</b>：{@code excite} 带阻尼，由占卜顶起；
 *       它驱动边框加粗、四角光斑变亮</li>
 * </ol>
 * <h2>空间签名（规范 §2.2）</h2>
 * 与「确立基础」（同样是「N 个东西成列」）的区分：
 * <table border="1">
 *   <tr><td></td><td>确立基础</td><td>先见之明</td></tr>
 *   <tr><td>形状</td><td>尖顶方碑（实心竖块）</td><td><b>空心矩形框</b></td></tr>
 *   <tr><td>位置</td><td>角色左侧、贴地</td><td><b>身前上方、悬空</b></td></tr>
 *   <tr><td>排列</td><td>水平向左排开</td><td><b>扬起 13° 的斜列</b></td></tr>
 *   <tr><td>材质</td><td>石（不透明，有暗侧面）</td><td><b>半透明光框</b></td></tr>
 *   <tr><td>动静</td><td>静止</td><td><b>各自错相缓浮</b></td></tr>
 * </table>
 * 四个维度全部不同，且矩形 vs 尖顶碑在最基本的形状上就能分开。
 * <h2>不绑定骨骼（§1.1）与性能（§3）</h2>
 * 坐标只来自 {@link BodyFx}；没有子特效；tick 里不 new；
 * 每帧只为颜色建 1 份独立副本；混合函数 get/set 还原。
 */
final class ForesightAuraEffect extends PowerAuraEffect {
    /** 出现 / 消失时长。出现比确立基础稍快（它是「浮起来」不是「升起来」）。 */
    private static final float APPEAR_TIME = 0.72f;
    private static final float VANISH_TIME = 0.92f;
    /** 逐框错开的显形间隔（秒）。6 个框铺开 0.30s。 */
    private static final float FRAME_STEP = 0.06f;
    /** 锚点缓动速率。 */
    private static final float FOLLOW_LERP = 7.5f;
    /** 兴奋度的上升 / 回落（带阻尼）。 */
    private static final float EXCITE_RISE = 7.0f;
    private static final float EXCITE_FALL = 1.9f;
    /** 平时兴奋度基础值。 */
    private static final float EXCITE_IDLE = 0.20f;
    /** 缓浮的振幅（× 框高）与周期（秒，每个框错相）。 */
    private static final float FLOAT_AMP = 0.11f;
    private static final float FLOAT_PERIOD = 3.4f;
    /** 未显影状态的呼吸周期（秒）。极慢：这是「安静的预告」，不该抢戏。 */
    private static final float BREATH_PERIOD = 4.6f;

    private final float[] spot = new float[2];

    private float appear;
    private float vanish;
    private float excite;
    /**
     * 整列锚点的当前位置。
     * <p>
     * 它<b>本身已经是</b>「平滑追踪 {@link ForesightFx#groupX}/{@link ForesightFx#groupY}」
     * 的结果，所以绘制时直接用它做整列中心，<b>不要再叠加「目标 − 当前」的滞后量</b>
     * —— 那等于把滞后算了两遍，角色一移动整列的拖尾会翻倍。
     */
    private float followX;
    private float followY;
    private boolean anchored;

    ForesightAuraEffect(AbstractCreature owner, String cardId, String powerId) {
        super(owner, cardId, powerId);
        // 框在身前上方，画在人前面才不会被立绘吃掉
        this.renderBehind = false;
        this.appear = 0.0f;
        this.vanish = 0.0f;
        this.excite = EXCITE_IDLE;
    }

    /** 占卜时调用：兴奋度顶起，框边加粗、四角变亮。 */
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
        // ---- 消失：向内缩扁 ----
        if (this.isEnd) {
            this.vanish = Math.min(1.0f, this.vanish + dt / VANISH_TIME);
        } else if (this.vanish > 0.0f) {
            this.vanish = Math.max(0.0f, this.vanish - dt / VANISH_TIME);
        }

        // ---- 兴奋度：带阻尼（规范 §三.4）----
        float speed = EXCITE_IDLE > this.excite ? EXCITE_RISE : EXCITE_FALL;
        this.excite += (EXCITE_IDLE - this.excite)
                * MathUtils.clamp(dt * speed, 0.0f, 1.0f);

        // ---- 锚点：缓动追赶角色 ----
        this.spot[0] = ForesightFx.groupX(this.owner);
        this.spot[1] = ForesightFx.groupY(this.owner);
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

        // ---- 出现：pow2Out 撑开；消失：向内缩扁 ----
        float growIn = Interpolation.pow2Out.apply(0.0f, 1.0f,
                MathUtils.clamp(this.appear, 0.0f, 1.0f));
        float outEase = Interpolation.pow2In.apply(0.0f, 1.0f,
                MathUtils.clamp(this.vanish, 0.0f, 1.0f));
        // 宽收缩得比高快：读作「被抽扁」
        float wScale = growIn * MathUtils.lerp(1.0f, 0.10f, outEase);
        float hScale = growIn * MathUtils.lerp(1.0f, 0.55f, outEase);
        if (wScale <= 0.02f) {
            return;
        }

        int n = ForesightFx.frames(this.owner);
        float w = ForesightFx.frameW(this.owner) * wScale;
        float h = ForesightFx.frameH(this.owner) * hScale;
        float sp = ForesightFx.spacing(this.owner);
        float tilt = ForesightFx.tilt();
        // 整列中心就是缓动追踪值本身（见 followX 的注释）
        float cx0 = this.followX;
        float cy0 = this.followY;

        // 亮度：兴奋 + 呼吸双驱动
        float breath = 0.5f + 0.5f * MathUtils.sin(
                this.time * MathUtils.PI2 / BREATH_PERIOD);
        float bright = (0.72f + 0.28f * ex) * (0.88f + 0.12f * breath);

        // 取独立副本：Draw 内部用的是共享 Color
        Color edge = new Color(ForesightFx.VIOLET);
        Color core = new Color(ForesightFx.ICE);

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        float rad = 13.0f * MathUtils.degreesToRadians;
        for (int i = 0; i < n; ++i) {
            // ---- 逐框错开显形：第 i 个框需要 appear 超过 i × FRAME_STEP ----
            float need = (i * FRAME_STEP) / Math.max(0.001f, APPEAR_TIME);
            if (this.appear < need) {
                continue;
            }
            // 该框自己的显形进度（归一化到它自己的窗口里）
            float own = MathUtils.clamp(
                    (this.appear - need) / Math.max(0.001f, 1.0f - need), 0.0f, 1.0f);
            float ownEase = Interpolation.swingOut.apply(0.0f, 1.0f, own);

            float u = i - (n - 1) * 0.5f;
            float cx = cx0 + MathUtils.cos(rad) * sp * u;
            float cy = cy0 + MathUtils.sin(rad) * sp * u;
            // 错相位缓浮：读作「悬着」
            cy += MathUtils.sin(this.time * MathUtils.PI2 / FLOAT_PERIOD
                    + i * 1.31f) * h * FLOAT_AMP;

            // 未显影是常态：reveal 由兴奋度轻微顶一下（占卜时整列会亮一档）
            float reveal = MathUtils.clamp(ex * 0.55f, 0.0f, 1.0f);
            // 框高随自身显形撑开：读作「一张一张立起来」
            float fh = h * MathUtils.lerp(0.35f, 1.0f, ownEase);
            ForesightDraw.frame(sb, cx, cy, w, fh, tilt,
                    alpha * bright, reveal, edge, core, ForesightFx.DORMANT);
        }

        sb.setBlendFunction(src, dst);
    }
}
