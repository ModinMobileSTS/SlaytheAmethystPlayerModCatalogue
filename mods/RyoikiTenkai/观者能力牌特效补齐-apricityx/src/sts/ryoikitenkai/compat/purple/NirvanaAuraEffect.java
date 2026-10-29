package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;

/**
 * 「涅槃」的常驻表现：<b>贴身的层叠六边护壁</b>。
 * <p>
 * <h2>每一层回答什么</h2>
 * <b>一层护壁 = 一次占卜给的格挡</b>。层数直接由当前格挡值算出
 * （见 {@link NirvanaFx#layers}），所以：
 * <ul>
 *   <li>占卜加格挡 → <b>多一层</b>，从内侧顶出</li>
 *   <li>受击掉格挡 → <b>层数减少</b>，外层收回去</li>
 *   <li>回合开始清零 → <b>只剩一层薄壳</b>（不消失，因为能力还在）</li>
 * </ul>
 * 「受击变薄」这一条是本设计最要紧的：它让护壁与战斗产生实时因果，
 * 而不是一个永远长一样的装饰。
 * <h2>与心之堡垒的区分（规范 §2.2 四个维度）</h2>
 * <table border="1">
 *   <tr><td></td><td>心之堡垒</td><td>涅槃</td></tr>
 *   <tr><td>几何</td><td>立方体，12 棱 + 透视</td><td><b>六边形，平面棱壳</b></td></tr>
 *   <tr><td>层数</td><td>永远单层</td><td><b>1~3 层同心，随格挡增减</b></td></tr>
 *   <tr><td>尺寸</td><td>比身体大</td><td><b>贴身体轮廓</b></td></tr>
 *   <tr><td>变化驱动</td><td>姿态切换</td><td><b>格挡增减</b></td></tr>
 * </table>
 * 决定性的是「层数」与「变化驱动」：立方体永远不变形，护壁实时增减。
 * <h2>动画四件套（规范 §三）</h2>
 * <ol>
 *   <li><b>出现</b>：每层从 0.55 撑开到 1.0（{@code pow2Out}，0.68s），
 *       层与层错开 0.08s —— 读作「一层层套上去」</li>
 *   <li><b>消失</b>：层<b>向内收回到身体</b>并淡出（0.88s）。
 *       注意消失比出现慢（规范 §一的基准）</li>
 *   <li><b>位移</b>：锚点用 {@code FOLLOW_LERP} 缓动追角色</li>
 *   <li><b>状态</b>：{@code excite} 带阻尼，由占卜顶起；
 *       驱动棱的亮部与顶点团光</li>
 * </ol>
 * <h2>层数变化不跳位</h2>
 * 第 {@code i} 层的半径只由 {@code i} 决定（{@code base + step × i}），
 * <b>不随总层数变化</b>。所以从 2 层变 3 层时，原有的两层完全不动，
 * 只是外面多出一层 —— 玩家的参照点不跳（规范 §三.7）。
 * <h2>缓慢自转</h2>
 * 六边形以极慢速度单向自转（约 5°/s）。不为「好看」，
 * 而是为了让六边形的**棱**持续扫过视线 —— 静止的多边形容易被读成 UI 图标。
 */
final class NirvanaAuraEffect extends PowerAuraEffect {
    /** 出现 / 消失时长。消失比出现长（规范 §一基准）。 */
    private static final float APPEAR_TIME = 0.68f;
    private static final float VANISH_TIME = 0.88f;
    /** 层与层之间的显形错开（秒）。3 层错开 0.16s。 */
    private static final float LAYER_STEP = 0.08f;
    /** 锚点缓动速率。 */
    private static final float FOLLOW_LERP = 8.0f;
    /** 兴奋度的上升 / 回落（带阻尼）。 */
    private static final float EXCITE_RISE = 8.0f;
    private static final float EXCITE_FALL = 1.7f;
    /** 平时兴奋度基础值。 */
    private static final float EXCITE_IDLE = 0.18f;
    /** 自转速度（度/秒）。极慢：只要棱在动，不要读成「旋转的装饰」。 */
    private static final float SPIN_DEG_PER_SEC = 5.0f;
    /**
     * 层数变化的平滑速率。
     * <p>
     * 受击时 {@code currentBlock} 会一帧内跳变，层数也跳。
     * 用这个速率把「层数」平滑掉，护壁就会<b>渐缩</b>而不是<b>突减一层</b>。
     */
    private static final float LAYER_LERP = 6.0f;

    private final float[] spot = new float[2];

    private float appear;
    private float vanish;
    private float excite;
    /**
     * 平滑后的层数（可以是小数）。
     * <p>
     * 用小数是因为受击时层数会跳变，直接取整数会「啪」地少一层；
     * 小数版本能让外层<b>渐缩</b>回内侧。
     */
    private float smoothLayers;
    private float spin;
    /**
     * 锚点当前位置：本身已是平滑追踪值，绘制时直接用，
     * <b>不要再叠加滞后量</b>（先见之明踩过这个坑）。
     */
    private float followX;
    private float followY;
    private boolean anchored;

    NirvanaAuraEffect(AbstractCreature owner, String cardId, String powerId) {
        super(owner, cardId, powerId);
        // 护壁包住身体，画在人前面才能看出「在里面」
        this.renderBehind = false;
        this.appear = 0.0f;
        this.vanish = 0.0f;
        this.excite = EXCITE_IDLE;
        this.spin = 0.0f;
        this.smoothLayers = 1.0f;
    }

    /** 占卜得到格挡时调用：棱亮一档。 */
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
        // ---- 消失：层向内收回 ----
        if (this.isEnd) {
            this.vanish = Math.min(1.0f, this.vanish + dt / VANISH_TIME);
        } else if (this.vanish > 0.0f) {
            this.vanish = Math.max(0.0f, this.vanish - dt / VANISH_TIME);
        }

        // ---- 兴奋度：带阻尼（规范 §三.4）----
        float speed = EXCITE_IDLE > this.excite ? EXCITE_RISE : EXCITE_FALL;
        this.excite += (EXCITE_IDLE - this.excite)
                * MathUtils.clamp(dt * speed, 0.0f, 1.0f);

        // ---- 层数平滑：受击掉格挡时让外层渐缩，而不是「啪」地少一层 ----
        float target = NirvanaFx.layers(this.owner);
        this.smoothLayers += (target - this.smoothLayers)
                * MathUtils.clamp(dt * LAYER_LERP, 0.0f, 1.0f);

        // ---- 极慢自转 ----
        this.spin += dt * SPIN_DEG_PER_SEC;
        this.spin = this.spin % 360.0f;

        // ---- 锚点：缓动追角色 ----
        this.spot[0] = NirvanaFx.centerX(this.owner);
        this.spot[1] = NirvanaFx.centerY(this.owner);
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

        // ---- 出现：pow2Out 撑开；消失：向内收回 ----
        float growIn = Interpolation.pow2Out.apply(0.0f, 1.0f,
                MathUtils.clamp(this.appear, 0.0f, 1.0f));
        float outEase = Interpolation.pow2In.apply(0.0f, 1.0f,
                MathUtils.clamp(this.vanish, 0.0f, 1.0f));
        // 收回到 0.35 倍再淡出，读作「收进身体」
        float shellScale = growIn * MathUtils.lerp(1.0f, 0.35f, outEase);
        if (shellScale <= 0.02f) {
            return;
        }

        float cx = this.followX;
        float cy = this.followY;
        float bright = 0.82f + 0.18f * ex;
        float a = MathUtils.clamp(alpha * bright, 0.0f, 1.0f);

        // ---- 实际要画几层：平滑层数向上取整，配合每层的独立显形进度 ----
        // 例：smoothLayers = 2.4 → 画 3 层，但第 3 层的显形进度只有 0.4
        int full = (int) Math.floor(this.smoothLayers);
        float frac = this.smoothLayers - full;
        int draw = Math.min(NirvanaFx.MAX_LAYERS, full + (frac > 0.001f ? 1 : 0));

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        // 护壁内部一层整体的薄光，读作「这里有个壳」
        float baseR = NirvanaFx.radius(this.owner, 0) * shellScale;
        Draw.glow(sb, cx, cy, baseR * 3.2f, baseR * 3.0f, 0.0f,
                NirvanaFx.BRONZE, a * (0.06f + 0.06f * ex));

        // ---- 格挡已经用光：只画一圈很淡的薄壳 ----
        // 不能什么都不画，否则读作「能力消失了」
        if (!NirvanaFx.hasBlock(this.owner)) {
            NirvanaDraw.spentShell(sb, cx, cy, baseR,
                    baseR * NirvanaFx.flatten(0), this.spin, a);
            sb.setBlendFunction(src, dst);
            return;
        }

        for (int i = 0; i < draw; ++i) {
            // ---- 逐层错开显形：第 i 层需要 appear 超过 i × LAYER_STEP ----
            float need = (i * LAYER_STEP) / Math.max(0.001f, APPEAR_TIME);
            if (this.appear < need) {
                continue;
            }
            float own = MathUtils.clamp(
                    (this.appear - need) / Math.max(0.001f, 1.0f - need), 0.0f, 1.0f);
            float ownEase = Interpolation.swingOut.apply(0.0f, 1.0f, own);

            // 该层自己的"存在强度"：整层 full 内是 1，多出来的那层按 frac
            float presence = i < full ? 1.0f : frac;
            presence = MathUtils.clamp(presence, 0.0f, 1.0f);
            if (presence <= 0.01f) {
                continue;
            }
            // 层自身的撑开：刚出现的一层从 0.55 长到 1.0
            float layerGrow = MathUtils.lerp(0.55f, 1.0f, ownEase);

            float rx = NirvanaFx.radius(this.owner, i) * shellScale * layerGrow;
            float ry = rx * NirvanaFx.flatten(i);
            if (rx <= 2.0f) {
                continue;
            }

            // 越内层内衬越实（保护感更强），越外层越透
            float inner = MathUtils.lerp(1.0f, 0.35f,
                    i / (float) Math.max(1, NirvanaFx.MAX_LAYERS - 1));
            // 刚被顶出的那一层带过曝
            float flash = presence < 0.999f
                    ? (1.0f - presence) * 0.9f
                    : 0.0f;

            NirvanaDraw.shell(sb, cx, cy, rx, ry, this.spin,
                    inner, flash, a * presence * ownEase);
        }

        sb.setBlendFunction(src, dst);
    }
}
