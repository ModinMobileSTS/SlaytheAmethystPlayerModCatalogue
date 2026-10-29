package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 「研习」<b>被打出</b>时的演出：<b>页从胸口飞向左下角，铺成扇面</b>。
 * <p>
 * <h2>为什么必须有这一段</h2>
 * 与先见之明<b>相反</b>：{@code Study.use()} 里<b>一个特效都没有</b>
 * （只有 {@code ApplyPowerAction}）。所以打出这张牌时画面上完全没有任何变化 ——
 * 玩家会怀疑牌没生效。
 * <p>
 * 这里要同时说清两件事：
 * <ol>
 *   <li><b>装上了</b>（有东西生成、飞出去）</li>
 *   <li><b>装在哪里</b>（落到抽牌堆上方）—— 这条尤其重要，
 *       因为它的常驻表现不在身上，而在左下角。
 *       如果不演「飞过去」，玩家会以为身上应该有东西，然后找不到</li>
 * </ol>
 * <h2>时间线（总长 1.55s，{@code FAST_MODE} × 0.72）</h2>
 * <pre>
 *   0.00–0.30  起势   胸口亮起一小团，页成形（swingOut 由 0.3 弹到 1.0）
 *   0.26–1.14  飞渡   页沿圆弧飞向左下角，边飞边翻、逐渐放大到位
 *   1.14–1.28  落位   页在扇面上短暂定住，墨光闪一下
 *   1.28–1.55  余韵   扇面整体淡入（常驻接管），余弦淡出
 * </pre>
 * <h2>为什么飞的是圆弧</h2>
 * 直线会让它读成「一个图标被拖拽过去」。圆弧有离心感，读作「被抛出去、然后落位」。
 * 控制点上提，让轨迹先向右上拱一点再落下 —— 弧线更明显。
 * <h2>与常驻的交接</h2>
 * 本段的页最终位置与常驻的扇面位置<b>完全相同</b>（都取自
 * {@link StudyFx#pageX}/{@link StudyFx#pageY}），所以交付时不会跳位。
 * 这也是规范 §六「常驻与一次性特效必须共享同一批常量」的直接应用。
 * <h2>声音与震屏</h2>
 * 一局只几次 —— 允许轻震屏 + 音效（原版这里一点反馈都没有）。
 * 音效走 200ms 去重。
 */
final class StudyAssignEffect extends AbstractGameEffect {
    /** 起势时长（标准模式，会乘 timeScale）。 */
    private static final float FORM_BASE = 0.30f;
    /** 飞渡时长。 */
    private static final float FLY_BASE = 0.88f;
    /** 落位定住时长。 */
    private static final float LAND_BASE = 0.14f;
    /** 余韵时长。 */
    private static final float TAIL_BASE = 0.27f;
    /** 页与页之间错开（多张研习叠加时）。 */
    private static final float STAGGER_BASE = 0.10f;

    private final float timeScale;
    private final float formTime;
    private final float flyTime;
    private final float landTime;
    private final float tailTime;
    private final float stagger;
    private final int count;
    private final float life;

    private final float fromX;
    private final float fromY;
    private final float[] toX;
    private final float[] toY;
    private final float[] tilt;
    /** 弧线的控制点偏移方向：统一向右上拱。 */
    private final float ctrlX;
    private final float ctrlY;

    private float time;
    private float excite;

    StudyAssignEffect(AbstractCreature owner, int count) {
        this.timeScale = Settings.FAST_MODE ? 0.72f : 1.0f;
        this.formTime = FORM_BASE * this.timeScale;
        this.flyTime = FLY_BASE * this.timeScale;
        this.landTime = LAND_BASE * this.timeScale;
        this.tailTime = TAIL_BASE * this.timeScale;
        this.stagger = STAGGER_BASE * this.timeScale;
        this.count = MathUtils.clamp(count, 1, StudyFx.MAX_PAGES);

        // 起点：胸口（与斋戒的命中点一致，都用 BodyFx 采样）
        this.fromX = BodyFx.centerX(owner);
        this.fromY = BodyFx.groundY(owner) + BodyFx.height(owner) * 0.56f;

        // 终点：与常驻完全相同的扇面位置
        this.toX = new float[this.count];
        this.toY = new float[this.count];
        this.tilt = new float[this.count];
        for (int i = 0; i < this.count; ++i) {
            this.toX[i] = StudyFx.pageX(i, this.count);
            this.toY[i] = StudyFx.pageY(i, this.count);
            this.tilt[i] = StudyFx.pageTilt(i, this.count);
        }

        // 控制点：中点再向右上提一点，让轨迹是明确的上拱弧
        this.ctrlX = (this.fromX + StudyFx.stackX()) * 0.5f
                + Settings.WIDTH * 0.06f;
        this.ctrlY = Math.max(this.fromY, StudyFx.stackY())
                + Settings.HEIGHT * 0.10f;

        this.life = this.formTime
                + (this.count - 1) * this.stagger
                + this.flyTime
                + this.landTime
                + this.tailTime;
        this.startingDuration = this.duration = this.life;
        // 飞行横跨屏幕下半部分，普通层会被底部 UI 与手牌盖住
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
        // 状态：带阻尼。飞渡期间拉满，落位后回落。
        float target = this.time < this.formTime + this.flyTime ? 1.0f : 0.30f;
        float speed = target > this.excite ? 15.0f : 2.4f;
        this.excite += (target - this.excite) * MathUtils.clamp(dt * speed, 0.0f, 1.0f);
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.duration <= 0.0f) {
            return;
        }
        float ex = MathUtils.clamp(this.excite, 0.0f, 1.0f);
        float punch = 0.88f + 0.26f * ex;
        float pw = StudyFx.pageW();
        float ph = StudyFx.pageH();

        // 取独立副本：Draw 内部用的是共享 Color
        Color ink = new Color(StudyFx.INK);
        Color core = new Color(StudyFx.CORE);

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        // ---- 起势：胸口一小团（只画一次，不随页数叠加）----
        if (this.time < this.formTime) {
            float q = MathUtils.clamp(this.time / Math.max(0.001f, this.formTime),
                    0.0f, 1.0f);
            float grow = Interpolation.swingOut.apply(0.0f, 1.0f, q);
            float r = Math.max(14.0f * Settings.scale, pw * 0.45f) * grow;
            Draw.glow(sb, this.fromX, this.fromY, r * 4.4f, r * 4.4f, 0.0f,
                    ink, 0.22f * q * punch);
            Draw.glow(sb, this.fromX, this.fromY, r * 2.0f, r * 2.0f, 0.0f,
                    ink, 0.58f * q * punch);
            Draw.glow(sb, this.fromX, this.fromY, r * 0.8f, r * 0.8f, 0.0f,
                    core, 0.80f * q);
        }

        for (int i = 0; i < this.count; ++i) {
            float t0 = i * this.stagger;
            // 起点（含每页错开的前置起势）
            float tForm = this.formTime + t0;
            if (this.time < tForm) {
                continue;
            }
            float local = this.time - tForm;

            if (local <= this.flyTime) {
                // ---- 飞渡：沿二次贝塞尔从胸口飞到扇面落点 ----
                float q = local / Math.max(0.001f, this.flyTime);
                // pow2Out：起手快、贴近落点时减速，读作「被放到位置上」
                float e = Interpolation.pow2Out.apply(0.0f, 1.0f, q);
                float u = 1.0f - e;
                float px = u * u * this.fromX + 2.0f * u * e * this.ctrlX
                        + e * e * this.toX[i];
                float py = u * u * this.fromY + 2.0f * u * e * this.ctrlY
                        + e * e * this.toY[i];

                // 边飞边翻页；边飞边放大到位
                float flip = MathUtils.lerp(0.10f, 0.55f, e);
                float scale = MathUtils.lerp(0.42f, 1.0f, e);
                float spin = this.tilt[i] - 70.0f * (1.0f - e);

                StudyDraw.page(sb, px, py, pw * scale, ph * scale, spin,
                        flip, 0.95f * punch);

                // 拖尾：读作「一路翻过去」
                for (int j = 1; j <= 2; ++j) {
                    float be = MathUtils.clamp(e - j * 0.09f, 0.0f, 1.0f);
                    float bu = 1.0f - be;
                    float bx = bu * bu * this.fromX + 2.0f * bu * be * this.ctrlX
                            + be * be * this.toX[i];
                    float by = bu * bu * this.fromY + 2.0f * bu * be * this.ctrlY
                            + be * be * this.toY[i];
                    float bs = scale * (1.0f - j * 0.18f);
                    Draw.glow(sb, bx, by, pw * bs * 1.15f, ph * bs * 1.15f,
                            0.0f, ink, (0.14f - j * 0.04f) * punch);
                }
                continue;
            }

            // ---- 落位定住：页在扇面位置上短暂停住，墨光闪一下 ----
            float landQ = MathUtils.clamp(
                    (local - this.flyTime) / Math.max(0.001f, this.landTime),
                    0.0f, 1.0f);
            float tailQ = MathUtils.clamp(
                    (local - this.flyTime - this.landTime)
                            / Math.max(0.001f, this.tailTime), 0.0f, 1.0f);

            if (landQ < 1.0f) {
                float px = this.toX[i];
                float py = this.toY[i];
                // 落位时定格在「刚被翻到一半」的样子，然后交给常驻
                StudyDraw.page(sb, px, py, pw, ph, this.tilt[i], 0.42f,
                        0.95f * punch);
                // 闪一下
                Draw.glow(sb, px, py, pw * 1.8f, ph * 1.8f, this.tilt[i],
                        core, 0.26f * landQ * punch);
                continue;
            }

            // ---- 余韵：扇面整体淡出（常驻已经接管）----
            if (tailQ < 1.0f) {
                float fade = FastingDraw.cosineOut(tailQ);
                StudyDraw.page(sb, this.toX[i], this.toY[i], pw, ph,
                        this.tilt[i], 0.42f, 0.95f * fade * punch);
            }
        }

        // 落位后图标上一圈淡淡的归档验证：与回合末的 settle 用同一画笔
        float settleAt = this.formTime + this.flyTime;
        if (this.time > settleAt) {
            float q = MathUtils.clamp(
                    (this.time - settleAt) / Math.max(0.001f,
                            this.landTime + this.tailTime), 0.0f, 1.0f);
            StudyDraw.settle(sb, StudyFx.deckX(), StudyFx.deckY(),
                    Math.max(26.0f * Settings.scale, pw * 0.60f), q, punch * 0.80f);
        }

        sb.setBlendFunction(src, dst);
    }

    @Override
    public void dispose() {
    }
}
