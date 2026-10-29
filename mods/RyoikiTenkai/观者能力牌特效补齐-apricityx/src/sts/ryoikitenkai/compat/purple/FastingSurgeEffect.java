package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 「斋戒」<b>施放</b>时的演出：<b>红绿能量球从脚下涌上胸口，原版光圈依次亮起</b>。
 * <p>
 * <h2>光圈复用原版 {@link FastingEffect}</h2>
 * 与 {@link FastingTollEffect} 一致：不再自绘环，直接放原版光圈
 * （{@code FastingDraw.vanillaRing}）。原版曲线已调好，不会消失突兀。
 * <p>
 * 施放这张牌时原版自己也会放一圈 {@code CHARTREUSE} 色，
 * 这里补的是「红 + 绿」两圈，让玩家看出加的是力量与敏捷两种属性。
 * <h2>时间轴整体缩放</h2>
 * 所有阶段同比缩放，并由时间轴反推 {@code life}：
 * <pre>
 *   life = impactAt + 原版光圈时长 × timeScale + 错开 + 余量
 * </pre>
 * <h2>时间线（标准模式，总长约 2.94s）</h2>
 * <pre>
 *   0.00–0.34  起势   脚下粗白光收拢 + 地面压出暗环
 *   0.28–1.30  上升   magicNumber 串红绿交替能量球涌到胸口
 *   1.16       汇合   白色爆闪 + 红圈（原版光圈）
 *   1.30       第二圈 绿圈（原版光圈）
 *   1.56–2.94  余韵   圆形光点从胸口继续上飘，余弦淡出
 * </pre>
 * <h2>与「回合开始」互为镜像</h2>
 * <table border="1">
 *   <caption>两侧的视觉语法完全对称</caption>
 *   <tr><td></td><td>回合开始（失）</td><td>施放（得）</td></tr>
 *   <tr><td>起点</td><td>屏幕左下角的能量槽</td><td>角色脚下</td></tr>
 *   <tr><td>方向</td><td>从界面飞进战场</td><td>从地面升到胸口</td></tr>
 *   <tr><td>本体</td><td>一颗能量球（贴图）</td><td>一串红绿能量球</td></tr>
 * </table>
 * <h2>球串数 = magicNumber</h2>
 * 原版这张牌加 {@code magicNumber} 点力量与敏捷（3，升级 4），
 * 所以画 {@code magicNumber} 串能量球 —— 数字对得上。
 * <h2>禁止细线条</h2>
 * 全文没有任何 {@code Draw.line}。
 */
final class FastingSurgeEffect extends AbstractGameEffect {
    /** 标准模式下的起势时长（秒）。实际会乘 timeScale。 */
    private static final float RISE_IN_BASE = 0.34f;
    /** 标准模式下的上升时长（秒）。实际会乘 timeScale。 */
    private static final float ASCEND_BASE = 1.02f;
    /** 原版 FastingEffect 的时长（秒），用于反推 life。 */
    private static final float VANILLA_RING_TIME = 1.0f;
    /** 命中之后额外留的余量（秒，标准模式）。 */
    private static final float TAIL_PAD = 0.30f;
    /** 最多几串能量球。 */
    private static final int MAX_BEAMS = 5;
    /** 每串几个球。 */
    private static final int ORBS_PER_BEAM = 4;
    /** 每圈外飞的光点数。 */
    private static final int SPARKS = 14;

    private final float cx;
    private final float bottomY;
    private final float chestY;
    private final float bodyH;
    private final float orbR;
    private final int beams;
    private final float life;
    /** 时间整体缩放系数：所有阶段都用它，避免时间轴错位。 */
    private final float timeScale;
    /** 起势 / 上升 / 命中（都已乘 timeScale）。 */
    private final float riseIn;
    private final float ascendTime;
    private final float impactAt;
    /** 能量贴图（可能为 null，此时走光晕降级）。 */
    private final Texture tex;
    /** 能量光晕色：跟随当前角色。 */
    private final Color glowColor;

    /** 每串的横向偏移、出发延迟与形状种子（构造时定死，不每帧随机）。 */
    private final float[] offX = new float[MAX_BEAMS];
    private final float[] delay = new float[MAX_BEAMS];
    private final float[] seed = new float[MAX_BEAMS];

    private float time;
    private float excite;
    /** 球串的自转角。 */
    private float spin;
    /** 红圈 / 绿圈是否已放出。 */
    private boolean redFired;
    private boolean greenFired;

    FastingSurgeEffect(AbstractCreature owner, int magicNumber) {
        // 整体时间缩放：快进模式下压缩到 0.72，但各阶段同比缩放
        this.timeScale = Settings.FAST_MODE ? 0.72f : 1.0f;
        this.riseIn = RISE_IN_BASE * this.timeScale;
        this.ascendTime = ASCEND_BASE * this.timeScale;

        this.cx = BodyFx.centerX(owner);
        this.bottomY = BodyFx.groundY(owner);
        this.chestY = FastingFx.chestY(owner);
        this.bodyH = BodyFx.height(owner);
        this.orbR = Math.max(8.0f * Settings.scale, this.bodyH * 0.058f);
        // 球串数 = 加的力量/敏捷点数，一一对应
        this.beams = MathUtils.clamp(magicNumber, 1, MAX_BEAMS);
        // 能量贴图 + 颜色：和回合开始那颗球同一套，所以看起来是「同一种能量」
        this.tex = FastingDraw.energyTexture();
        this.glowColor = FastingFx.energyColor();

        for (int i = 0; i < MAX_BEAMS; ++i) {
            float k = i - (this.beams - 1) * 0.5f;
            // 以角色中轴为中心横向铺开
            this.offX[i] = k * this.orbR * 2.6f;
            // 中间那串最先出发，两侧稍晚
            this.delay[i] = Math.abs(k) * 0.06f * this.timeScale;
            this.seed[i] = MathUtils.random(0.0f, MathUtils.PI2);
        }

        // 命中时刻 = 起势 + 上升的 78%
        this.impactAt = this.riseIn + this.ascendTime * 0.78f;
        // 总时长 = 命中 + 原版光圈时长 + 绿圈错开 + 余量（全部同比缩放）
        this.life = this.impactAt
                + VANILLA_RING_TIME * this.timeScale
                + FastingFx.RING_STEP * this.timeScale
                + TAIL_PAD * this.timeScale;
        this.startingDuration = this.duration = this.life;
        this.renderBehind = false;
    }

    @Override
    public void update() {
        float dt = Gdx.graphics.getDeltaTime();
        this.time += dt;
        this.duration -= dt;
        this.spin += dt * 130.0f;

        // ---- 命中瞬间：放原版光圈（红先）----
        if (!this.redFired && this.time >= this.impactAt) {
            this.redFired = true;
            FastingDraw.vanillaRing(this.cx, this.chestY, FastingDraw.STRENGTH);
        }
        // ---- 错开 RING_STEP 后：放绿圈 ----
        if (this.redFired && !this.greenFired
                && this.time >= this.impactAt + FastingFx.RING_STEP * this.timeScale) {
            this.greenFired = true;
            FastingDraw.vanillaRing(this.cx, this.chestY, FastingDraw.DEXTERITY);
        }

        if (this.duration <= 0.0f) {
            this.isDone = true;
            return;
        }
        // 状态：带阻尼（规范 §三.4）。起势快、回落慢。
        float target = this.time < this.impactAt + 0.40f * this.timeScale
                ? 1.0f : 0.24f;
        float speed = target > this.excite ? 15.0f : 2.2f;
        this.excite += (target - this.excite) * MathUtils.clamp(dt * speed, 0.0f, 1.0f);
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.duration <= 0.0f) {
            return;
        }
        float ex = MathUtils.clamp(this.excite, 0.0f, 1.0f);
        float punch = 0.82f + 0.38f * ex;
        float breath = 0.5f + 0.5f * MathUtils.sin(this.time * MathUtils.PI2 / 0.85f);

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        // ================= 0. 起势：脚下粗白光收拢 + 地面暗环 =================
        if (this.time < this.riseIn + 0.20f * this.timeScale) {
            float q = MathUtils.clamp(this.time / this.riseIn, 0.0f, 1.0f);
            float out = 1.0f - MathUtils.clamp(
                    (this.time - this.riseIn) / (0.20f * this.timeScale), 0.0f, 1.0f);
            float shrink = Interpolation.pow2Out.apply(0.0f, 1.0f, q);
            // 地面被压出的暗环：读作「有东西要从地里顶出来」
            Draw.arc(sb, this.cx, this.bottomY, this.orbR * 11.0f,
                    this.orbR * 3.2f, 0.0f, 360.0f,
                    Math.max(5.0f, this.orbR * 0.50f), FastingDraw.DRAINED,
                    out * 0.72f, 30);
            // 收拢的白光：粗环，从大缩到小
            Draw.arc(sb, this.cx, this.bottomY,
                    this.orbR * MathUtils.lerp(12.6f, 4.1f, shrink),
                    this.orbR * MathUtils.lerp(3.8f, 1.3f, shrink),
                    0.0f, 360.0f, Math.max(6.0f, this.orbR * 0.62f),
                    FastingDraw.CORE, out * 0.88f, 32);
            Draw.glow(sb, this.cx, this.bottomY, this.orbR * 11.0f * out,
                    this.orbR * 3.6f * out, 0.0f, FastingDraw.CORE, out * 0.55f);
        }

        // ================= 1. 上升：红绿交替的能量球串 =================
        for (int i = 0; i < this.beams; ++i) {
            float local = this.time - this.riseIn - this.delay[i];
            if (local <= 0.0f) {
                continue;
            }
            float q = MathUtils.clamp(local / this.ascendTime, 0.0f, 1.0f);
            // pow3Out：起手极快、末端减速 —— 读作「一口气涌上来」
            float rise = Interpolation.pow3Out.apply(0.0f, 1.0f, q);

            // 红绿交替：奇偶决定颜色
            Color c = (i % 2 == 0) ? FastingDraw.STRENGTH : FastingDraw.DEXTERITY;
            // 淡出：涌到胸口后用余弦化掉，不再是线性硬切
            float beamFade = q < 0.80f ? 1.0f
                    : FastingDraw.cosineOut((q - 0.80f) / 0.20f);
            if (beamFade <= 0.01f) {
                continue;
            }
            float bx = this.cx + this.offX[i];

            // 一串球：头部在 rise 处，后面几个依次落后
            for (int k = 0; k < ORBS_PER_BEAM; ++k) {
                float f = rise - k * 0.11f;
                if (f <= 0.0f) {
                    continue;
                }
                float oy = this.bottomY + (this.chestY - this.bottomY) * f;
                // 横向轻微蛇行，读作能量在跳动
                float ox = bx + MathUtils.sin(this.seed[i] + k * 2.1f
                        + this.time * 5.5f) * this.orbR * 0.55f;
                // 越靠后越小越淡
                float kf = k / (float) ORBS_PER_BEAM;
                float r = this.orbR * (1.0f - 0.42f * kf);
                float a = beamFade * (1.0f - 0.55f * kf) * punch;

                // 球体：用能量贴图（有纹理的实体，不是一团光）
                if (this.tex != null) {
                    float size = r * 2.6f;
                    sb.setColor(Draw.tint(Color.WHITE, a * 0.92f));
                    sb.draw(this.tex, ox - size * 0.5f, oy - size * 0.5f,
                            size * 0.5f, size * 0.5f, size, size,
                            1.0f, 1.0f, this.spin - k * 40.0f, 0, 0,
                            this.tex.getWidth(), this.tex.getHeight(), false, false);
                }
                // 光晕：颜色就是红/绿，所以两种属性一眼可分
                Draw.glow(sb, ox, oy, r * 4.6f, r * 4.6f, 0.0f, c, a * 0.34f);
                Draw.glow(sb, ox, oy, r * 2.2f, r * 2.2f, 0.0f, c, a * 0.62f);
                Draw.glow(sb, ox, oy, r * 0.95f, r * 0.95f, 0.0f,
                        FastingDraw.CORE, a * 0.55f);
            }

            // 头部额外一团亮光：读作「涌到哪了」
            if (rise > 0.02f) {
                float headY = this.bottomY + (this.chestY - this.bottomY) * rise;
                Draw.glow(sb, bx, headY, this.orbR * 6.0f, this.orbR * 6.0f, 0.0f,
                        c, beamFade * punch * 0.34f);
                Draw.glow(sb, bx, headY, this.orbR * 2.4f, this.orbR * 2.4f, 0.0f,
                        FastingDraw.CORE, beamFade * punch * 0.50f);
            }
        }

        // ================= 2. 汇合：白色爆闪（环交给原版光圈） =================
        float since = this.time - this.impactAt;
        if (since >= 0.0f) {
            // 白爆：0.45s（同比缩放），末端余弦化掉
            float flashSpan = 0.45f * this.timeScale;
            float flashQ = MathUtils.clamp(since / flashSpan, 0.0f, 1.0f);
            if (flashQ < 1.0f) {
                float bright = flashQ < 0.16f ? flashQ / 0.16f
                        : FastingDraw.cosineOut((flashQ - 0.16f) / 0.84f);
                if (bright > 0.01f) {
                    float grow = 1.0f + 0.60f * flashQ;
                    Draw.glow(sb, this.cx, this.chestY,
                            this.orbR * 9.0f * grow, this.orbR * 7.8f * grow,
                            0.0f, FastingDraw.CORE, bright * 0.90f * punch);
                    Draw.glow(sb, this.cx, this.chestY,
                            this.orbR * 3.8f, this.orbR * 3.4f, 0.0f,
                            FastingDraw.CORE, bright * 0.82f);
                }
            }
        }

        // ================= 3. 余韵：从胸口继续向上飘的圆点（余弦淡出） =================
        float tailStart = 0.40f * this.timeScale;
        float tailSpan = 1.60f * this.timeScale;
        float tailQ = MathUtils.clamp((since - tailStart) / tailSpan, 0.0f, 1.0f);
        if (tailQ > 0.0f && tailQ < 1.0f) {
            float fade = FastingDraw.cosineOut(tailQ);
            for (int k = 0; k < 10; ++k) {
                float ph = (tailQ * 1.5f + k / 10.0f) % 1.0f;
                float px = this.cx + MathUtils.sin(k * 2.7f + this.time * 2.0f)
                        * this.orbR * 1.8f;
                float py = this.chestY + this.bodyH * 0.36f * ph;
                float r = Math.max(2.4f, this.orbR * 0.42f) * (1.0f - 0.50f * ph);
                Color c = (k % 2 == 0) ? FastingDraw.STRENGTH : FastingDraw.DEXTERITY;
                FastingDraw.spark(sb, px, py, r, fade * 0.85f, c);
            }
        }

        sb.setBlendFunction(src, dst);
    }

    @Override
    public void dispose() {
    }
}
