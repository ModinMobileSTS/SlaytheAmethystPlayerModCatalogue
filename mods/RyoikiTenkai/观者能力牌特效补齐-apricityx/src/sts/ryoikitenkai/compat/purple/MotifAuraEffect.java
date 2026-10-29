package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;

/**
 * 通用图案骨架的常驻实现。
 * <p>
 * 一张牌一种图案，都从卡面本身的构图取形，彼此不共用母题。
 * <p>
 * 图案全部由 {@link Draw} 的原版贴图现场画出，不再叠通用光环/卡影，
 * 所以不会和原版特效或彼此撞形。
 * <p>
 * 触发反馈走 {@link #pulse()}：只让图案自己的形态动一下（光环收紧、魔方翻转、
 * 火焰环收拢、Ω 亮起……），比再放一圈通用光圈更贴卡面。
 * <p>
 * 环境粒子（音符、金屑、水珠、余烬）由本类自己的小池子维护，
 * 每帧只在 render 里画，不往 effectList 里塞对象，因此不存在并发修改的风险。
 * <h2>本轮清理：停用母题里的星芒已全部换成粗弧光斑</h2>
 * {@code MANDALA}（涅槃）与 {@code RUNE}（研习）已经不再被任何卡牌使用
 * （见 {@link Motif} 的类注释），但它们的旧绘制方法里仍有 {@code Draw.star} 调用。
 * 现在这些调用<b>一律换成 {@code Draw.arc} 的粗弧光斑</b>，
 * 于是整个补丁的星芒调用点归零。
 * 保留这些方法而不是删掉，是为了不为了删一个形状去改两张 switch 表；
 * 万一以后想让某个母题复活，形状是对的、合规的。
 */
final class MotifAuraEffect extends PowerAuraEffect {
    private static final int MOTES = 18;

    private static final Color GOLD = new Color(1.00f, 0.84f, 0.42f, 1.0f);
    private static final Color WHITE = new Color(0.94f, 0.97f, 1.00f, 1.0f);
    private static final Color GREY = new Color(0.78f, 0.79f, 0.92f, 1.0f);
    private static final Color ORANGE = new Color(1.00f, 0.60f, 0.18f, 1.0f);
    private static final Color EMBER = new Color(0.82f, 0.24f, 0.18f, 1.0f);

    private final Motif motif;
    private final Color color;
    private final Color accent;

    /** 触发脉冲：1 → 0，图案按它做形态变化。 */
    private float pulse;
    private float spawnTimer;

    private final float[] mx = new float[MOTES];
    private final float[] my = new float[MOTES];
    private final float[] mvx = new float[MOTES];
    private final float[] mvy = new float[MOTES];
    private final float[] ml = new float[MOTES];
    private final float[] mlMax = new float[MOTES];
    private final float[] ms = new float[MOTES];
    private final int[] mk = new int[MOTES];

    MotifAuraEffect(AbstractCreature owner, String cardId, String powerId,
                    Motif motif, Color color, Color accent) {
        super(owner, cardId, powerId);
        this.motif = motif;
        this.color = new Color(color.r, color.g, color.b, 1.0f);
        this.accent = new Color(accent.r, accent.g, accent.b, 1.0f);
        this.spawnTimer = MathUtils.random(0.15f, 0.70f);
    }

    /** 能力真的触发时调用：让图案自己动一下。 */
    void pulse() {
        this.pulse = 1.0f;
    }

    // ---------------- 每帧逻辑 ----------------

    @Override
    protected void tick(float dt) {
        this.pulse = Math.max(0.0f, this.pulse - dt * 2.1f);
        updateMotes(dt);
        this.spawnTimer -= dt;
        if (this.spawnTimer <= 0.0f) {
            this.spawnTimer = spawnMote();
        }
    }

    private void updateMotes(float dt) {
        for (int i = 0; i < MOTES; ++i) {
            if (this.ml[i] <= 0.0f) {
                continue;
            }
            this.ml[i] -= dt;
            this.mx[i] += this.mvx[i] * dt;
            this.my[i] += this.mvy[i] * dt;
            if (this.ml[i] <= 0.0f) {
                this.ml[i] = 0.0f;
            }
        }
    }

    private void addMote(float x, float y, float vx, float vy, float life, float size, int kind) {
        for (int i = 0; i < MOTES; ++i) {
            if (this.ml[i] > 0.0f) {
                continue;
            }
            this.mx[i] = x;
            this.my[i] = y;
            this.mvx[i] = vx;
            this.mvy[i] = vy;
            this.ml[i] = life;
            this.mlMax[i] = life;
            this.ms[i] = size;
            this.mk[i] = kind;
            return;
        }
    }

    /** 按图案决定环境粒子怎么出生，返回下一次出生的间隔。 */
    private float spawnMote() {
        if (this.owner == null) {
            return 1.0f;
        }
        float cx = BodyFx.centerX(this.owner);
        float gy = BodyFx.groundY(this.owner);
        float hw = BodyFx.halfWidth(this.owner);
        float h = BodyFx.height(this.owner);
        float sc = Settings.scale;
        int stacks = Math.min(3, PurpleEffectRegistry.stacksOf(this.owner, this.powerId));

        switch (this.motif) {
            case SCORE:
                addMote(cx + MathUtils.random(-hw, hw) * 0.9f,
                        gy + MathUtils.random(0.12f, 0.62f) * h,
                        MathUtils.random(-12.0f, 12.0f) * sc,
                        MathUtils.random(42.0f, 78.0f) * sc,
                        MathUtils.random(1.1f, 1.7f), MathUtils.random(11.0f, 16.0f) * sc, 1);
                return MathUtils.random(0.50f, 0.90f) / stacks;
            case VORTEX: {
                float ang = MathUtils.random(0.0f, MathUtils.PI2);
                float r = hw * MathUtils.random(1.0f, 1.5f);
                float x = cx + MathUtils.cos(ang) * r;
                float cy = gy + hw * 0.24f;
                float y = cy + MathUtils.sin(ang) * r * 0.34f;
                addMote(x, y, (cx - x) * 0.6f, (cy - y) * 0.6f,
                        MathUtils.random(0.6f, 1.0f), MathUtils.random(4.0f, 7.0f) * sc, 0);
                return MathUtils.random(0.16f, 0.32f) / stacks;
            }
            case HALO:
                addMote(cx + MathUtils.random(-0.7f, 0.7f) * hw,
                        gy + MathUtils.random(0.42f, 0.62f) * h,
                        0.0f, MathUtils.random(16.0f, 30.0f) * sc,
                        MathUtils.random(0.7f, 1.2f), MathUtils.random(3.0f, 5.0f) * sc, 0);
                return MathUtils.random(0.70f, 1.30f);
            case RIBBON:
                addMote(cx + MathUtils.random(-1.2f, 1.2f) * hw,
                        gy + MathUtils.random(0.45f, 0.95f) * h,
                        0.0f, MathUtils.random(-46.0f, -22.0f) * sc,
                        MathUtils.random(0.7f, 1.1f), MathUtils.random(3.5f, 6.0f) * sc, 0);
                return MathUtils.random(0.20f, 0.40f);
            case MIRROR:
                addMote(cx + MathUtils.random(-1.1f, 1.1f) * hw,
                        gy + MathUtils.random(0.20f, 0.90f) * h,
                        0.0f, MathUtils.random(14.0f, 28.0f) * sc,
                        MathUtils.random(0.6f, 1.0f), MathUtils.random(3.0f, 5.0f) * sc, 0);
                return MathUtils.random(0.90f, 1.60f);
            case CUBE:
                addMote(cx + MathUtils.random(-1.3f, 1.3f) * hw,
                        gy + MathUtils.random(0.30f, 0.85f) * h,
                        0.0f, MathUtils.random(-12.0f, 12.0f) * sc,
                        MathUtils.random(0.5f, 0.9f), MathUtils.random(3.0f, 5.0f) * sc, 0);
                return MathUtils.random(0.50f, 0.90f);
            case MANDALA:
                addMote(cx + MathUtils.random(-0.8f, 0.8f) * hw, gy + h * 0.90f,
                        MathUtils.random(-14.0f, 14.0f) * sc,
                        MathUtils.random(20.0f, 40.0f) * sc,
                        MathUtils.random(0.7f, 1.2f), MathUtils.random(3.5f, 6.0f) * sc, 0);
                return MathUtils.random(0.35f, 0.75f) / stacks;
            case BLAST:
                addMote(cx + MathUtils.random(-1.0f, 1.0f) * hw,
                        gy + MathUtils.random(0.15f, 0.70f) * h,
                        MathUtils.random(-26.0f, 26.0f) * sc,
                        MathUtils.random(30.0f, 62.0f) * sc,
                        MathUtils.random(0.6f, 1.1f), MathUtils.random(4.0f, 7.0f) * sc, 0);
                return MathUtils.random(0.10f, 0.22f) / stacks;
            case RUNE:
                addMote(cx + MathUtils.random(-1.0f, 1.0f) * hw,
                        gy + MathUtils.random(0.10f, 0.30f) * h,
                        0.0f, MathUtils.random(26.0f, 44.0f) * sc,
                        MathUtils.random(1.0f, 1.6f), MathUtils.random(2.5f, 4.0f) * sc, 0);
                return MathUtils.random(0.45f, 0.85f);
            case PALM:
                addMote(cx + MathUtils.random(-0.9f, 0.9f) * hw,
                        gy + MathUtils.random(0.35f, 0.75f) * h,
                        MathUtils.random(-14.0f, 14.0f) * sc,
                        MathUtils.random(12.0f, 26.0f) * sc,
                        MathUtils.random(0.6f, 1.0f), MathUtils.random(3.0f, 5.0f) * sc, 0);
                return MathUtils.random(0.65f, 1.15f);
            case FLAME_EYE:
                addMote(cx + MathUtils.random(-0.5f, 0.5f) * hw,
                        gy + MathUtils.random(0.55f, 0.90f) * h,
                        MathUtils.random(-8.0f, 8.0f) * sc,
                        MathUtils.random(18.0f, 34.0f) * sc,
                        MathUtils.random(0.6f, 1.0f), MathUtils.random(3.0f, 5.0f) * sc, 0);
                return MathUtils.random(0.30f, 0.65f);
            case OMEGA:
                addMote(cx + MathUtils.random(-1.0f, 1.0f) * hw,
                        gy + MathUtils.random(0.05f, 0.35f) * h,
                        MathUtils.random(-10.0f, 10.0f) * sc,
                        MathUtils.random(34.0f, 60.0f) * sc,
                        MathUtils.random(0.9f, 1.5f), MathUtils.random(4.0f, 7.0f) * sc, 0);
                return MathUtils.random(0.16f, 0.34f) / stacks;
            default:
                return 1.0f;
        }
    }

    private Color moteColor() {
        switch (this.motif) {
            case SCORE:
            case RIBBON:
            case MIRROR:
            case CUBE:
            case PALM:
                return this.accent;
            case MANDALA:
                return GOLD;
            case OMEGA:
                return EMBER;
            case RUNE:
                return GREY;
            default:
                return this.color;
        }
    }

    // ---------------- 绘制 ----------------

    @Override
    public void render(SpriteBatch sb) {
        if (this.fade <= 0.004f || this.owner == null) {
            return;
        }
        float cx = BodyFx.centerX(this.owner);
        float gy = BodyFx.groundY(this.owner);
        float hw = BodyFx.halfWidth(this.owner);
        float h = BodyFx.height(this.owner);
        float a = this.fade;
        float sc = Settings.scale;

        sb.setBlendFunction(770, 1);
        switch (this.motif) {
            case SCORE:
                renderScore(sb, cx, gy, hw, h, a, sc);
                break;
            case VORTEX:
                renderVortex(sb, cx, gy, hw, h, a, sc);
                break;
            case HALO:
                renderHalo(sb, cx, gy, hw, h, a, sc);
                break;
            case RIBBON:
                renderRibbon(sb, cx, gy, hw, h, a, sc);
                break;
            case MIRROR:
                renderMirror(sb, cx, gy, hw, h, a, sc);
                break;
            case CUBE:
                renderCube(sb, cx, gy, hw, h, a, sc);
                break;
            case MANDALA:
                renderMandala(sb, cx, gy, hw, h, a, sc);
                break;
            case BLAST:
                renderBlast(sb, cx, gy, hw, h, a, sc);
                break;
            case RUNE:
                renderRune(sb, cx, gy, hw, h, a, sc);
                break;
            case PALM:
                renderPalm(sb, cx, gy, hw, h, a, sc);
                break;
            case FLAME_EYE:
                renderFlameEye(sb, cx, gy, hw, h, a, sc);
                break;
            case OMEGA:
                renderOmega(sb, cx, gy, hw, h, a, sc);
                break;
            default:
                break;
        }
        renderMotes(sb, a);
        sb.setBlendFunction(770, 771);
    }

    private void renderMotes(SpriteBatch sb, float a) {
        Color c = moteColor();
        for (int i = 0; i < MOTES; ++i) {
            if (this.ml[i] <= 0.0f) {
                continue;
            }
            float t = this.ml[i] / Math.max(0.001f, this.mlMax[i]);
            float alpha = a * t * 0.80f;
            if (this.mk[i] == 1) {
                NoteGlyph.draw(sb, this.mx[i], this.my[i], this.ms[i],
                        -12.0f + 22.0f * (1.0f - t), c, alpha);
            } else {
                Draw.glow(sb, this.mx[i], this.my[i], this.ms[i], this.ms[i], 0.0f, c, alpha);
            }
        }
    }

    /** 战歌：倾斜的五线谱，谱线上挂着青蓝音符。 */
    private void renderScore(SpriteBatch sb, float cx, float gy, float hw, float h,
                             float a, float sc) {
        float staffCx = cx - hw * 0.25f;
        float staffCy = gy + h * 0.55f;
        float len = hw * 3.2f;
        float rad = -17.0f * MathUtils.degreesToRadians;
        float dx = MathUtils.cos(rad);
        float dy = MathUtils.sin(rad);
        float nx = -dy;
        float ny = dx;
        for (int i = 0; i < 5; ++i) {
            float off = (i - 2) * h * 0.040f;
            float wob = MathUtils.sin(this.time * 1.2f + i * 0.8f) * 2.2f * sc;
            float x0 = staffCx - dx * len * 0.5f + nx * (off + wob);
            float y0 = staffCy - dy * len * 0.5f + ny * (off + wob);
            float x1 = staffCx + dx * len * 0.5f + nx * (off + wob);
            float y1 = staffCy + dy * len * 0.5f + ny * (off + wob);
            float bright = 0.20f + 0.16f * MathUtils.sin(this.time * 2.4f - i * 1.1f) + 0.26f * this.pulse;
            Draw.line(sb, x0, y0, x1, y1, 1.8f * sc, WHITE, a * bright);
        }
        for (int i = 0; i < 3; ++i) {
            float t = 0.30f + 0.20f * i + 0.02f * MathUtils.sin(this.time * 1.5f + i);
            float off = (i % 2 == 0 ? 1.0f : -1.0f) * h * 0.055f;
            float x = staffCx + dx * (t - 0.5f) * len + nx * off;
            float y = staffCy + dy * (t - 0.5f) * len + ny * off
                    + MathUtils.sin(this.time * 1.8f + i * 2.0f) * 4.0f * sc;
            float alpha = a * (0.40f + 0.32f * this.pulse)
                    * (0.70f + 0.30f * MathUtils.sin(this.time * 1.3f + i));
            NoteGlyph.draw(sb, x, y, 15.0f * sc, -20.0f,
                    i % 2 == 0 ? this.accent : this.color, alpha);
        }
    }

    /** 树立：脚边的橙金漩涡（卡面是丰饶角倒出的金流卷成漩涡）。 */
    private void renderVortex(SpriteBatch sb, float cx, float gy, float hw, float h,
                              float a, float sc) {
        float cy = gy + hw * 0.24f;
        float squash = 0.34f;
        float spin = this.time * (0.90f + this.pulse * 2.60f);
        float baseR = hw * (1.35f + 0.18f * this.pulse);
        Draw.glow(sb, cx, cy, hw * 2.1f, hw * 1.3f, 0.0f, this.accent,
                a * (0.10f + 0.12f * this.pulse));
        for (int arm = 0; arm < 3; ++arm) {
            float phase = arm * MathUtils.PI2 / 3.0f;
            float prevX = 0.0f;
            float prevY = 0.0f;
            for (int i = 0; i <= 30; ++i) {
                float t = i / 30.0f;
                float ang = phase + spin + t * 3.1f;
                float r = baseR * (1.02f - t * 0.94f);
                float x = cx + MathUtils.cos(ang) * r;
                float y = cy + MathUtils.sin(ang) * r * squash;
                if (i > 0) {
                    Draw.line(sb, prevX, prevY, x, y, 3.0f * sc, this.color,
                            a * (0.05f + 0.34f * t));
                    if (i % 6 == 0) {
                        Draw.line(sb, prevX, prevY, x, y, 1.3f * sc, this.accent,
                                a * (0.06f + 0.30f * t));
                    }
                }
                prevX = x;
                prevY = y;
            }
        }
        Draw.glow(sb, cx, cy, hw * 0.5f, hw * 0.5f * squash * 2.0f, 0.0f, this.accent,
                a * (0.18f + 0.20f * this.pulse));
    }

    /** 斋戒：捧在身前的黄绿光环，环内几道横纹。 */
    private void renderHalo(SpriteBatch sb, float cx, float gy, float hw, float h,
                            float a, float sc) {
        float hy = gy + h * 0.52f;
        float rr = hw * (0.74f - 0.16f * this.pulse) * (0.96f + 0.05f * breath(2.6f));
        float ry = rr * 0.94f;
        Draw.glow(sb, cx, hy, rr * 2.4f, ry * 2.4f, 0.0f, this.color,
                a * (0.10f + 0.06f * this.pulse));
        Draw.ring(sb, cx, hy, rr, ry, 4.6f * sc, this.color, a * 0.58f, 32);
        Draw.ring(sb, cx, hy, rr * 0.96f, ry * 0.96f, 1.6f * sc, this.accent, a * 0.46f, 28);
        for (int i = 0; i < 4; ++i) {
            float by = hy + (i - 1.5f) * ry * 0.32f;
            float bar = rr * (0.46f + 0.16f * this.pulse);
            Draw.line(sb, cx - bar, by, cx + bar, by, 2.2f * sc,
                    i % 2 == 0 ? this.accent : this.color,
                    a * (0.32f + 0.34f * this.pulse));
        }
    }

    /** 如水：两条青蓝水带缠身，中轴一缕金杖（卡面里水带缠的就是那根杖）。 */
    private void renderRibbon(SpriteBatch sb, float cx, float gy, float hw, float h,
                              float a, float sc) {
        int seg = 24;
        float speed = 0.85f + this.pulse * 1.8f;
        for (int rib = 0; rib < 2; ++rib) {
            float phase = rib * MathUtils.PI;
            float prevX = 0.0f;
            float prevY = 0.0f;
            for (int i = 0; i <= seg; ++i) {
                float t = i / (float) seg;
                float ang = phase + t * MathUtils.PI2 * 1.1f - this.time * speed;
                float r = hw * (1.08f + 0.14f * MathUtils.sin(t * 3.4f + rib));
                float x = cx + MathUtils.sin(ang) * r;
                float y = gy + h * (0.05f + t * 1.02f);
                if (i > 0) {
                    float body = MathUtils.sin(t * MathUtils.PI);
                    Draw.line(sb, prevX, prevY, x, y, 4.4f * sc, this.color,
                            a * (0.08f + 0.22f * body));
                    Draw.line(sb, prevX, prevY, x, y, 1.5f * sc, this.accent,
                            a * (0.14f + 0.32f * body));
                }
                prevX = x;
                prevY = y;
            }
        }
        Draw.line(sb, cx, gy + h * 0.05f, cx, gy + h * 0.94f, 2.2f * sc, GOLD, a * 0.18f);
        Draw.glow(sb, cx, gy + h * 0.97f, 15.0f * sc, 22.0f * sc, 0.0f, GOLD,
                a * (0.20f + 0.12f * this.pulse));
    }

    /** 洞悉现实：白蓝垂直光柱 + 两侧错位的镜面分身。 */
    private void renderMirror(SpriteBatch sb, float cx, float gy, float hw, float h,
                              float a, float sc) {
        float cw = hw * 0.92f;
        Draw.glow(sb, cx, gy + h * 0.54f, cw * 2.2f, h * 1.05f, 0.0f, this.accent,
                a * (0.09f + 0.10f * this.pulse));
        Draw.line(sb, cx - cw, gy + h * 0.08f, cx - cw, gy + h * 0.99f, 1.8f * sc,
                this.accent, a * 0.24f);
        Draw.line(sb, cx + cw, gy + h * 0.08f, cx + cw, gy + h * 0.99f, 1.8f * sc,
                this.accent, a * 0.24f);
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 3; ++i) {
                float flick = 0.5f + 0.5f * MathUtils.sin(this.time * 1.6f + i * 2.0f
                        + (side > 0 ? 1.1f : 0.0f));
                float ox = cx + side * hw * (1.10f + i * 0.60f);
                float oy = gy + h * (0.50f - i * 0.05f);
                float size = h * 0.30f * (1.0f - i * 0.14f);
                float alpha = a * (0.10f + 0.34f * this.pulse + 0.22f * flick);
                Draw.silhouette(sb, ox, oy, size, this.color, alpha);
                float frame = hw * 0.26f * (1.0f - i * 0.10f);
                Draw.line(sb, ox - frame, gy + h * 0.12f, ox - frame, gy + h * 0.92f,
                        1.1f * sc, this.accent, alpha * 0.70f);
                Draw.line(sb, ox + frame, gy + h * 0.12f, ox + frame, gy + h * 0.92f,
                        1.1f * sc, this.accent, alpha * 0.70f);
            }
        }
    }

    /** 心之堡垒：双层六边形护罩 + 罩外悬转的魔方。 */
    private void renderCube(SpriteBatch sb, float cx, float gy, float hw, float h,
                            float a, float sc) {
        float cy = gy + h * 0.56f;
        float shieldR = hw * (1.62f + 0.14f * this.pulse);
        for (int layer = 0; layer < 2; ++layer) {
            float r = shieldR * (layer == 0 ? 1.0f : 0.74f);
            float spin = this.time * (layer == 0 ? 0.26f : -0.42f);
            float alpha = a * (layer == 0 ? 0.30f : 0.18f);
            for (int i = 0; i < 6; ++i) {
                float a1 = spin + i * MathUtils.PI2 / 6.0f;
                float a2 = a1 + MathUtils.PI2 / 6.0f;
                Draw.line(sb,
                        cx + MathUtils.cos(a1) * r, cy + MathUtils.sin(a1) * r * 1.06f,
                        cx + MathUtils.cos(a2) * r, cy + MathUtils.sin(a2) * r * 1.06f,
                        2.4f * sc, this.accent, alpha);
            }
        }
        float cubeY = gy + h * 1.14f;
        float cr = hw * 0.40f * (1.0f + 0.08f * this.pulse);
        float spin = this.time * 0.55f;
        float[] vx = new float[6];
        float[] vy = new float[6];
        for (int i = 0; i < 6; ++i) {
            float ang = spin + (30.0f + 60.0f * i) * MathUtils.degreesToRadians;
            vx[i] = cx + MathUtils.cos(ang) * cr;
            vy[i] = cubeY + MathUtils.sin(ang) * cr * 0.92f;
        }
        for (int i = 0; i < 6; ++i) {
            int j = (i + 1) % 6;
            Draw.line(sb, vx[i], vy[i], vx[j], vy[j], 2.2f * sc, this.color, a * 0.55f);
        }
        for (int i = 0; i < 6; i += 2) {
            Draw.line(sb, cx, cubeY, vx[i], vy[i], 1.8f * sc, this.accent, a * 0.50f);
        }
        Draw.glow(sb, cx, cubeY, cr * 1.6f, cr * 1.6f, 0.0f, this.accent,
                a * (0.10f + 0.10f * this.pulse));
    }

    /**
     * 涅槃（已停用）：金边紫心的八瓣曼陀罗。
     * <p>
     * <b>不再被任何卡牌使用</b> —— 涅槃已改用 {@link NirvanaAuraEffect}
     * 的「贴身层叠六边护壁」。保留此方法只为不改 switch 表。
     * <p>
     * 本轮清理：原本每个花瓣尖端调 {@code Draw.star}（硬禁），
     * 现已换成<b>粗弧光斑</b>，形状与色相不变。
     */
    private void renderMandala(SpriteBatch sb, float cx, float gy, float hw, float h,
                               float a, float sc) {
        float cy = gy + h * 0.90f;
        float rr = hw * (1.02f + 0.20f * this.pulse + 0.05f * breath(3.2f));
        float spin = this.time * 0.42f;
        int petals = 8;
        for (int i = 0; i < petals; ++i) {
            float ang = spin + i * MathUtils.PI2 / petals;
            float x0 = cx + MathUtils.cos(ang) * rr * 0.52f;
            float y0 = cy + MathUtils.sin(ang) * rr * 0.52f;
            float mid = ang + 0.52f;
            float x1 = cx + MathUtils.cos(mid) * rr * 0.94f;
            float y1 = cy + MathUtils.sin(mid) * rr * 0.94f;
            float tip = ang + 0.24f;
            float x2 = cx + MathUtils.cos(tip) * rr * 1.12f;
            float y2 = cy + MathUtils.sin(tip) * rr * 1.12f;
            Draw.line(sb, x0, y0, x1, y1, 3.4f * sc, GOLD, a * 0.30f);
            Draw.line(sb, x1, y1, x2, y2, 2.4f * sc, GOLD, a * 0.26f);
            Draw.line(sb, x0, y0, x1, y1, 1.4f * sc, this.color, a * 0.44f);
            // 原本是 Draw.star(x2, y2, 7f * sc, ...)：改为粗弧光斑
            Draw.glow(sb, x2, y2, 9.0f * sc, 9.0f * sc, 0.0f, GOLD, a * 0.42f);
        }
        Draw.glow(sb, cx, cy, 28.0f * sc, 28.0f * sc, 0.0f, GOLD,
                a * (0.24f + 0.20f * this.pulse));
        for (int i = 0; i < 4; ++i) {
            float ang = spin * 0.7f + i * MathUtils.PI2 / 4.0f;
            float r0 = 10.0f * sc;
            float r1 = (22.0f + 16.0f * this.pulse) * sc;
            Draw.line(sb, cx + MathUtils.cos(ang) * r0, cy + MathUtils.sin(ang) * r0,
                    cx + MathUtils.cos(ang) * r1, cy + MathUtils.sin(ang) * r1,
                    1.8f * sc, this.accent, a * (0.30f + 0.40f * this.pulse));
        }
    }

    /** 突袭：橙红爆裂星芒 + 斜向残影。 */
    private void renderBlast(SpriteBatch sb, float cx, float gy, float hw, float h,
                             float a, float sc) {
        float cy = gy + h * 0.56f;
        float baseR = hw * (1.45f + 0.42f * this.pulse);
        float spin = this.time * 0.65f;
        int rays = 12;
        for (int i = 0; i < rays; ++i) {
            float ang = spin + i * MathUtils.PI2 / rays;
            boolean strong = i % 2 == 0;
            float len = baseR * (strong ? 1.0f : 0.60f)
                    * (0.86f + 0.14f * MathUtils.sin(this.time * 3.1f + i * 1.4f));
            float r0 = hw * (0.52f + 0.10f * this.pulse);
            Draw.line(sb, cx + MathUtils.cos(ang) * r0, cy + MathUtils.sin(ang) * r0,
                    cx + MathUtils.cos(ang) * len, cy + MathUtils.sin(ang) * len,
                    (strong ? 3.2f : 2.0f) * sc,
                    strong ? this.color : this.accent,
                    a * (strong ? 0.34f : 0.20f));
        }
        for (int i = 0; i < 2; ++i) {
            float flick = 0.5f + 0.5f * MathUtils.sin(this.time * 2.2f + i * MathUtils.PI);
            if (flick < 0.30f) {
                continue;
            }
            float ox = cx + (i == 0 ? -hw * 1.05f : hw * 1.05f);
            float oy = gy + h * (i == 0 ? 0.44f : 0.66f);
            Draw.silhouette(sb, ox, oy, h * 0.24f, this.accent, a * (0.16f + 0.26f * flick));
        }
    }

    /**
     * 研习（已停用）：身周浮起的灰白铭文 + 头顶三根橙刺。
     * <p>
     * <b>不再被任何卡牌使用</b> —— 研习已改用 {@link StudyFx} 的「扇形纸页」。
     * 保留此方法只为不改 switch 表。
     * <p>
     * 本轮清理：橙刺尖端原本调 {@code Draw.star}（硬禁），
     * 现已换成<b>粗弧光斑</b>。
     */
    private void renderRune(SpriteBatch sb, float cx, float gy, float hw, float h,
                            float a, float sc) {
        int slots = 6;
        float spin = this.time * 0.50f + this.pulse * 1.4f;
        for (int i = 0; i < slots; ++i) {
            float t = (this.time * 0.11f + i / (float) slots) % 1.0f;
            float ang = spin + i * MathUtils.PI2 / slots;
            float ox = cx + MathUtils.cos(ang) * hw * 1.45f;
            float oy = gy + h * (0.10f + t * 0.82f);
            float alpha = a * 0.32f * MathUtils.sin(t * MathUtils.PI);
            float size = 17.0f * sc;
            float[] seg = Draw.RUNE[i % Draw.RUNE.length];
            for (int s = 0; s + 3 < seg.length; s += 4) {
                Draw.line(sb, ox + seg[s] * size, oy + seg[s + 1] * size,
                        ox + seg[s + 2] * size, oy + seg[s + 3] * size,
                        1.7f * sc, GREY, alpha);
            }
        }
        float topY = gy + h * 1.02f;
        for (int i = -1; i <= 1; ++i) {
            float flick = 0.65f + 0.35f * MathUtils.sin(this.time * 3.0f + i * 1.7f);
            float ang = (90.0f + i * 26.0f) * MathUtils.degreesToRadians;
            float len = (h * 0.20f + h * 0.10f * this.pulse) * flick;
            float x1 = cx + i * hw * 0.16f;
            Draw.line(sb, x1, topY, x1 + MathUtils.cos(ang) * len,
                    topY + MathUtils.sin(ang) * len, 2.6f * sc, ORANGE, a * 0.52f * flick);
            // 原本是 Draw.star(...)：改为粗弧光斑
            Draw.glow(sb, x1 + MathUtils.cos(ang) * len,
                    topY + MathUtils.sin(ang) * len, 8.0f * sc, 8.0f * sc,
                    0.0f, ORANGE, a * 0.38f * flick);
        }
    }

    /** 与手交谈：敌人身上的青绿掌印 + 锯齿电光。 */
    private void renderPalm(SpriteBatch sb, float cx, float gy, float hw, float h,
                            float a, float sc) {
        float cy = gy + h * 0.55f;
        float s = hw * 0.92f;
        float alpha = a * (0.30f + 0.38f * this.pulse) * (0.86f + 0.14f * breath(2.4f));
        Draw.glow(sb, cx, cy, s * 1.15f, s * 1.35f, 0.0f, this.color, alpha * 0.30f);
        for (int i = 0; i < 4; ++i) {
            float fx = cx + (i - 1.5f) * s * 0.26f;
            Draw.line(sb, fx, cy + s * 0.24f, fx + (i - 1.5f) * s * 0.08f, cy + s * 0.94f,
                    4.0f * sc, this.color, alpha);
        }
        Draw.line(sb, cx - s * 0.40f, cy + s * 0.08f, cx - s * 0.96f, cy + s * 0.40f,
                4.4f * sc, this.color, alpha * 0.90f);
        Draw.ring(sb, cx, cy, s * 0.50f, s * 0.58f, 2.0f * sc, this.accent, alpha * 0.55f, 22);
        for (int i = 0; i < 6; ++i) {
            float flick = 0.5f + 0.5f * MathUtils.sin(this.time * 4.2f + i * 1.3f);
            if (flick < 0.42f) {
                continue;
            }
            float ang = this.time * 0.7f + i * MathUtils.PI2 / 6.0f;
            float r = s * 1.30f;
            float x1 = cx + MathUtils.cos(ang) * r;
            float y1 = cy + MathUtils.sin(ang) * r * 0.92f;
            float x2 = cx + MathUtils.cos(ang + 0.26f) * r * 1.34f;
            float y2 = cy + MathUtils.sin(ang + 0.26f) * r * 1.20f;
            float x3 = cx + MathUtils.cos(ang) * r * 1.62f;
            float y3 = cy + MathUtils.sin(ang) * r * 1.46f;
            Draw.line(sb, x1, y1, x2, y2, 2.2f * sc, this.accent, a * 0.46f * flick);
            Draw.line(sb, x2, y2, x3, y3, 2.0f * sc, this.accent, a * 0.34f * flick);
        }
    }

    /** 先见（已停用）：头顶紫红火焰环 + 中心金眼。 */
    private void renderFlameEye(SpriteBatch sb, float cx, float gy, float hw, float h,
                                float a, float sc) {
        float cy = gy + h * 0.97f;
        float rr = hw * (1.00f - 0.14f * this.pulse);
        float spin = this.time * 0.50f;
        int tongues = 10;
        for (int i = 0; i < tongues; ++i) {
            float ang = spin + i * MathUtils.PI2 / tongues;
            float flick = 0.55f + 0.45f * MathUtils.sin(this.time * 3.4f + i * 1.6f);
            float r0 = rr * 0.52f;
            float r1 = rr * (0.90f + 0.32f * flick);
            float r2 = rr * (1.15f + 0.42f * flick);
            float wob = 0.18f * MathUtils.sin(this.time * 2.2f + i);
            float a2 = ang + wob;
            float a3 = ang - wob * 0.5f;
            Draw.line(sb, cx + MathUtils.cos(ang) * r0, cy + MathUtils.sin(ang) * r0,
                    cx + MathUtils.cos(a2) * r1, cy + MathUtils.sin(a2) * r1,
                    3.0f * sc, this.color, a * 0.30f);
            Draw.line(sb, cx + MathUtils.cos(a2) * r1, cy + MathUtils.sin(a2) * r1,
                    cx + MathUtils.cos(a3) * r2, cy + MathUtils.sin(a3) * r2,
                    2.0f * sc, this.color, a * 0.22f);
            Draw.line(sb, cx + MathUtils.cos(ang) * r0, cy + MathUtils.sin(ang) * r0,
                    cx + MathUtils.cos(a2) * r1, cy + MathUtils.sin(a2) * r1,
                    1.2f * sc, this.accent, a * 0.18f);
        }
        float eyeW = hw * 0.46f;
        float eyeH = hw * 0.23f;
        Draw.ring(sb, cx, cy, eyeW, eyeH, 2.2f * sc, GOLD, a * 0.46f, 20);
        Draw.line(sb, cx, cy - eyeH * 0.80f, cx, cy + eyeH * 0.80f, 3.0f * sc, GOLD,
                a * (0.44f + 0.36f * this.pulse));
        Draw.glow(sb, cx, cy, eyeW * 1.1f, eyeH * 1.4f, 0.0f, this.accent, a * 0.20f);
    }

    /** 欧米伽：头顶金色 Ω 徽记。 */
    private void renderOmega(SpriteBatch sb, float cx, float gy, float hw, float h,
                             float a, float sc) {
        float cy = gy + h * 1.10f;
        float rr = hw * (0.50f + 0.08f * this.pulse);
        float thick = 4.2f * sc;
        float alpha = a * (0.52f + 0.34f * this.pulse);
        Draw.arc(sb, cx, cy, rr, rr, 315.0f, 270.0f, thick, this.color, alpha, 26);
        float ex = rr * 0.707f;
        float ey = rr * 0.707f;
        float legBottom = cy - rr * 0.98f;
        Draw.line(sb, cx + ex, cy - ey, cx + ex * 1.22f, legBottom, thick, this.color, alpha);
        Draw.line(sb, cx - ex, cy - ey, cx - ex * 1.22f, legBottom, thick, this.color, alpha);
        Draw.line(sb, cx + ex * 0.86f, legBottom, cx + ex * 1.72f, legBottom, thick * 0.9f,
                this.color, alpha * 0.90f);
        Draw.line(sb, cx - ex * 1.72f, legBottom, cx - ex * 0.86f, legBottom, thick * 0.9f,
                this.color, alpha * 0.90f);
        Draw.glow(sb, cx, cy, rr * 2.6f, rr * 2.6f, 0.0f, this.accent,
                a * (0.10f + 0.12f * this.pulse));
    }
}
