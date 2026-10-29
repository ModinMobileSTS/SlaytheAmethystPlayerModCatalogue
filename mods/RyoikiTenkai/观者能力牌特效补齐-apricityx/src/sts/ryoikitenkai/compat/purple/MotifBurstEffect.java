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
 * 通用图案骨架的一次性爆发。
 * <p>
 * 按母题画一次「形状向外炸开」的演出，配合 {@link MotifAuraEffect#pulse()}
 * 让图案在触发瞬间有形态变化。
 * <p>
 * <h2>本轮清理：停用母题里的星芒已换成粗弧光斑</h2>
 * {@code MANDALA}（涅槃）与 {@code RUNE}（研习）已不再被任何卡牌使用
 * （见 {@link Motif} 类注释），但它们的旧绘制分支里仍有 {@code Draw.star}。
 * 现在这些调用<b>一律换成 {@code Draw.glow} 圆形光斑</b>，
 * 于是整个补丁的星芒调用点归零。
 */
final class MotifBurstEffect extends AbstractGameEffect {
    /** 各母题爆发一次的时长（秒，标准模式）。 */
    private static final float LIFE_BASE = 1.05f;

    private final Motif motif;
    private final Color color;
    private final Color accent;
    private final float cx;
    private final float cy;
    private final float hw;
    private final float h;
    /** 强度系数：由触发方给出，控制亮度与张开幅度。 */
    private final float power;
    private final float life;

    private float time;

    MotifBurstEffect(AbstractCreature owner, Motif motif, Color color, Color accent,
                     float power) {
        this.motif = motif;
        this.color = new Color(color.r, color.g, color.b, 1.0f);
        this.accent = new Color(accent.r, accent.g, accent.b, 1.0f);
        this.cx = BodyFx.centerX(owner);
        this.cy = BodyFx.groundY(owner) + BodyFx.height(owner) * 0.56f;
        this.hw = BodyFx.halfWidth(owner);
        this.h = BodyFx.height(owner);
        this.power = MathUtils.clamp(power, 0.2f, 1.5f);
        this.life = LIFE_BASE * (Settings.FAST_MODE ? 0.72f : 1.0f);
        this.startingDuration = this.duration = this.life;
        this.renderBehind = false;
    }

    @Override
    public void update() {
        float dt = Gdx.graphics.getDeltaTime();
        this.time += dt;
        this.duration -= dt;
        if (this.duration <= 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.duration <= 0.0f) {
            return;
        }
        float p = MathUtils.clamp(1.0f - this.duration / Math.max(0.001f, this.life),
                0.0f, 1.0f);
        // 亮度包络：起势快、末端余弦化掉（不硬切）
        float env = p < 0.18f ? p / 0.18f
                : FastingDraw.cosineOut((p - 0.18f) / 0.82f);
        // 展开用 pow2Out：起手快、末端柔
        float spread = Interpolation.pow2Out.apply(0.0f, 1.0f, p);
        float peak = env * this.power;
        if (peak <= 0.006f) {
            return;
        }

        float sc = Settings.scale;
        float cx = this.cx;
        float cy = this.cy;

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        switch (this.motif) {
            case OMEGA:
                renderOmegaBurst(sb, cx, cy, sc, p, spread, env, peak);
                break;
            case MANDALA:
                renderMandalaBurst(sb, cx, cy, sc, p, spread, env, peak);
                break;
            case RUNE:
                renderRuneBurst(sb, cx, cy, sc, p, spread, env, peak);
                break;
            case PALM:
            case MIRROR:
            default:
                renderRingBurst(sb, cx, cy, sc, p, spread, env, peak);
                break;
        }

        sb.setBlendFunction(src, dst);
    }

    /** 通用：一圈粗弧向外扩 + 中心一团光。 */
    private void renderRingBurst(SpriteBatch sb, float cx, float cy, float sc,
                                 float p, float spread, float env, float peak) {
        float r = this.hw * (0.50f + 2.10f * spread);
        float thick = Math.max(6.0f * sc, this.hw * 0.16f) * (1.0f - 0.45f * p);
        Draw.arc(sb, cx, cy, r, r * 0.70f, 0.0f, 360.0f, thick,
                this.color, peak * 0.82f, 34);
        Draw.arc(sb, cx, cy, r * 0.94f, r * 0.66f, 0.0f, 360.0f,
                Math.max(4.5f * sc, thick * 0.42f), this.accent, peak * 0.58f, 30);
        Draw.glow(sb, cx, cy, r * 1.8f, r * 1.8f, 0.0f, this.color, peak * 0.20f);
    }

    /** 欧米伽：金色 Ω 向外炸开 + 向外散的红烟。 */
    private void renderOmegaBurst(SpriteBatch sb, float cx, float cy, float sc,
                                  float p, float spread, float env, float peak) {
        float rr = this.hw * (0.50f + 1.30f * spread);
        float thick = Math.max(5.0f * sc, this.hw * 0.11f) * (1.0f - 0.30f * p);
        Draw.arc(sb, cx, cy, rr, rr, 315.0f, 270.0f, thick, this.color, peak * 0.85f, 26);
        float ex = rr * 0.707f;
        float ey = rr * 0.707f;
        float leg = cy - rr * 0.98f;
        Draw.line(sb, cx + ex, cy - ey, cx + ex * 1.22f, leg, thick, this.color, peak * 0.85f);
        Draw.line(sb, cx - ex, cy - ey, cx - ex * 1.22f, leg, thick, this.color, peak * 0.85f);
        Draw.glow(sb, cx, cy, rr * 2.0f, rr * 2.0f, 0.0f, this.accent, peak * 0.22f);
        // 向外散的红烟余烬（圆形光点，不用线段）
        for (int i = 0; i < 8; ++i) {
            float ang = i * MathUtils.PI2 / 8.0f + 0.3f;
            float r = this.hw * (0.35f + 1.05f * spread);
            float px = cx + MathUtils.cos(ang) * r;
            float py = cy + MathUtils.sin(ang) * r * 0.9f;
            float s = Math.max(4.0f * sc, this.hw * 0.10f) * (1.0f - 0.5f * p);
            Draw.glow(sb, px, py, s * 2.4f, s * 2.4f, 0.0f, this.accent, peak * 0.42f);
        }
    }

    /**
     * 涅槃（已停用）的爆发：金边曼陀罗向外炸开。
     * <p>
     * <b>不再被任何卡牌使用</b>（涅槃已改用 {@link NirvanaShellEffect}）。
     * 本轮清理：花瓣尖端的 {@code Draw.star} 已换成<b>圆形光斑</b>。
     */
    private void renderMandalaBurst(SpriteBatch sb, float cx, float cy, float sc,
                                    float p, float spread, float env, float peak) {
        float rr = this.hw * (0.60f + 1.20f * spread);
        int petals = 8;
        for (int i = 0; i < petals; ++i) {
            float ang = i * MathUtils.PI2 / petals + p * 0.8f;
            float x0 = cx + MathUtils.cos(ang) * rr * 0.5f;
            float y0 = cy + MathUtils.sin(ang) * rr * 0.5f;
            float x1 = cx + MathUtils.cos(ang + 0.5f) * rr * 0.95f;
            float y1 = cy + MathUtils.sin(ang + 0.5f) * rr * 0.95f;
            float x2 = cx + MathUtils.cos(ang + 0.22f) * rr * 1.15f;
            float y2 = cy + MathUtils.sin(ang + 0.22f) * rr * 1.15f;
            Draw.line(sb, x0, y0, x1, y1, Math.max(4.5f * sc, 3.2f * sc),
                    GOLD(), peak * 0.55f);
            Draw.line(sb, x1, y1, x2, y2, Math.max(4.5f * sc, 2.2f * sc),
                    GOLD(), peak * 0.45f);
            // 原本是 Draw.star(x2, y2, 7f * sc, ...)：改为圆形光斑
            Draw.glow(sb, x2, y2, 9.0f * sc, 9.0f * sc, 0.0f, GOLD(), peak * 0.52f);
        }
        for (int i = 0; i < 8; ++i) {
            float ang = i * MathUtils.PI2 / 8.0f + 0.22f;
            float rin = 12.0f * sc;
            float rout = (26.0f + 52.0f * p) * sc;
            Draw.line(sb, cx + MathUtils.cos(ang) * rin, cy + MathUtils.sin(ang) * rin,
                    cx + MathUtils.cos(ang) * rout, cy + MathUtils.sin(ang) * rout,
                    Math.max(4.5f * sc, 1.8f * sc), this.accent, peak * 0.55f);
        }
    }

    /**
     * 研习（已停用）的爆发：灰白铭文向外散 + 头顶橙刺。
     * <p>
     * <b>不再被任何卡牌使用</b>（研习已改用 {@link StudyAssignEffect} / {@link StudyDropEffect}）。
     * 本轮清理：橙刺尖端的 {@code Draw.star} 已换成<b>圆形光斑</b>。
     */
    private void renderRuneBurst(SpriteBatch sb, float cx, float cy, float sc,
                                 float p, float spread, float env, float peak) {
        int slots = 6;
        float size = 17.0f * sc;
        for (int i = 0; i < slots; ++i) {
            float ang = i * MathUtils.PI2 / slots + p * 0.5f;
            float r = this.hw * (1.1f + 0.9f * spread);
            float ox = cx + MathUtils.cos(ang) * r;
            float oy = cy + MathUtils.sin(ang) * r * 0.8f;
            float[] seg = Draw.RUNE[i % Draw.RUNE.length];
            for (int s = 0; s + 3 < seg.length; s += 4) {
                Draw.line(sb, ox + seg[s] * size, oy + seg[s + 1] * size,
                        ox + seg[s + 2] * size, oy + seg[s + 3] * size,
                        Math.max(4.5f * sc, 1.7f * sc), this.color, peak * 0.42f);
            }
        }
        float topY = cy + this.h * 0.36f;
        for (int i = -1; i <= 1; ++i) {
            float ang = (90.0f + i * 26.0f) * MathUtils.degreesToRadians;
            float len = this.h * 0.20f * (0.6f + 0.8f * spread);
            float x1 = cx + i * this.hw * 0.16f;
            Draw.line(sb, x1, topY, x1 + MathUtils.cos(ang) * len,
                    topY + MathUtils.sin(ang) * len, Math.max(4.5f * sc, 2.6f * sc),
                    ORANGE(), peak * 0.52f);
            // 原本是 Draw.star(...)：改为圆形光斑
            Draw.glow(sb, x1 + MathUtils.cos(ang) * len,
                    topY + MathUtils.sin(ang) * len, 8.0f * sc, 8.0f * sc,
                    0.0f, ORANGE(), peak * 0.40f);
        }
    }

    // ---------------- 共享颜色（复用一个对象，避免每帧 new） ----------------

    private static final Color GOLD_CACHE = new Color(1.00f, 0.84f, 0.42f, 1.0f);
    private static final Color ORANGE_CACHE = new Color(1.00f, 0.60f, 0.18f, 1.0f);

    private static Color GOLD() {
        return GOLD_CACHE;
    }

    private static Color ORANGE() {
        return ORANGE_CACHE;
    }

    @Override
    public void dispose() {
    }
}
