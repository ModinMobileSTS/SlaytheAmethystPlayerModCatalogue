package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 「虔信」<b>真言满 10、升入神性</b>时的演出：
 * <b>十根刻度齐亮 → 整圈向心收束 → 神性粉光从体内炸开</b>。
 * <p>
 * <h2>这个动作对应什么</h2>
 * 原版在两处会触发同一件事：
 * <pre>
 * // MantraPower.stackPower()  —— 真言满 10
 * if (this.amount &gt;= 10) {
 *     addToTop(new ChangeStanceAction("Divinity"));
 *     this.amount -= 10;
 * }
 *
 * // DevotionPower.atStartOfTurnPostDraw()  —— 没有真言且层数够时
 * if (!player.hasPower("Mantra") &amp;&amp; this.amount &gt;= 10) {
 *     addToBot(new ChangeStanceAction("Divinity"));
 * }
 * </pre>
 * 两处都是「真言被献出、换来了神性」。所以画面是
 * <b>刻度齐亮 → 收进身体 → 神性粉光炸开</b>，而不是「又多了一圈光」。
 * <h2>本轮返工：加大加亮（用户反馈「特效不够明显」）</h2>
 * <ul>
 *   <li><b>齐亮段</b>：十根刻度的长度从 {@code rx×0.16} 提到 <b>{@code rx×0.34}</b>，
 *       厚度提到 {@code rx×0.14}，并叠一层整圈的「轨道过曝」</li>
 *   <li><b>收束段</b>：收束终点的环厚度提到 <b>{@code arx×0.16}</b>，
 *       并加<b>向内吸的光点</b>（10 颗，沿半径往中心跑）——
 *       让「真言被吸进身体」有可以数的实体</li>
 *   <li><b>爆发段</b>：主闪光纵向拉伸到 {@code ry×18}，冲击环扩到 {@code rx×5.2}；
 *       再加<b>一圈向外飞的刻度光点</b>，把刚才那 10 根刻度「献出去」的动作补完</li>
 *   <li>全程走<b>最顶层队列</b>（{@link DevotionFx#onAscend} 用 {@code queueTop}），
 *       否则爆发段的上半截会被手牌压住</li>
 * </ul>
 * <h2>时间线（总长 2.45s，{@code FAST_MODE} × 0.72）</h2>
 * <pre>
 *   0.00–0.46  齐亮   十根刻度同时炸亮 + 整圈轨道过曝，向外弹一下
 *   0.30–0.52  蓄力   中心核逐帧变亮（读作「满了」）
 *   0.46–1.30  收束   整圈缩向角色，十颗光点沿半径被吸进中心
 *   1.30–1.86  爆发   粉白闪光纵向拉满 + 一圈冲击环 + 十颗刻度光向外飞
 *   1.86–2.45  余韵   粉色光点向上飘散，余弦淡出
 * </pre>
 * <h2>为什么是「收束」而不是「扩散」</h2>
 * 扩散读作「获得力量」，收束读作「<b>献出、转换</b>」。
 * 虔信做的是把攒下的真言<b>交出去</b>换取神性姿态，所以主体必须是收束。
 * 扩散留给爆发那一段（收束之后向外炸），两段方向相反，叙事才完整。
 * <h2>颜色</h2>
 * 原版 {@code DivinityStance.onEnterStance()} 用 {@code Color.PINK} 做边框闪光，
 * 所以这里爆发段全部用 {@link DevotionFx#DIVINITY}，与姿态切换的光呼应。
 * <h2>声音与震屏</h2>
 * 原版进入神性自己会播 {@code STANCE_ENTER_DIVINITY} 与 {@code BorderFlashEffect(PINK)}，
 * 所以这里<b>不再加音效与震屏</b>，只补视觉细节。
 */
final class DevotionAscendEffect extends AbstractGameEffect {
    /** 齐亮时长（标准模式，会乘 timeScale）。 */
    private static final float BURST_BASE = 0.46f;
    /** 收束时长。 */
    private static final float COLLAPSE_BASE = 0.84f;
    /** 爆发与余韵。 */
    private static final float BLOOM_BASE = 1.15f;
    /** 向外飞的刻度光点数。 */
    private static final int SHARDS = 10;

    private final float cx;
    private final float cy;
    private final float footY;
    private final float rx;
    private final float ry;
    private final float bodyH;
    private final float life;
    private final float timeScale;
    private final float burstTime;
    private final float collapseTime;

    private float time;
    private float excite;

    DevotionAscendEffect(AbstractCreature owner) {
        this.timeScale = Settings.FAST_MODE ? 0.72f : 1.0f;
        this.burstTime = BURST_BASE * this.timeScale;
        this.collapseTime = COLLAPSE_BASE * this.timeScale;

        this.cx = DevotionFx.centerX(owner);
        this.cy = DevotionFx.centerY(owner);
        this.footY = BodyFx.groundY(owner);
        this.rx = DevotionFx.radiusX(owner);
        this.ry = DevotionFx.radiusY(owner);
        this.bodyH = BodyFx.height(owner);

        this.life = (BURST_BASE + COLLAPSE_BASE + BLOOM_BASE) * this.timeScale;
        this.startingDuration = this.duration = this.life;
        // 爆发段会漫到角色上方，必须是顶层
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
        // 状态：带阻尼。齐亮瞬间拉满，之后缓慢回落。
        float target = this.time < this.burstTime + this.collapseTime * 0.55f
                ? 1.0f : 0.25f;
        float speed = target > this.excite ? 16.0f : 2.0f;
        this.excite += (target - this.excite) * MathUtils.clamp(dt * speed, 0.0f, 1.0f);
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.duration <= 0.0f) {
            return;
        }
        float ex = MathUtils.clamp(this.excite, 0.0f, 1.0f);
        float punch = 0.90f + 0.32f * ex;
        // 全程用神性粉：这一段就是「献出真言换来的」
        Color main = new Color(DevotionFx.DIVINITY);
        Color core = new Color(DevotionFx.DIVINITY_CORE);

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        // ================= 1. 齐亮：十根刻度同时炸白 =================
        if (this.time < this.burstTime) {
            float q = MathUtils.clamp(this.time / this.burstTime, 0.0f, 1.0f);
            // 环整体短时放大（读作「满值撑开」）
            float swell = 1.0f + 0.20f * MathUtils.sin(q * MathUtils.PI);
            // 轨道本身也过曝一下，让「整圈」都在发光而不只是刻度
            float trackA = q < 0.20f ? q / 0.20f
                    : FastingDraw.cosineOut((q - 0.20f) / 0.80f);
            DevotionDraw.track(sb, this.cx, this.cy,
                    this.rx * swell, this.ry * swell, trackA * 1.3f, main);
            DevotionDraw.tickBurst(sb, this.cx, this.cy,
                    this.rx * swell, this.ry * swell, q, 1.0f, main, core);
            // 中心一颗粉色核心：随蓄力变亮
            float cr = this.rx * (0.40f + 0.45f * q);
            Draw.glow(sb, this.cx, this.cy, cr * 4.2f, cr * 3.4f, 0.0f,
                    main, 0.34f * punch * (1.0f - q * 0.4f));
            Draw.glow(sb, this.cx, this.cy, cr * 1.8f, cr * 1.5f, 0.0f,
                    main, 0.50f * (1.0f - q * 0.3f));
            Draw.glow(sb, this.cx, this.cy, cr * 0.8f, cr * 0.7f, 0.0f,
                    core, 0.85f * (1.0f - q * 0.2f));
        }

        // ================= 2. 收束：整圈缩向角色 + 十颗光点被吸进去 =================
        float since = this.time - this.burstTime;
        if (since >= 0.0f && since < this.collapseTime) {
            float q = MathUtils.clamp(since / this.collapseTime, 0.0f, 1.0f);
            DevotionDraw.collapse(sb, this.cx, this.cy, this.rx, this.ry,
                    q, punch, main, core);

            // 收得越紧，中心越亮 —— 读作「全都收进去了」
            float cr = this.rx * (0.20f + 0.55f * q);
            Draw.glow(sb, this.cx, this.cy, cr * 2.8f, cr * 2.4f, 0.0f,
                    main, 0.32f * punch * q);
            Draw.glow(sb, this.cx, this.cy, cr * 1.3f, cr * 1.1f, 0.0f,
                    core, 0.70f * q);

            // 十颗刻度光：沿半径从外往内跑，与刻度一一对应
            // 读作「刚才那十根，一根一根被收进体内」
            float inward = 1.0f - (1.0f - q) * (1.0f - q);
            float shardA = q < 0.15f ? q / 0.15f : 1.0f;
            for (int i = 0; i < SHARDS; ++i) {
                float deg = -90.0f + 360.0f * i / SHARDS;
                float rad = deg * MathUtils.degreesToRadians;
                float cs = MathUtils.cos(rad);
                float sn = MathUtils.sin(rad);
                // 各自轻微错开，避免整齐得像机关
                float s = MathUtils.clamp(inward * (0.85f + 0.15f
                        * MathUtils.sin(i * 1.7f)), 0.0f, 1.0f);
                float r = MathUtils.lerp(1.0f, 0.04f, s);
                float px = this.cx + cs * this.rx * r;
                float py = this.cy + sn * this.ry * r;
                float size = Math.max(6.0f * Settings.scale, this.rx * 0.075f)
                        * (1.0f - 0.45f * s);
                Draw.glow(sb, px, py, size * 5.0f, size * 5.0f, 0.0f,
                        main, 0.26f * shardA * punch);
                Draw.glow(sb, px, py, size * 2.0f, size * 2.0f, 0.0f,
                        main, 0.72f * shardA * punch);
                Draw.glow(sb, px, py, size * 0.85f, size * 0.85f, 0.0f,
                        core, 0.90f * shardA);
            }
        }

        // ================= 3. 爆发：粉白闪光 + 冲击环 + 刻度光向外飞 =================
        float bloom = this.time - this.burstTime - this.collapseTime;
        if (bloom >= 0.0f) {
            float totalBloom = Math.max(0.001f,
                    this.life - this.burstTime - this.collapseTime);
            float q = MathUtils.clamp(bloom / totalBloom, 0.0f, 1.0f);
            // 亮度：起势极快、末端余弦化掉
            float bright = q < 0.14f ? q / 0.14f
                    : FastingDraw.cosineOut((q - 0.14f) / 0.86f);
            if (bright > 0.01f) {
                // 中央粉白闪光：纵向拉满整条身体，读作「升上去了」
                Draw.glow(sb, this.cx, this.cy + this.bodyH * 0.12f,
                        this.rx * (2.6f + 3.8f * q),
                        this.ry * (8.0f + 10.0f * q),
                        0.0f, main, 0.56f * bright * punch);
                Draw.glow(sb, this.cx, this.cy + this.bodyH * 0.10f,
                        this.rx * (1.2f + 1.8f * q),
                        this.ry * (3.4f + 4.4f * q),
                        0.0f, core, 0.72f * bright);

                // 冲击环：向外扩的粗椭圆环（贴地视角，横向更宽）
                float grow = 1.0f - (1.0f - q) * (1.0f - q);
                float ringR = this.rx * (1.0f + 4.2f * grow);
                Draw.arc(sb, this.cx, this.cy, ringR, ringR * DevotionFx.FLATTEN,
                        0.0f, 360.0f,
                        Math.max(8.0f * Settings.scale, this.rx * 0.15f * bright),
                        main, bright * 0.85f * punch, 40);
                Draw.arc(sb, this.cx, this.cy, ringR * 0.90f,
                        ringR * DevotionFx.FLATTEN * 0.90f,
                        0.0f, 360.0f,
                        Math.max(5.0f * Settings.scale, this.rx * 0.07f * bright),
                        core, bright * 0.62f, 34);
                // 环本身向外的一层体积光
                Draw.glow(sb, this.cx, this.cy, ringR * 2.4f,
                        ringR * DevotionFx.FLATTEN * 2.4f, 0.0f,
                        main, 0.24f * bright * punch);

                // 十颗刻度光向外飞：把「刻度被献出去」补完
                float flyQ = MathUtils.clamp((q - 0.06f) / 0.94f, 0.0f, 1.0f);
                float flyA = FastingDraw.cosineOut(flyQ);
                float flyR = MathUtils.lerp(0.10f, 2.4f,
                        1.0f - (1.0f - flyQ) * (1.0f - flyQ));
                for (int i = 0; i < SHARDS; ++i) {
                    float deg = -90.0f + 360.0f * i / SHARDS;
                    float rad = deg * MathUtils.degreesToRadians;
                    float px = this.cx + MathUtils.cos(rad) * this.rx * flyR;
                    float py = this.cy + MathUtils.sin(rad) * this.ry * flyR;
                    float size = Math.max(5.0f * Settings.scale, this.rx * 0.075f)
                            * (1.0f - 0.5f * flyQ);
                    Draw.glow(sb, px, py, size * 4.6f, size * 4.6f, 0.0f,
                            main, flyA * 0.30f * punch);
                    Draw.glow(sb, px, py, size * 1.8f, size * 1.8f, 0.0f,
                            main, flyA * 0.74f);
                    Draw.glow(sb, px, py, size * 0.8f, size * 0.8f, 0.0f,
                            core, flyA * 0.88f);
                }
            }

            // ================= 4. 余韵：粉色光点向上飘散 =================
            float tailQ = MathUtils.clamp((q - 0.18f) / 0.82f, 0.0f, 1.0f);
            if (tailQ > 0.0f && tailQ < 1.0f) {
                float fade = FastingDraw.cosineOut(tailQ);
                for (int k = 0; k < 12; ++k) {
                    float ph = (tailQ * 1.4f + k / 12.0f) % 1.0f;
                    // 横向散开，然后向上飘
                    float spread = MathUtils.sin(k * 2.7f) * this.rx * 0.95f;
                    float px = this.cx + spread * (0.5f + 0.5f * ph);
                    float py = this.cy + this.bodyH * 0.55f * ph;
                    float r = Math.max(4.0f * Settings.scale, this.rx * 0.085f)
                            * (1.0f - 0.5f * ph);
                    Draw.glow(sb, px, py, r * 4.0f, r * 4.0f, 0.0f,
                            main, fade * 0.30f);
                    Draw.glow(sb, px, py, r * 1.7f, r * 1.7f, 0.0f,
                            core, fade * 0.52f);
                }
                // 从脚底一路升起的粉色余晖：读作「整条身体都升上去了」
                float h = this.cy - this.footY;
                if (h > 1.0f) {
                    Draw.glow(sb, this.cx, this.footY + h * 0.5f,
                            this.rx * 2.2f, h * 1.3f, 0.0f, main, fade * 0.26f);
                }
            }
        }

        sb.setBlendFunction(src, dst);
    }

    @Override
    public void dispose() {
    }
}
