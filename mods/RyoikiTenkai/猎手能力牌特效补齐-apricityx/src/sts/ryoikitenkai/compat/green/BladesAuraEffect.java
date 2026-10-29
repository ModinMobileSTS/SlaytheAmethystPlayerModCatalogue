package sts.ryoikitenkai.compat.green;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;

/**
 * 「无限刀刃」的常驻表现：<b>一排匕首悬浮在人物右侧，刃尖一律朝右</b>。
 * <p>
 * <h2>姿态（按需求）</h2>
 * <pre>
 *   (人物)   ═╪═ ═╪═     ← 刃尖朝右（DIR_DEG = 0°）
 *            ═╪═ ═╪═
 *            ═╪═
 * </pre>
 * <ul>
 *   <li><b>刃尖一律水平朝右</b>，所有刀同向 —— 读作「一排待发的刀」</li>
 *   <li>悬浮在人物<b>右侧身外</b>，按列排布（每列 2 把、各列横向错开）</li>
 *   <li><b>不自转</b>，只有每把错相的轻微上下浮动 —— 读作「浮着」</li>
 * </ul>
 * 上一版是「绕身一圈、刃尖朝外（法向）、缓慢自转」；这一版按需求改成
 * 「右侧、同向朝右、悬停」，也因此与「精准」的静止标靶进一步区分开：
 * 一个在左、一个在右；一个是光环刻痕、一个是排开的实体刀。
 * <h2>尺寸（本轮放大）</h2>
 * 刀长 {@code max(34px × scale, 半宽 × 0.62)}，是上一版的 1.55 倍；
 * 厚度也跟着由 {@link BladesFx#bladeThick} 派生（不低于 {@code MIN_LINE × 1.3}）。
 * <h2>数量：{@code amount + 2}（3 ~ 6）</h2>
 * 叠加仍然看得出来（每叠一层多一把），单张也有「一排」的观感。
 * <h2>深度：同一排也要有前后</h2>
 * 第二列（靠右的那列）画在<b>后</b>面（更暗更小一点），第一列压在前面 ——
 * 这样「两列错开」才读得出空间，而不是平铺在一张纸上。
 * <h2>动画四件套（规范 §三）</h2>
 * <ol>
 *   <li><b>出现</b>：逐把错开浮现（0.05s/把），从悬浮区中心向外推开到各自槽位。
 *       曲线用 {@link PowerAuraEffect#swing}（单调 pow2Out，不会死平）</li>
 *   <li><b>消失</b>：向<b>右</b>飞散 + 缩小 + 淡出（{@code pow2In}）——
 *       顺着刃尖方向散开，符合「刀往那边去」的直觉</li>
 *   <li><b>位移</b>：锚点 {@code FOLLOW_LERP} 缓动追角色，再叠加逐把轻浮</li>
 *   <li><b>状态</b>：{@code excite}（回合开始拿到刀时顶起）带阻尼</li>
 * </ol>
 * <h2>性能</h2>
 * 最多 6 把 × 约 10 笔 ≈ 60 笔，不含子特效、不 new 对象。
 * 颜色用类私有副本 + {@code set()}；深度排序用静态槽位。
 */
final class BladesAuraEffect extends PowerAuraEffect {
    /** 锚点缓动速率。 */
    private static final float FOLLOW_LERP = 8.5f;
    /** 兴奋度：回合开始拿到小刀时顶起。 */
    private static final float EXCITE_RISE = 12.0f;
    private static final float EXCITE_FALL = 2.2f;
    /** 逐把错开浮现（秒）。 */
    private static final float APPEAR_STEP = 0.05f;
    /** 单把从中心推开到槽位所用的归一化跨度。 */
    private static final float RISE_SPAN = 0.85f;
    /** 消失时向右飞散的距离（× 刀长）。 */
    private static final float VANISH_FLY = 1.1f;

    /** 类私有颜色副本。 */
    private final Color green = new Color();
    private final Color blade = new Color();
    private final Color crack = new Color();
    private final Color wood = new Color();
    private final Color guard = new Color();

    private float followX;
    private float followY;
    private boolean anchored;

    /** 兴奋度 0~1。 */
    private float excite;

    /** 槽位计算用的复用数组（长度 2）。 */
    private final float[] slot = new float[2];

    BladesAuraEffect(AbstractCreature owner) {
        super(owner, GreenEffectRegistry.BLADES_CARD, GreenEffectRegistry.BLADES_ID);
        // 悬浮在右侧身外：画在人前面，避免被立绘或怪物吃掉
        this.renderBehind = false;
    }

    /** 回合开始拿到小刀：整排亮一档。 */
    @Override
    void pulse() {
        this.excite = 1.0f;
    }

    @Override
    protected void tick(float dt) {
        this.excite = smooth(this.excite, 0.0f, EXCITE_RISE, EXCITE_FALL, dt);

        float tx = BladesFx.centerX(this.owner);
        float ty = BladesFx.centerY(this.owner);
        if (!this.anchored) {
            this.followX = tx;
            this.followY = ty;
            this.anchored = true;
        } else {
            float k = MathUtils.clamp(dt * FOLLOW_LERP, 0.0f, 1.0f);
            this.followX += (tx - this.followX) * k;
            this.followY += (ty - this.followY) * k;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.fade <= 0.004f || !alive()) {
            return;
        }
        int n = BladesFx.count(this.owner);
        if (n <= 0) {
            return;
        }

        float grown = swing(appearT());
        if (grown <= 0.004f) {
            return;
        }
        float v = vanish();
        float vEase = GreenDraw.easeIn(v);
        if (vEase >= 1.0f) {
            return;
        }

        float cx = this.followX;
        float cy = this.followY;
        float len = BladesFx.bladeLen(this.owner);
        int cols = n <= 3 ? 1 : 2;

        this.green.set(BladesFx.GREEN);
        this.blade.set(BladesFx.BLADE);
        this.crack.set(BladesFx.CRACK);
        this.wood.set(BladesFx.WOOD);
        this.guard.set(BladesFx.GUARD);

        int blendSrc = sb.getBlendSrcFunc();
        int blendDst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        // 两列有前后：先画右列（靠外、视为在后），再画左列（压在前）。
        // 单列时只有一次循环。
        for (int pass = 0; pass < cols; ++pass) {
            int col = cols - 1 - pass;   // pass 0 → 右列，pass 1 → 左列
            for (int i = col; i < n; i += cols) {
                // ---- 逐把浮现：按索引错开，从悬浮区中心推到槽位 ----
                float localT = MathUtils.clamp(
                        (appearT() - i * APPEAR_STEP) / RISE_SPAN, 0.0f, 1.0f);
                if (localT <= 0.0f) {
                    continue;
                }
                float rise = swing(localT);

                BladesFx.slotOffset(i, n, len, this.slot);
                // 远列（右侧那列）稍小稍暗，给前后感
                float depth = cols == 1 ? 1.0f : (col == 0 ? 1.0f : 0.86f);
                float px = cx + this.slot[0] * rise
                        + vEase * len * VANISH_FLY * depth;
                float py = cy + this.slot[1] * rise
                        + BladesFx.bobOffset(i, this.time);

                float a = this.fade * grown * (0.58f + 0.42f * depth)
                        * (1.0f - vEase) * (0.82f + 0.18f * this.excite);
                float g = MathUtils.clamp(
                        0.35f + 0.45f * this.excite + 0.20f * rise, 0.0f, 1.0f);
                // 刃尖一律朝右
                BladesDraw.dagger(sb, px, py, len * depth * (0.75f + 0.25f * rise),
                        BladesFx.DIR_DEG, a, g);
            }
        }

        sb.setBlendFunction(blendSrc, blendDst);
    }
}
