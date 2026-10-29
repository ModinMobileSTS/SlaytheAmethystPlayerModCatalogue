package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 打出「惩击」时：<b>该牌在谱上那一组三个音符一起脱谱飞出、扑向目标敌人</b>。
 * <p>
 * <h2>「颤什么就打什么」（本轮修正）</h2>
 * 上一版这里只收到一个<b>组序号</b>，然后用序号反推坐标、再自己
 * {@code Kind.random()} 掷音型。于是同一张牌被按住时颤动的那组，
 * 和飞出去的这组，位置靠猜、形状每次都不一样。
 * <p>
 * 现在改为接收 {@link HymnAuraEffect.ChordSnapshot}：由常驻表现
 * （{@link HymnAuraEffect#takeChordForStrike}）把<b>那一个声部当帧的真实坐标与音型</b>
 * 原样交出来。所以：
 * <ul>
 *   <li>飞出去的形状 = 刚才颤动的形状 = 谱上一直显示的形状</li>
 *   <li>起点 = 那一组此刻的真实位置（哪怕它正在缓动重排，也不会错位）</li>
 *   <li>那一组离场后，其余各组的格宽自动变大，靠缓动滑到新位置，
 *       打出的牌不会留下一个空洞</li>
 * </ul>
 * <p>
 * <h2>可见性</h2>
 * 走 {@code topLevelEffectsQueue}（{@link PurpleEffectRegistry#queueTop}）：
 * 起点在角色上方、终点在怪物身上，中途穿过手牌区，普通队列会被盖住。
 */
final class HymnNoteStrikeEffect extends AbstractGameEffect {
    /** 拖尾采样点。 */
    private static final int TRAIL = 10;
    /** 命中爆发持续时间。 */
    private static final float BURST = 0.30f;
    /** 这组音符的数量。 */
    private static final int NOTES = HymnAuraEffect.NOTES_PER_CHORD;

    private static final Color ICE = new Color(0.86f, 0.97f, 1.00f, 1.0f);
    private static final Color AZURE = new Color(0.42f, 0.84f, 1.00f, 1.0f);
    private static final Color WHITE = new Color(1.00f, 1.00f, 1.00f, 1.0f);

    private final float[] sx = new float[NOTES];
    private final float[] sy = new float[NOTES];
    private final float[] ctrlX = new float[NOTES];
    private final float[] ctrlY = new float[NOTES];
    private final NoteGlyph.Kind[] kinds = new NoteGlyph.Kind[NOTES];

    private final float ex;
    private final float ey;
    private final float size;
    private final float fly;

    private float time;

    private final float[] cur = new float[2];
    private final float[] prev = new float[2];

    /**
     * @param snap 该牌在谱上那一组的真实坐标与音型（来自常驻表现）
     */
    HymnNoteStrikeEffect(HymnAuraEffect.ChordSnapshot snap,
                         float targetX, float targetY) {
        float sc = Settings.scale;

        for (int k = 0; k < NOTES; ++k) {
            // 起点原样沿用谱上的位置与音型，不做任何重算
            this.sx[k] = snap.xs[k];
            this.sy[k] = snap.ys[k];
            this.kinds[k] = snap.kinds[k];
            // 轨迹轻微分开，避免飞到一起时完全重叠
            this.ctrlX[k] = (this.sx[k] + targetX) * 0.5f + (k - 1) * 14.0f * sc;
            this.ctrlY[k] = Math.max(this.sy[k], targetY)
                    + Math.abs(targetX - this.sx[k]) * 0.24f + (k - 1) * 9.0f * sc;
        }

        this.ex = targetX;
        this.ey = targetY;
        // 与谱上的音符同尺寸起飞，不突然放大
        this.size = HymnAuraEffect.NOTE_SIZE * sc;
        this.fly = Settings.FAST_MODE ? 0.40f : 0.72f;
        this.startingDuration = this.duration = this.fly + BURST;
        this.renderBehind = false;
    }

    @Override
    public void update() {
        this.time += Gdx.graphics.getDeltaTime();
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration <= 0.0f) {
            this.isDone = true;
        }
    }

    private void bezier(int k, float p, float[] out) {
        float u = 1.0f - p;
        out[0] = u * u * this.sx[k] + 2.0f * u * p * this.ctrlX[k] + p * p * this.ex;
        out[1] = u * u * this.sy[k] + 2.0f * u * p * this.ctrlY[k] + p * p * this.ey;
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.duration <= 0.0f) {
            return;
        }
        float sc = Settings.scale;
        float p = MathUtils.clamp(this.time / this.fly, 0.0f, 1.0f);
        float burst = MathUtils.clamp((this.time - this.fly) / BURST, 0.0f, 1.0f);
        boolean hit = this.time >= this.fly;

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        // ---- 脱谱余辉：起点留三个正在消散的音符（形状与谱上一致） ----
        float afterglow = MathUtils.clamp(1.0f - this.time / 0.45f, 0.0f, 1.0f);
        if (afterglow > 0.01f) {
            for (int k = 0; k < NOTES; ++k) {
                float g = this.size * (1.2f + 1.2f * (1.0f - afterglow));
                Draw.glow(sb, this.sx[k], this.sy[k], g, g, 0.0f, AZURE, afterglow * 0.55f);
                NoteGlyph.draw(sb, this.sx[k], this.sy[k],
                        this.size * (0.55f + 0.5f * (1.0f - afterglow)),
                        HymnAuraEffect.TILT_DEG, this.kinds[k], ICE,
                        afterglow * 0.45f, 0.12f);
            }
        }

        if (!hit) {
            float move = Interpolation.pow2In.apply(0.0f, 1.0f, p);

            for (int k = 0; k < NOTES; ++k) {
                // 拖尾（三枚各有一条，但收窄一些，免得糊成一片）
                for (int j = TRAIL; j >= 1; --j) {
                    float pk = Math.max(0.0f, move - 0.034f * j);
                    bezier(k, pk, this.prev);
                    float ta = (1.0f - j / (float) (TRAIL + 1)) * 0.58f;
                    if (ta <= 0.01f) {
                        continue;
                    }
                    float tw = this.size * (1.0f - j / (float) (TRAIL + 1)) * 0.75f;
                    Draw.glow(sb, this.prev[0], this.prev[1], tw * 2.0f, tw * 2.0f, 0.0f,
                            AZURE, ta);
                    Draw.glow(sb, this.prev[0], this.prev[1], tw * 1.0f, tw * 1.0f, 0.0f,
                            ICE, ta * 0.75f);
                }
                // 音符本体
                bezier(k, move, this.cur);
                float sz = this.size * (1.0f + 0.12f * p);
                Draw.glow(sb, this.cur[0], this.cur[1], sz * 1.8f, sz * 1.8f, 0.0f,
                        AZURE, 0.30f);
                Draw.glow(sb, this.cur[0], this.cur[1], sz * 1.2f, sz * 1.2f, 0.0f,
                        ICE, 0.36f);
                NoteGlyph.draw(sb, this.cur[0], this.cur[1], sz,
                        HymnAuraEffect.TILT_DEG + this.time * 110.0f,
                        this.kinds[k], WHITE, 1.0f, 0.14f);
            }
        } else {
            // ---- 命中：三音同时炸开 ----
            float q = burst;
            float fade = 1.0f - q;

            Draw.glow(sb, this.ex, this.ey,
                    this.size * (3.0f + 5.8f * q), this.size * (3.0f + 5.8f * q), 0.0f,
                    WHITE, fade * 0.92f);
            Draw.glow(sb, this.ex, this.ey,
                    this.size * (1.6f + 3.0f * q), this.size * (1.6f + 3.0f * q), 0.0f,
                    ICE, fade * 0.82f);

            Draw.ring(sb, this.ex, this.ey,
                    this.size * (1.2f + 4.8f * q), this.size * (1.2f + 4.8f * q),
                    MathUtils.lerp(9.0f, 2.0f, q) * sc, ICE, fade * 0.82f, 36);

            float blade = this.size * (1.0f + 4.0f * q);
            float bt = MathUtils.lerp(7.0f, 1.4f, q) * sc;
            for (int k = 0; k < 4; ++k) {
                float ang = k * MathUtils.PI2 / 4.0f + 0.4f;
                Draw.line(sb, this.ex, this.ey,
                        this.ex + MathUtils.cos(ang) * blade,
                        this.ey + MathUtils.sin(ang) * blade * 0.9f,
                        bt, WHITE, fade * 0.68f);
            }

            // 飞散的小音符：沿用这一组自己的三种音型，不再另掷
            for (int k = 0; k < NOTES * 3; ++k) {
                float ang = k * MathUtils.PI2 / (NOTES * 3.0f) + q * 1.8f;
                float dist = this.size * (0.6f + 3.0f * q);
                float fx = this.ex + MathUtils.cos(ang) * dist;
                float fy = this.ey + MathUtils.sin(ang) * dist * 0.85f;
                float sz = this.size * 0.52f * (1.0f - q * 0.45f);
                NoteGlyph.draw(sb, fx, fy, sz,
                        ang * MathUtils.radiansToDegrees + q * 180.0f,
                        this.kinds[k % NOTES], k % 2 == 0 ? ICE : WHITE,
                        fade * 0.82f, 0.16f);
            }
        }

        sb.setBlendFunction(src, dst);
    }

    @Override
    public void dispose() {
    }
}
