package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;

/**
 * 「战歌」的常驻表现：<b>一面倾斜 15° 的五线谱，每张惩击在谱上对应一组三个音符</b>。
 * <p>
 * <h2>一牌一声部</h2>
 * 每张惩击第一次出现在手上时领到一个 {@link Chord} 声部，此后：
 * <ul>
 *   <li><b>颤动</b>：按住哪张牌，就只有它那一声部颤（用 {@code ==} 比牌对象）</li>
 *   <li><b>飞出</b>：打出哪张牌，就把<b>它那一个声部当帧的真实坐标与音型</b>
 *       （{@link #takeChordForStrike}）交给飞行特效 —— 颤什么就打什么</li>
 *   <li><b>重排</b>：其余声部的目标格位按新手牌数重算，
 *       每帧用 {@code MOVE_LERP} 缓动追赶，于是自动「带动画地排开」</li>
 * </ul>
 * <h2>本轮修正：全部放慢（用户反馈「太快、消失不丝滑」）</h2>
 * <ul>
 *   <li>音符出现 0.34s → <b>0.62s</b></li>
 *   <li>音符消失 0.26s → <b>0.72s</b>，并把淡出曲线从线性改为 {@code pow2In}</li>
 *   <li>位移缓动 11 → <b>8</b>（更黏、更慢）</li>
 *   <li>消失时的上浮与缩小幅度都加大，让「散去」更有过程感</li>
 * </ul>
 * 音符消失原本是 0.26s 线性掉到 0 —— 眼睛基本抓不到，这就是「消失不丝滑」的来源。
 * <h2>怎么可靠判断「正在按住某张牌」</h2>
 * 三个互补判据并联（{@code InputHelper.isMouseDown} /
 * {@code isDraggingCard} / {@code inSingleTargetMode}），
 * <b>但前提是 {@code hoveredCard} 就是惩击</b>，所以纯悬停不触发。
 * <h2>不绑定骨骼与性能</h2>
 * 坐标只来自 {@link BodyFx}；声部数组预分配、每帧不 new；混合函数 get/set 还原。
 */
final class HymnAuraEffect extends PowerAuraEffect {
    /** 固定的五线谱线数。 */
    static final int STAFF_LINES = 5;
    /** 小节数。 */
    private static final int MEASURES = 4;
    /** 每张牌对应的音符数。 */
    static final int NOTES_PER_CHORD = 3;
    /** 谱上最多同时显示几张牌的音符。 */
    static final int MAX_CHORDS = 4;
    /** 音符槽位总数。 */
    static final int MAX_NOTES = MAX_CHORDS * NOTES_PER_CHORD;
    /** 战歌锻出的临时牌。 */
    static final String SMITE_ID = "Smite";

    /** 谱面中心相对角色脚底的高度倍数。 */
    static final float STAFF_LIFT = 1.30f;
    /** 相邻两线的间距（像素，未乘 scale）。 */
    static final float LINE_GAP = 40.0f;
    /** 音符基准尺寸（像素，未乘 scale）。 */
    static final float NOTE_SIZE = 68.0f;
    /** 谱面倾角：15°。 */
    static final float TILT_DEG = 15.0f;
    static final float TILT_RAD_COS = MathUtils.cos(TILT_DEG * MathUtils.degreesToRadians);
    static final float TILT_RAD_SIN = MathUtils.sin(TILT_DEG * MathUtils.degreesToRadians);

    /** 谱上音符可用的总跨度（谱线是 −1..1，两端留边）。 */
    private static final float SPREAD = 1.86f;
    /** 组内相邻两音的最大横向间距（u 单位）。 */
    private static final float NOTE_GAP_U = 0.20f;

    /** 音高表。每声部随机取一个起点，组内三音沿表连续取三格。 */
    private static final float[] PITCH = {
            2.0f, 1.0f, 2.5f, 1.5f, 2.0f, 0.5f, 2.5f, 1.0f, 1.5f, 2.0f, 1.0f, 2.5f
    };

    /** 唤起缓动：上升快、回落慢。 */
    private static final float EXCITE_RISE = 5.5f;
    private static final float EXCITE_FALL = 2.4f;

    /** 出现 / 消失动画时长。都比原来慢一倍以上，让过程看得见。 */
    private static final float APPEAR_TIME = 0.62f;
    private static final float VANISH_TIME = 0.72f;
    /** 位置缓动速率：小一点更黏、更慢。 */
    private static final float MOVE_LERP = 8.0f;

    private static final com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion LINE =
            ImageMaster.THICK_3D_LINE;

    private static final Color AZURE = new Color(0.42f, 0.84f, 1.00f, 1.0f);
    private static final Color ICE = new Color(0.86f, 0.97f, 1.00f, 1.0f);
    private static final Color WHITE = new Color(1.00f, 1.00f, 1.00f, 1.0f);
    private static final Color DARK = new Color(0.04f, 0.08f, 0.16f, 1.0f);

    /** 一组音符（= 一张惩击的声部）。位置、音型、动画状态都跟着这张牌走。 */
    private static final class Chord {
        AbstractCard card;
        final NoteGlyph.Kind[] kinds = new NoteGlyph.Kind[NOTES_PER_CHORD];
        final float[] x = new float[NOTES_PER_CHORD];
        final float[] y = new float[NOTES_PER_CHORD];
        /** 当前在谱上的排序，决定目标格位。 */
        int order;
        /** 音高起点，随机一次后固定。 */
        int pitch;
        float appear;
        float vanish;
        /** 该组自身的颤动强度 0~1。 */
        float tremor;
        boolean anchored;
        boolean active;

        void reset(AbstractCard card) {
            this.card = card;
            this.order = 0;
            this.pitch = MathUtils.random(PITCH.length - 1);
            this.appear = 0.0f;
            this.vanish = 0.0f;
            this.tremor = 0.0f;
            this.anchored = false;
            this.active = true;
            for (int k = 0; k < NOTES_PER_CHORD; ++k) {
                this.kinds[k] = NoteGlyph.Kind.random();
            }
        }

        void clear() {
            this.card = null;
            this.appear = 0.0f;
            this.vanish = 0.0f;
            this.tremor = 0.0f;
            this.anchored = false;
            this.active = false;
        }
    }

    /** 打出一张惩击时交给飞行特效的「这一组此刻长什么样」。 */
    static final class ChordSnapshot {
        final float[] xs = new float[NOTES_PER_CHORD];
        final float[] ys = new float[NOTES_PER_CHORD];
        final NoteGlyph.Kind[] kinds = new NoteGlyph.Kind[NOTES_PER_CHORD];
    }

    private final Chord[] chords = new Chord[MAX_CHORDS];
    private final AbstractCard[] handOrder = new AbstractCard[MAX_CHORDS];
    private float excite;

    private final float[] tmpA = new float[2];
    private final float[] tmpB = new float[2];

    HymnAuraEffect(AbstractCreature owner, String cardId, String powerId) {
        super(owner, cardId, powerId);
        this.renderBehind = false;
        for (int i = 0; i < MAX_CHORDS; ++i) {
            this.chords[i] = new Chord();
        }
    }

    @Override
    void pulse() {
        // 刻意为空：谱面读数由手牌决定
    }

    // ---------------- 布局几何 ----------------

    static float cellU(int count) {
        int n = MathUtils.clamp(count, 1, MAX_CHORDS);
        return SPREAD / n;
    }

    static float centerU(int order, int count) {
        int n = MathUtils.clamp(count, 1, MAX_CHORDS);
        int o = MathUtils.clamp(order, 0, n - 1);
        return -SPREAD * 0.5f + cellU(n) * (o + 0.5f);
    }

    static float gapU(int count) {
        return Math.min(cellU(count) / NOTES_PER_CHORD, NOTE_GAP_U);
    }

    static float noteU(int order, int count, int k) {
        return centerU(order, count) + (k - 1) * gapU(count);
    }

    static float pitchV(int index) {
        int i = ((index % PITCH.length) + PITCH.length) % PITCH.length;
        return PITCH[i] - (STAFF_LINES - 1) * 0.5f;
    }

    /** 兜底布局：没有活着的常驻实例时按几何推算。 */
    static ChordSnapshot layoutSnapshot(AbstractCreature owner, int order, int count) {
        ChordSnapshot snap = new ChordSnapshot();
        float[] t = new float[2];
        for (int k = 0; k < NOTES_PER_CHORD; ++k) {
            toWorld(owner, noteU(order, count, k),
                    pitchV(order * NOTES_PER_CHORD + k), t);
            snap.xs[k] = t[0];
            snap.ys[k] = t[1];
            snap.kinds[k] = NoteGlyph.Kind.byIndex(order * NOTES_PER_CHORD + k);
        }
        return snap;
    }

    // ---------------- 状态读取 ----------------

    static int smitesInHand() {
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null || p.hand == null || p.hand.group == null) {
            return 0;
        }
        int n = 0;
        for (int i = 0; i < p.hand.group.size(); ++i) {
            AbstractCard c = p.hand.group.get(i);
            if (c != null && SMITE_ID.equals(c.cardID)) {
                ++n;
            }
        }
        return Math.min(n, MAX_CHORDS);
    }

    /**
     * 此刻被<b>按住/拖动</b>的那张惩击；纯悬停、或按的是别的牌返回 null。
     */
    static AbstractCard heldSmiteCard() {
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null || p.hoveredCard == null) {
            return null;
        }
        if (!SMITE_ID.equals(p.hoveredCard.cardID)) {
            return null;
        }
        if (InputHelper.isMouseDown || p.isDraggingCard || p.inSingleTargetMode) {
            return p.hoveredCard;
        }
        return null;
    }

    /**
     * 把某张惩击对应的声部<b>取走</b>交给飞行动画，并让它立刻离谱。
     * 这就是「颤什么就打什么」：返回的坐标与音型就是它此刻在谱上的样子。
     */
    ChordSnapshot takeChordForStrike(AbstractCard card) {
        if (card == null) {
            return null;
        }
        for (int i = 0; i < MAX_CHORDS; ++i) {
            Chord ch = this.chords[i];
            if (!ch.active || ch.card != card) {
                continue;
            }
            ChordSnapshot snap = new ChordSnapshot();
            for (int k = 0; k < NOTES_PER_CHORD; ++k) {
                snap.xs[k] = ch.x[k];
                snap.ys[k] = ch.y[k];
                snap.kinds[k] = ch.kinds[k];
            }
            ch.clear();
            return snap;
        }
        return null;
    }

    private Chord find(AbstractCard card) {
        for (int i = 0; i < MAX_CHORDS; ++i) {
            Chord ch = this.chords[i];
            if (ch.active && ch.card == card) {
                return ch;
            }
        }
        return null;
    }

    private Chord freeChord() {
        for (int i = 0; i < MAX_CHORDS; ++i) {
            if (!this.chords[i].active) {
                return this.chords[i];
            }
        }
        return null;
    }

    private boolean inHandOrder(AbstractCard card, int n) {
        for (int i = 0; i < n; ++i) {
            if (this.handOrder[i] == card) {
                return true;
            }
        }
        return false;
    }

    // ---------------- 每帧 ----------------

    @Override
    protected void tick(float dt) {
        AbstractPlayer p = AbstractDungeon.player;

        // ---- 1. 手牌里的惩击，按手牌顺序 ----
        int n = 0;
        if (p != null && p.hand != null && p.hand.group != null) {
            for (int i = 0; i < p.hand.group.size() && n < MAX_CHORDS; ++i) {
                AbstractCard c = p.hand.group.get(i);
                if (c != null && SMITE_ID.equals(c.cardID)) {
                    this.handOrder[n++] = c;
                }
            }
        }
        for (int i = n; i < MAX_CHORDS; ++i) {
            this.handOrder[i] = null;
        }

        // ---- 2. 已经不在手牌里的声部开始退场 ----
        for (int i = 0; i < MAX_CHORDS; ++i) {
            Chord ch = this.chords[i];
            if (ch.active && ch.vanish <= 0.0f && !inHandOrder(ch.card, n)) {
                ch.vanish = 0.0001f;
            }
        }

        // ---- 3. 手上的每张惩击都要有一个声部 ----
        for (int i = 0; i < n; ++i) {
            AbstractCard card = this.handOrder[i];
            if (card == null) {
                continue;
            }
            Chord ch = find(card);
            if (ch == null) {
                ch = freeChord();
                if (ch == null) {
                    continue;
                }
                ch.reset(card);
            } else if (ch.vanish > 0.0f) {
                ch.vanish = 0.0f;
            }
            ch.order = i;
        }

        // ---- 4. 颤动：只给「被按住的那张牌」的声部 ----
        AbstractCard held = heldSmiteCard();
        float cell = cellU(n);
        float gap = gapU(n);
        boolean anyHeld = false;

        for (int i = 0; i < MAX_CHORDS; ++i) {
            Chord ch = this.chords[i];
            if (!ch.active) {
                continue;
            }
            boolean isHeld = ch.vanish <= 0.0f && held != null && ch.card == held;
            if (isHeld) {
                anyHeld = true;
                ch.tremor = Math.min(1.0f, ch.tremor + dt * 5.5f);
            } else {
                ch.tremor = Math.max(0.0f, ch.tremor - dt * 3.2f);
            }

            if (ch.appear < 1.0f) {
                ch.appear = Math.min(1.0f, ch.appear + dt / APPEAR_TIME);
            }
            if (ch.vanish > 0.0f) {
                ch.vanish = Math.min(1.0f, ch.vanish + dt / VANISH_TIME);
                if (ch.vanish >= 1.0f) {
                    ch.clear();
                }
                // 退场过程中保持原位，避免一边缩小一边乱滑
                continue;
            }

            // ---- 5. 目标格位：组数一变格宽就变，缓动让它自己滑过去 ----
            int order = MathUtils.clamp(ch.order, 0, Math.max(0, n - 1));
            float center = -SPREAD * 0.5f + cell * (order + 0.5f);
            for (int k = 0; k < NOTES_PER_CHORD; ++k) {
                float u = center + (k - 1) * gap;
                toWorld(this.owner, u, pitchV(ch.pitch + k), this.tmpA);
                if (!ch.anchored) {
                    ch.x[k] = this.tmpA[0];
                    ch.y[k] = this.tmpA[1];
                } else {
                    float m = MathUtils.clamp(dt * MOVE_LERP, 0.0f, 1.0f);
                    ch.x[k] += (this.tmpA[0] - ch.x[k]) * m;
                    ch.y[k] += (this.tmpA[1] - ch.y[k]) * m;
                }
            }
            ch.anchored = true;
        }

        // ---- 6. 谱面整体提亮 ----
        float target = anyHeld ? 1.0f : 0.0f;
        float speed = target > this.excite ? EXCITE_RISE : EXCITE_FALL;
        this.excite += (target - this.excite) * MathUtils.clamp(dt * speed, 0.0f, 1.0f);
    }

    // ---------------- 几何 ----------------

    static float staffCenterY(AbstractCreature owner) {
        return BodyFx.groundY(owner) + BodyFx.height(owner) * STAFF_LIFT;
    }

    static float staffHalfLen(AbstractCreature owner) {
        return Math.max(BodyFx.halfWidth(owner) * 2.4f, Settings.WIDTH * 0.24f);
    }

    private static void toWorld(AbstractCreature owner, float u, float v, float[] out) {
        toWorldAt(BodyFx.centerX(owner), staffCenterY(owner), staffHalfLen(owner), u, v, out);
    }

    private static void toWorldAt(float cx, float cy, float halfLen, float u, float v,
                                  float[] out) {
        float along = u * halfLen;
        float across = v * LINE_GAP * Settings.scale;
        out[0] = cx + along * TILT_RAD_COS - across * TILT_RAD_SIN;
        out[1] = cy + along * TILT_RAD_SIN + across * TILT_RAD_COS;
    }

    private static float lineSlotV(float slot) {
        return slot - (STAFF_LINES - 1) * 0.5f;
    }

    // ---------------- 绘制原语 ----------------

    private static void stroke(SpriteBatch sb, float x1, float y1, float x2, float y2,
                               float thickness, Color color, float alpha) {
        if (alpha <= 0.004f || thickness <= 0.3f) {
            return;
        }
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len <= 0.8f) {
            return;
        }
        float rot = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        // 厚度兜底：谱线也不允许变成发丝
        float t = Math.max(4.5f, thickness);
        sb.setColor(Draw.tint(color, alpha));
        sb.draw(LINE, (x1 + x2) * 0.5f - len * 0.5f, (y1 + y2) * 0.5f - t * 0.5f,
                len * 0.5f, t * 0.5f, len, t,
                1.0f, 1.0f, rot);
    }

    private void staffLine(SpriteBatch sb, float cx, float cy, float halfLen, float slot,
                           float thick, Color color, float alpha) {
        float v = lineSlotV(slot);
        toWorldAt(cx, cy, halfLen, -1.0f, v, this.tmpA);
        toWorldAt(cx, cy, halfLen, 1.0f, v, this.tmpB);
        stroke(sb, this.tmpA[0], this.tmpA[1], this.tmpB[0], this.tmpB[1],
                thick, color, alpha);
    }

    private void barLine(SpriteBatch sb, float cx, float cy, float halfLen, float u,
                         float halfHeightV, float thick, Color color, float alpha) {
        toWorldAt(cx, cy, halfLen, u, -halfHeightV, this.tmpA);
        toWorldAt(cx, cy, halfLen, u, halfHeightV, this.tmpB);
        stroke(sb, this.tmpA[0], this.tmpA[1], this.tmpB[0], this.tmpB[1],
                thick, color, alpha);
    }

    private void clef(SpriteBatch sb, float cx, float cy, float halfLen, float sc,
                      Color color, float alpha) {
        float size = LINE_GAP * Settings.scale * 2.6f;
        float u0 = -0.94f;
        float prevX = 0.0f;
        float prevY = 0.0f;

        int seg = 10;
        for (int i = 0; i <= seg; ++i) {
            float f = i / (float) seg;
            float uu = u0 + (MathUtils.sin(f * 3.1f) * size * 0.22f) / Math.max(1.0f, halfLen);
            float vv = (f - 0.5f) * size * 1.85f / (LINE_GAP * Settings.scale);
            toWorldAt(cx, cy, halfLen, uu, vv, this.tmpA);
            if (i > 0) {
                stroke(sb, prevX, prevY, this.tmpA[0], this.tmpA[1],
                        Math.max(5.0f, 4.8f * sc), color, alpha);
            }
            prevX = this.tmpA[0];
            prevY = this.tmpA[1];
        }

        toWorldAt(cx, cy, halfLen, u0, -size * 0.62f / (LINE_GAP * Settings.scale), this.tmpA);
        Draw.ring(sb, this.tmpA[0], this.tmpA[1], size * 0.34f, size * 0.30f,
                Math.max(4.5f, 3.4f * sc), color, alpha * 0.92f, 18);
        toWorldAt(cx, cy, halfLen, u0, size * 0.95f / (LINE_GAP * Settings.scale), this.tmpA);
        Draw.glow(sb, this.tmpA[0], this.tmpA[1], size * 0.62f, size * 0.62f, 0.0f,
                color, alpha * 0.80f);
    }

    /** 画一枚音符：暗影 + 外发光 + 音符本体，字形跟随谱面倾斜。 */
    private static void note(SpriteBatch sb, NoteGlyph.Kind kind, float x, float y,
                             float size, float swing, Color color, float alpha, float glow) {
        Draw.glow(sb, x + size * 0.08f, y - size * 0.12f,
                size * 1.25f, size * 1.25f, 0.0f, DARK, alpha * 0.50f);
        float halo = size * (1.5f + 0.9f * glow);
        Draw.glow(sb, x, y, halo, halo, 0.0f, color, alpha * (0.22f + 0.26f * glow));
        Draw.glow(sb, x, y, size * 1.05f, size * 1.05f, 0.0f, color,
                alpha * (0.28f + 0.28f * glow));
        NoteGlyph.draw(sb, x, y, size, swing + TILT_DEG, kind, color,
                Math.min(1.0f, alpha * 1.1f), 0.12f);
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.fade <= 0.004f || this.owner == null) {
            return;
        }
        float sc = Settings.scale;
        float cx = BodyFx.centerX(this.owner);
        float cy = staffCenterY(this.owner);
        float halfLen = staffHalfLen(this.owner);
        float a = this.fade;
        float staffHeightV = (STAFF_LINES - 1) * 0.5f;

        float breath = 0.5f + 0.5f * MathUtils.sin(this.time * MathUtils.PI2 / 2.6f);
        float bright = 0.90f + 0.10f * breath;
        float excite = this.excite;

        int living = 0;
        for (int c = 0; c < MAX_CHORDS; ++c) {
            if (this.chords[c].active) {
                ++living;
            }
        }

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        // ---- 0. 谱面底光 ----
        float glowW = halfLen * 2.15f;
        float glowH = (STAFF_LINES - 1) * LINE_GAP * sc * 2.6f;
        Draw.glow(sb, cx, cy, glowW, glowH, TILT_DEG, AZURE,
                a * (0.13f + 0.02f * breath + 0.035f * living + 0.10f * excite));
        Draw.glow(sb, cx, cy, halfLen * 1.95f, glowH * 0.62f, TILT_DEG, ICE,
                a * (0.08f + 0.02f * breath + 0.024f * living + 0.07f * excite));

        // ---- 1. 五条平行谱线 ----
        float thick = Math.max(5.0f, 5.2f * sc);
        for (int i = 0; i < STAFF_LINES; ++i) {
            boolean edge = (i == 0 || i == STAFF_LINES - 1);
            staffLine(sb, cx, cy, halfLen, i,
                    thick * (edge ? 1.20f : 0.96f),
                    edge ? ICE : AZURE,
                    a * (edge ? 0.98f : 0.80f) * bright * (1.0f + 0.12f * excite));
        }

        // ---- 2. 小节线 ----
        float barHalfV = staffHeightV * 1.20f;
        float barThick = Math.max(4.5f, 4.4f * sc);
        for (int m = 1; m < MEASURES; ++m) {
            float u = -1.0f + 2.0f * m / MEASURES;
            barLine(sb, cx, cy, halfLen, u, barHalfV, barThick, ICE,
                    a * (0.62f + 0.10f * excite) * bright);
        }
        barLine(sb, cx, cy, halfLen, -1.0f, barHalfV, barThick * 1.15f, ICE, a * 0.76f * bright);
        barLine(sb, cx, cy, halfLen, 1.0f, barHalfV, barThick * 1.45f, WHITE, a * 0.92f * bright);
        barLine(sb, cx, cy, halfLen, 0.945f, barHalfV, barThick * 0.95f, ICE, a * 0.60f * bright);

        // ---- 3. 左端谱号 ----
        clef(sb, cx, cy, halfLen, sc, ICE, a * 0.84f * bright);

        // ---- 4. 音符：逐声部绘制，只有被按住的那组在颤 ----
        for (int c = 0; c < MAX_CHORDS; ++c) {
            Chord ch = this.chords[c];
            if (!ch.active) {
                continue;
            }
            // 出现：swingOut 弹性放大（自带过冲，读作「放上去」）
            float appearEase = Interpolation.swingOut.apply(0.0f, 1.0f, ch.appear);
            float vanishEase = ch.vanish;
            float t = ch.tremor;

            // 同组共享相位基准，三枚同步抖；未按住的组 t=0，三项动态全为零
            float tremorRaw = MathUtils.sin(this.time * 46.0f + c * 2.1f) * 0.55f
                    + MathUtils.sin(this.time * 71.0f + c * 1.3f) * 0.45f;
            float shakeX = tremorRaw * 8.0f * sc * t;
            float shakeY = MathUtils.sin(this.time * 53.0f + c * 2.7f) * 4.5f * sc * t;
            float swing = MathUtils.sin(this.time * 38.0f + c) * 16.0f * t;

            // 出现：从下方升起
            float riseY = (1.0f - appearEase) * 26.0f * sc;
            // 消失：改成缓动上浮 + 更明显缩小，让「散去」有过程
            float vEase = Interpolation.pow2In.apply(0.0f, 1.0f, vanishEase);
            float vanishY = vEase * 52.0f * sc;

            float sizeMul = (0.45f + 0.55f * appearEase)
                    * (1.0f - 0.62f * vEase)
                    * (1.0f + 0.30f * t + 0.12f * tremorRaw * t);
            float size = Math.max(30.0f, NOTE_SIZE * sc) * sizeMul;

            float alpha = a * (0.95f + 0.05f * breath)
                    * MathUtils.clamp(appearEase, 0.0f, 1.0f)
                    * (1.0f - vEase)
                    * (1.0f + 0.35f * t);
            if (alpha <= 0.008f || size <= 1.0f) {
                continue;
            }

            for (int k = 0; k < NOTES_PER_CHORD; ++k) {
                float breathI = 0.97f + 0.03f * MathUtils.sin(this.time * 2.4f + c * 1.7f + k);
                note(sb, ch.kinds[k],
                        ch.x[k] + shakeX,
                        ch.y[k] + shakeY - riseY + vanishY,
                        size * breathI,
                        swing,
                        k == 1 ? WHITE : ICE,
                        Math.min(1.0f, alpha),
                        t);
            }
        }

        sb.setBlendFunction(src, dst);
    }
}
