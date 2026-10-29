package sts.ryoikitenkai.compat.green;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 打出「无限刀刃」的施放演出：<b>多把匕首坠落地面 → 浮起、刃尖转向右 → 悬浮在人物右侧</b>。
 * <p>
 * <h2>三段动作</h2>
 * <pre>
 *   ① 坠落   各把匕首从上方错拍砸到地面，每把落地溅一圈贴地光
 *   ② 停顿   全部落地后停一拍 —— 让「地上插着一排刀」这个画面成立
 *   ③ 浮起   升到悬浮区，同时刃尖从「朝下」转成「朝右」
 *   ④ 就位   交接给常驻：一排同向的刀悬在右侧
 * </pre>
 * 「插在地上」= 刚被扔下来；「转成朝右」= 被操纵、排成阵列。
 * 落地必须有溅光，否则刀读作「凭空出现在地上」。
 * <h2>动画曲线（本轮修的「不流畅」）</h2>
 * <ul>
 *   <li>坠落：{@link GreenDraw#easeIn}（pow2In）—— 越接近地面越快，重力感</li>
 *   <li>浮起：{@link GreenDraw#easeOut}（pow2Out）—— 起手快、到位慢</li>
 *   <li>刃尖转向：与浮起<b>同一进度</b>做夹角插值，所以「一边升一边转」，
 *       不会先升完再转</li>
 *   <li>包络：{@link GreenDraw#smoothstep} + 余弦淡出 —— 两端斜率都收敛，不硬切也不死平</li>
 * </ul>
 * 全程<b>不用</b> {@code swingOut}：它冲到 1 以上再 {@code clamp} 会留下一段死平，
 * 读作「卡了一下才停」。
 * <h2>交接不跳位（规范 §2.13）</h2>
 * 终点每帧重算 {@link BladesFx#centerX/centerY/bladeLen} 与
 * {@link BladesFx#slotOffset}，与常驻同一组函数；刃尖方向都是
 * {@link BladesFx#DIR_DEG}。
 * <h2>频次（规范 §2.11）</h2>
 * 一局几次 → 允许轻震屏（在登记表里发）。
 * <b>不补音效</b>：{@code InfiniteBladesPower} 没重写 {@code playApplyPowerSfx}，
 * 会走 {@code AbstractPower} 默认实现播 {@code BUFF_1~3}，再补就双响。
 */
final class BladesSummonEffect extends AbstractGameEffect {
    /** 总时长。 */
    private static final float LIFE = 1.58f;

    /** ① 坠落：首把起飞时刻、逐把错开、单把下落跨度（均为归一化 p）。 */
    private static final float P_FALL_START = 0.02f;
    private static final float P_FALL_STEP = 0.045f;
    private static final float P_FALL_SPAN = 0.26f;
    /** ② 停顿结束 = ③ 浮起开始。 */
    private static final float P_HOVER_START = 0.55f;
    private static final float P_HOVER_END = 0.86f;
    /** ④ 收尾淡出。 */
    private static final float P_FADE_START = 0.92f;

    /** 落地点横向铺开的范围（× 刀长）。 */
    private static final float GROUND_SPREAD = 1.05f;
    /** 匕首起始高度（× 身高）：从画面上方砸下。 */
    private static final float FALL_FROM = 1.05f;
    /** 落地时插入地面多深（× 刀长）。 */
    private static final float STUCK = 0.18f;
    /** 落地溅光的持续跨度（归一化 p）。 */
    private static final float IMPACT_SPAN = 0.20f;

    private final AbstractCreature owner;
    private final int count;

    private final Color green = new Color();
    private final Color blade = new Color();
    private final Color crack = new Color();
    private final Color wood = new Color();
    private final Color guard = new Color();

    /** 槽位计算用的复用数组（长度 2）。 */
    private final float[] slot = new float[2];

    /** 累计时间（驱动每把刀的错相轻浮）。 */
    private float elapsed;

    private float p;

    BladesSummonEffect(AbstractCreature owner, int count) {
        this.owner = owner;
        this.count = MathUtils.clamp(count, 3, BladesFx.MAX_BLADES);
        this.startingDuration = LIFE;
        this.duration = LIFE;
        // 匕首在身上，画在人前面
        this.renderBehind = false;
    }

    @Override
    public void update() {
        float dt = Gdx.graphics.getDeltaTime();
        this.elapsed += dt;
        this.p = Math.min(1.0f, this.p + dt / LIFE);
        this.duration = LIFE * (1.0f - this.p);
        if (this.p >= 1.0f) {
            this.isDone = true;
        }
    }

    /** 第 {@code i} 把的起飞时刻（归一化）。 */
    private float fallStart(int i) {
        return P_FALL_START + i * P_FALL_STEP;
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.owner == null) {
            return;
        }
        // 整体包络：smoothstep 淡入 + 余弦淡出（两端斜率都收敛）
        float fadeIn = GreenDraw.smoothstep(MathUtils.clamp(this.p / 0.05f, 0.0f, 1.0f));
        float outT = MathUtils.clamp(
                (this.p - P_FADE_START) / (1.0f - P_FADE_START), 0.0f, 1.0f);
        float alpha = fadeIn * (0.5f + 0.5f * MathUtils.cos(outT * MathUtils.PI));
        if (alpha <= 0.004f) {
            return;
        }

        this.green.set(BladesFx.GREEN);
        this.blade.set(BladesFx.BLADE);
        this.crack.set(BladesFx.CRACK);
        this.wood.set(BladesFx.WOOD);
        this.guard.set(BladesFx.GUARD);

        float cx = BladesFx.centerX(this.owner);
        float cy = BladesFx.centerY(this.owner);
        float groundY = BladesFx.groundY(this.owner);
        float len = BladesFx.bladeLen(this.owner);
        float h = BodyFx.height(this.owner);
        int n = this.count;

        // ③ 浮起进度（pow2Out）
        float hoverT = MathUtils.clamp(
                (this.p - P_HOVER_START) / (P_HOVER_END - P_HOVER_START), 0.0f, 1.0f);
        float hover = GreenDraw.easeOut(hoverT);

        int blendSrc = sb.getBlendSrcFunc();
        int blendDst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        for (int i = 0; i < n; ++i) {
            // ---- 落地点：地面上一排（居中均分）----
            float gx = n <= 1 ? cx
                    : cx + ((i / (float) (n - 1)) - 0.5f) * 2.0f * len * GROUND_SPREAD;

            // ---- ① 坠落：从画面上方垂直砸下（pow2In 重力感）----
            float fallT = MathUtils.clamp(
                    (this.p - fallStart(i)) / P_FALL_SPAN, 0.0f, 1.0f);
            float drop = GreenDraw.easeIn(fallT);

            float landY = groundY - len * STUCK;
            float fromY = groundY + h * FALL_FROM;
            float gy = MathUtils.lerp(fromY, landY, drop);

            // ---- 落地溅光 ----
            float sinceLand = this.p - (fallStart(i) + P_FALL_SPAN);
            if (sinceLand > 0.0f && sinceLand < IMPACT_SPAN) {
                BladesDraw.impact(sb, gx, groundY, len, alpha * 0.95f,
                        MathUtils.clamp(sinceLand / IMPACT_SPAN, 0.0f, 1.0f));
            }

            // ---- ②③ 姿态：落地时竖插（刃尖朝下 −90°），浮起后朝右（DIR_DEG）----
            float px;
            float py;
            float dirDeg;
            if (this.p < P_HOVER_START) {
                dirDeg = -90.0f;
                px = gx;
                py = gy;
            } else {
                BladesFx.slotOffset(i, n, len, this.slot);
                float ox = cx + this.slot[0];
                float oy = cy + this.slot[1] + BladesFx.bobOffset(i, this.elapsed);
                px = MathUtils.lerp(gx, ox, hover);
                py = MathUtils.lerp(landY, oy, hover);
                // 从 −90° 转到 0°（朝右），与浮起同一进度：边升边转
                dirDeg = MathUtils.lerp(-90.0f, BladesFx.DIR_DEG, hover);
            }

            float a = alpha * 0.95f;
            // 下落与浮起瞬间能量更亮
            float g = MathUtils.clamp(
                    0.40f + 0.55f * drop * (1.0f - hover) + 0.30f * hover, 0.0f, 1.0f);
            BladesDraw.dagger(sb, px, py, len, dirDeg, a, g);
        }

        sb.setBlendFunction(blendSrc, blendDst);
    }

    @Override
    public void dispose() {
    }
}
