package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;

/**
 * 「洞悉现实」的常驻表现：<b>角色右侧身外的「＋」校准刻印 + 下方一列刻痕</b>。
 * <p>
 * <h2>它回答什么问题</h2>
 * 这张牌的能力<b>没有层数</b>，唯一的可读状态是「本场战斗它已经把几张新牌升成了 +」。
 * 所以常驻的读数落在刻印下方那六道刻痕上：<b>点亮几道 ＝ 升级过几张</b>（规范 §2.5 / §2.17）。
 * <p>
 * 未点亮的槽位始终画着（只是更短更暗），所以「0/6」也是可读的 ——
 * 规范 §4.10 明确要求「未激活状态不能看不见」。超过六张后整组保持点亮，
 * <b>不增加实体</b>（读数饱和，规范 §2.17 判据三：这是「多强」，不是「几个新东西」）。
 * <h2>位置为什么不挂在身上</h2>
 * 这张牌作用的对象是<b>新造出来的牌</b>，不是角色。但常驻必须留在视野里才读得到读数，
 * 所以选在<b>右侧身外</b>：既离开立绘，又与确立基础（左侧、贴地的石碑）左右对称、
 * 与欧米伽（头顶 Ω）、先见之明（头顶右侧光框）错开高度。
 * <h2>渲染层</h2>
 * 基类 {@code PowerAuraEffect} 默认 {@code renderBehind = true}；这里的刻印悬在<b>身外</b>，
 * 但为了避免被怪物/立绘压住（猛虎抓痕踩过这个坑），显式设成 {@code false}。
 * <h2>出现 / 消失</h2>
 * 出现：随基类的 0.75s 淡入，同时尺寸从 62% 张开到 100%（读作「被放到位置上」）。
 * 消失：光材质 → <b>上浮</b>散掉（规范 §3.2），所以这里追踪「曾经达到的最大不透明度」，
 * 用它的差值做出上升位移；淡入阶段差值为 0，不会误触发。
 */
final class MasterRealityAuraEffect extends PowerAuraEffect {
    /** 盖印脉冲：1 → 0，由 {@link #pulse()} 置位。 */
    private float stamp;
    /** 曾经达到的最大不透明度，用来算「淡出中」的进度。 */
    private float shown;
    /** 淡出时的上浮位移（像素）。 */
    private float lift;

    /** 六道刻痕各自的点亮进度（0~1，带阻尼；规范 §3.4）。 */
    private final float[] grow = new float[MasterRealityFx.MAX_SCRATCH];

    MasterRealityAuraEffect(AbstractCreature owner, String cardId, String powerId) {
        super(owner, cardId, powerId);
        // 刻印在身侧悬空，不能被立绘/怪物遮住
        this.renderBehind = false;
        this.stamp = 0.0f;
        this.shown = 0.0f;
        this.lift = 0.0f;
        // 这张牌刚挂上（或被重新打出）：读数从零重算
        MasterRealityFx.reset();
    }

    /** 能力真的升级了一张新牌时调用：刻印盖一下。 */
    @Override
    void pulse() {
        this.stamp = 1.0f;
    }

    @Override
    protected void tick(float dt) {
        this.stamp = Math.max(0.0f, this.stamp - dt * 2.4f);

        // 淡出进度：用「历史最大值 − 当前值」，这样淡入阶段恒为 0
        this.shown = Math.max(this.shown, this.fade);
        this.lift = (this.shown - this.fade) * 30.0f * Settings.scale;

        // 刻痕逐道点亮 / 熄灭，带阻尼，读数跳变时不会「啪」地一下
        int n = MasterRealityFx.upgradedCount();
        float k = MathUtils.clamp(dt * 9.0f, 0.0f, 1.0f);
        for (int i = 0; i < MasterRealityFx.MAX_SCRATCH; ++i) {
            float target = i < n ? 1.0f : 0.0f;
            this.grow[i] += (target - this.grow[i]) * k;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.fade <= 0.004f || this.owner == null) {
            return;
        }
        // 出现：尺寸从 62% 张开到 100%（与淡入同步）
        float half = MasterRealityFx.markHalf(this.owner) * (0.62f + 0.38f * this.fade);
        float cx = MasterRealityFx.markX(this.owner);
        float cy = MasterRealityFx.markY(this.owner) + this.lift;
        // 常驻要克制：只做轻微呼吸，不做大幅动作（规范 §2.5）
        float a = this.fade * (0.90f + 0.10f * breath(3.6f));

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);
        MasterRealityFx.drawMark(sb, cx, cy, half, a, this.stamp);
        MasterRealityFx.drawScratchSlots(sb, cx, cy, half, a, this.grow);
        sb.setBlendFunction(src, dst);
    }
}
