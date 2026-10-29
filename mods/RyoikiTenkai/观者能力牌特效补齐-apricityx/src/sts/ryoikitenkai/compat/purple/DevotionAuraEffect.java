package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;

/**
 * 「虔信」的常驻表现：<b>围绕角色胸口的一圈真言刻度光轮</b>。
 * <p>
 * <h2>为什么是「刻度环」</h2>
 * 虔信的机制是<b>累积真言到 10 点、升入神性</b>（见 {@link DevotionFx}）。
 * 所以常驻必须随时回答两个问题：<b>积了多少</b>、<b>还差多少</b>。
 * 十根刻度正好对应十点真言，玩家数亮点就知道进度，不用去看状态栏数字。
 * <h2>本轮返工：整体加大加亮（用户反馈「特效不够明显」）</h2>
 * 上一版只有一层「极淡的轨道 + 12×3 像素的扁点」，实际画面上几乎不可见。
 * 现在改成<b>六个部件叠出来的实体</b>：
 * <ol>
 *   <li>轨道（粗弧 + 内外体积光）</li>
 *   <li><b>进度弧</b>：整圈里最粗最亮的一条，长度 = 已积真言所占角度</li>
 *   <li>十根<b>径向粗棒</b>：已点亮的长而亮、未点亮的短而暗</li>
 *   <li>内圈六段<b>反向</b>慢转的粗弧 —— 与外侧同向旋转叠出体积感</li>
 *   <li>三个沿轨道滑行的诵经光点（带拖尾）</li>
 *   <li>中心的层级光：进度越高越亮</li>
 * </ol>
 * 每层单独看都还克制，叠起来才是一圈「读得出进度」的光轮（规范 §二.4）。
 * <h2>状态来自机制（规范 §2.3）</h2>
 * <ul>
 *   <li><b>进度</b>（真言 / 10）驱动：亮度、自转速度、进度弧长度、颜色</li>
 *   <li><b>未满</b>：紫白、慢转（7°/s）</li>
 *   <li><b>接近满</b>（&gt;60%）：颜色转向神性粉、转速升到 30°/s、整体变亮</li>
 *   <li><b>真言被清零</b>（升入神性后）：刻度全暗、环转为粉色高亮</li>
 * </ul>
 * <h2>动画四件套（规范 §三）</h2>
 * <ol>
 *   <li><b>出现</b>：整圈从 0.72 缓动展开（{@code pow2Out}，0.80s），
 *       刻度<b>依次</b>显形（错开 0.045s），不是一起冒出来</li>
 *   <li><b>消失</b>：整圈<b>向内收拢</b>到 0.16 倍 + 淡出（0.95s），
 *       不是整体淡掉 —— 环的合理收场是「收起来」</li>
 *   <li><b>位移</b>：锚点用 {@code FOLLOW_LERP} 缓动追赶角色</li>
 *   <li><b>状态</b>：{@code excite} 带阻尼，由「回合开始累加真言」顶起；
 *       所有亮度、光点强度、脉动幅度都乘它</li>
 * </ol>
 * <h2>空间签名（规范 §2.2）</h2>
 * 与心之堡垒（同样「绕身」）的区分：
 * <table border="1">
 *   <tr><td></td><td>心之堡垒</td><td>虔信</td></tr>
 *   <tr><td>形态</td><td>立体立方体，12 棱 + 深度排序</td><td>平面刻度环，10 根径向棒</td></tr>
 *   <tr><td>转速</td><td>快（随姿态加速）</td><td>极慢（7~30°/s）</td></tr>
 *   <tr><td>颜色</td><td>姿态色（红/蓝/金）</td><td>紫白 → 神性粉</td></tr>
 *   <tr><td>读数</td><td>无（纯氛围）</td><td><b>可数刻度 + 进度弧 = 进度</b></td></tr>
 * </table>
 * 一个是「壳」，一个是「环」；一个看气质，一个读数字。
 * <h2>不绑定骨骼（§1.1）与性能（§3）</h2>
 * 坐标只来自 {@link BodyFx}；没有子特效；每帧不 new；混合函数 get/set 还原。
 */
final class DevotionAuraEffect extends PowerAuraEffect {
    /** 出现 / 消失时长。环的展开与收拢都要看得见。 */
    private static final float APPEAR_TIME = 0.80f;
    private static final float VANISH_TIME = 0.95f;
    /** 刻度逐个显形的错开（秒）。10 根铺开 0.45s。 */
    private static final float TICK_STEP = 0.045f;
    /** 锚点缓动速率。 */
    private static final float FOLLOW_LERP = 7.0f;
    /** 兴奋度的上升 / 回落（带阻尼）。 */
    private static final float EXCITE_RISE = 6.0f;
    private static final float EXCITE_FALL = 1.8f;
    /** 平时兴奋度基础值。 */
    private static final float EXCITE_IDLE = 0.26f;
    /** 绕行光点数。 */
    private static final int CHANT_DOTS = 3;
    /** 一次涟漪的时长（秒）。 */
    private static final float RIPPLE_TIME = 0.62f;

    private final float[] spot = new float[2];

    private float appear;
    private float vanish;
    /** 带阻尼的兴奋度：回合开始累加真言时顶起。 */
    private float excite;
    /** 自转累计角度（度）。速度由进度决定，所以是累加。 */
    private float spin;
    /** 内圈的反向角度（度）。 */
    private float innerSpin;
    /** 绕行光的相位 0~1。 */
    private float chantPhase;
    /** 进度的平滑版本：让颜色/转速的变化不硬跳。 */
    private float smoothProgress;
    /** 一次涟漪的进度 0~1；&gt;= 1 表示这张没在放。 */
    private float rippleT;
    private float followX;
    private float followY;
    private boolean anchored;

    DevotionAuraEffect(AbstractCreature owner, String cardId, String powerId) {
        super(owner, cardId, powerId);
        // 环画在人前面，否则会被立绘遮掉；但它是横环，不挡脸
        this.renderBehind = false;
        this.appear = 0.0f;
        this.vanish = 0.0f;
        this.excite = EXCITE_IDLE;
        this.spin = 0.0f;
        this.innerSpin = 0.0f;
        this.chantPhase = 0.0f;
        this.smoothProgress = 0.0f;
        this.rippleT = 1.0f;
    }

    /** 回合开始累加真言时调用：兴奋度顶起 + 向外扩一圈涟漪。 */
    @Override
    void pulse() {
        this.excite = 1.0f;
        this.rippleT = 0.0f;
    }

    @Override
    protected void tick(float dt) {
        // ---- 出现 ----
        if (this.appear < 1.0f) {
            this.appear = Math.min(1.0f, this.appear + dt / APPEAR_TIME);
        }
        // ---- 消失：整圈向内收拢 ----
        if (this.isEnd) {
            this.vanish = Math.min(1.0f, this.vanish + dt / VANISH_TIME);
        } else if (this.vanish > 0.0f) {
            this.vanish = Math.max(0.0f, this.vanish - dt / VANISH_TIME);
        }

        // ---- 兴奋度：带阻尼（规范 §三.4）----
        float speed = EXCITE_IDLE > this.excite ? EXCITE_RISE : EXCITE_FALL;
        this.excite += (EXCITE_IDLE - this.excite)
                * MathUtils.clamp(dt * speed, 0.0f, 1.0f);

        // ---- 进度平滑：让颜色与转速的变化不硬跳 ----
        float target = DevotionFx.progress(this.owner);
        this.smoothProgress += (target - this.smoothProgress)
                * MathUtils.clamp(dt * 4.0f, 0.0f, 1.0f);

        // ---- 自转（外圈正向、内圈反向）：速度由进度决定 ----
        this.spin += dt * DevotionFx.spinSpeed(this.smoothProgress);
        this.spin = this.spin % 360.0f;
        this.innerSpin += dt * DevotionFx.INNER_SPIN;
        this.innerSpin = this.innerSpin % 360.0f;
        // 绕行光走得更快一点，读作「诵经不停」
        this.chantPhase += dt * (0.09f + 0.26f * this.smoothProgress);
        this.chantPhase = this.chantPhase % 1.0f;

        // ---- 涟漪推进 ----
        if (this.rippleT < 1.0f) {
            this.rippleT = Math.min(1.0f, this.rippleT + dt / RIPPLE_TIME);
        }

        // ---- 锚点：缓动追赶角色 ----
        this.spot[0] = DevotionFx.centerX(this.owner);
        this.spot[1] = DevotionFx.centerY(this.owner);
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
        float prog = MathUtils.clamp(this.smoothProgress, 0.0f, 1.0f);

        // ---- 出现：pow2Out 展开；消失：向内收拢 ----
        float growIn = Interpolation.pow2Out.apply(0.0f, 1.0f,
                MathUtils.clamp(this.appear, 0.0f, 1.0f));
        float outEase = Interpolation.pow2In.apply(0.0f, 1.0f,
                MathUtils.clamp(this.vanish, 0.0f, 1.0f));
        // 收拢到 0.16 倍再淡出，读作「收进去」
        float ringScale = growIn * MathUtils.lerp(1.0f, 0.16f, outEase);
        if (ringScale <= 0.02f) {
            return;
        }

        float cx = this.followX;
        float cy = this.followY;
        float rx = DevotionFx.radiusX(this.owner) * ringScale;
        float ry = DevotionFx.radiusY(this.owner) * ringScale;

        // 颜色：紫白 → 神性粉（进度驱动）。取独立副本，避免被后续绘制覆盖。
        Color main = new Color(DevotionFx.tint(prog));
        Color core = new Color(DevotionFx.coreTint(prog));
        // 亮度：兴奋 + 进度双驱动，并整体抬一档（上一版太暗）
        float bright = (0.86f + 0.22f * ex) * (0.88f + 0.26f * prog);
        float a = MathUtils.clamp(alpha * bright, 0.0f, 1.0f);

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        // ---- 1. 轨道：闭合粗弧，提供「这一圈在哪」 ----
        DevotionDraw.track(sb, cx, cy, rx, ry, a, main);

        // ---- 2. 进度弧：整圈最粗最亮的一条，长度 = 已积真言 ----
        int filled = DevotionFx.mantra(this.owner);
        DevotionDraw.gauge(sb, cx, cy, rx, ry, filled, this.spin, a, main, core);

        // ---- 3. 内圈：六段反向慢转的粗弧 ----
        DevotionDraw.innerRing(sb, cx, cy, rx, ry, this.innerSpin, a * 0.9f, main);

        // ---- 4. 十根径向刻度：已点亮的长而亮、未点亮的短而暗 ----
        // 出现时逐根显形：第 i 根需要 appear 超过 i 的错开量
        int visible = 0;
        for (int i = 0; i < DevotionFx.TICKS; ++i) {
            float need = (i * TICK_STEP) / Math.max(0.001f, APPEAR_TIME);
            if (this.appear >= need) {
                visible = i + 1;
            } else {
                break;
            }
        }
        DevotionDraw.ticks(sb, cx, cy, rx, ry, filled, visible, this.spin, a,
                main, core, DevotionFx.DIM, this.time, prog);

        // ---- 5. 绕行光：读作「正在诵经」 ----
        DevotionDraw.chant(sb, cx, cy, rx, ry, this.chantPhase,
                CHANT_DOTS, a * (0.80f + 0.20f * ex),
                main, core, 0.60f + 0.40f * prog);

        // ---- 6. 中心的层级光：进度越高越亮，读作「越积越满」 ----
        if (prog > 0.02f) {
            float coreSize = rx * (0.24f + 0.32f * prog);
            Draw.glow(sb, cx, cy, coreSize * 3.4f, coreSize * 2.6f, 0.0f,
                    main, a * 0.20f * prog * (0.7f + 0.6f * ex));
            Draw.glow(sb, cx, cy, coreSize * 1.5f, coreSize * 1.2f, 0.0f,
                    main, a * 0.24f * prog);
            Draw.glow(sb, cx, cy, coreSize * 0.7f, coreSize * 0.55f, 0.0f,
                    core, a * 0.30f * prog);
        }

        // ---- 7. 涟漪：这一回合真的加了真言的明确时间点 ----
        if (this.rippleT < 1.0f) {
            DevotionDraw.ripple(sb, cx, cy, rx, ry, this.rippleT,
                    a * (0.85f + 0.15f * ex), main, core);
        }

        // ---- 8. 满值预警：9 格时整圈开始过曝呼吸，读作「下一句就要满了」 ----
        if (filled >= DevotionFx.FULL - 1 && filled < DevotionFx.FULL) {
            float warn = 0.5f + 0.5f * MathUtils.sin(this.time * 9.0f);
            Draw.glow(sb, cx, cy, rx * 2.5f, ry * 2.5f, 0.0f, main,
                    a * (0.10f + 0.14f * warn));
        }

        sb.setBlendFunction(src, dst);
    }
}
