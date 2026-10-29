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
 * 「心之堡垒」在<b>姿态切换</b>时的爆发：<b>旧立方体炸开、新立方体成形</b>。
 * <p>
 * 这是这张牌真正生效的唯一时机（{@code onChangeStance} 里姿态不同就给格挡），
 * 所以演出要明确表达「换了一层壳」：
 * <ol>
 *   <li><b>炸壳</b>：旧姿态颜色的方块碎片向外飞散</li>
 *   <li><b>冲击环</b>：由内向外快速扩张的粗亮环</li>
 *   <li><b>新壳</b>：一个旋转的立方体从零膨胀到满，颜色是切换后那种姿态的</li>
 * </ol>
 * 首尾颜色不同是刻意的：玩家能从颜色变化读出「从平静切到了愤怒」。
 * <p>
 * <h2>不用星芒</h2>
 * 碎片是带厚度的小方块光斑，新壳是真正的 3D 立方体线框，全程无星形。
 * <p>
 * <h2>克制</h2>
 * 不震屏、不音效 —— 一回合可能切换好几次姿态。
 */
final class FortressBurstEffect extends AbstractGameEffect {
    /** 碎片数量。 */
    private static final int SHARDS = 16;
    private static final float CORE_HEIGHT = 0.54f;
    /** 新壳层数（线框重数）。 */
    private static final int SHELLS = 2;

    /** 立方体顶点与棱，与常驻表现同一套定义。 */
    private static final float[][] V = {
            {-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1},
            {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}
    };
    private static final int[][] EDGES = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0},
            {4, 5}, {5, 6}, {6, 7}, {7, 4},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };
    /** 每帧复用的投影结果。 */
    private final float[][] proj = new float[8][3];

    private final float cx;
    private final float cy;
    private final float hw;
    private final float h;
    private final float life;
    private final Color oldColor;
    private final Color newColor;

    /** 碎片各自的初始角度与速度。 */
    private final float[] ang = new float[SHARDS];
    private final float[] spd = new float[SHARDS];
    private final float[] size = new float[SHARDS];

    private float time;

    FortressBurstEffect(AbstractCreature owner, Color oldColor, Color newColor, String newStance) {
        this.cx = BodyFx.centerX(owner);
        this.cy = BodyFx.groundY(owner) + BodyFx.height(owner) * CORE_HEIGHT;
        this.hw = BodyFx.halfWidth(owner);
        this.h = BodyFx.height(owner);
        this.oldColor = new Color(oldColor.r, oldColor.g, oldColor.b, 1.0f);
        this.newColor = new Color(newColor.r, newColor.g, newColor.b, 1.0f);
        this.life = Settings.FAST_MODE ? 0.34f : 0.62f;
        this.startingDuration = this.duration = this.life;
        // 包住身体，必须画在身体前方
        this.renderBehind = false;
        for (int i = 0; i < SHARDS; ++i) {
            this.ang[i] = MathUtils.PI2 * i / SHARDS + MathUtils.random(-0.18f, 0.18f);
            this.spd[i] = MathUtils.random(0.75f, 1.30f);
            this.size[i] = MathUtils.random(5.0f, 10.0f);
        }
    }

    @Override
    public void update() {
        this.time += Gdx.graphics.getDeltaTime();
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration <= 0.0f) {
            this.isDone = true;
        }
    }

    private static void band(SpriteBatch sb, float x1, float y1, float x2, float y2,
                             float thickness, Color color, float alpha) {
        if (alpha <= 0.004f || thickness <= 0.4f) {
            return;
        }
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len <= 0.8f) {
            return;
        }
        float rot = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        Draw.glow(sb, (x1 + x2) * 0.5f, (y1 + y2) * 0.5f,
                len * 1.04f, thickness, rot, color, alpha);
    }

    /** 投影一个立方体。 */
    private void project(float rotY, float rotX, float size, float ox, float oy) {
        float cosY = MathUtils.cos(rotY);
        float sinY = MathUtils.sin(rotY);
        float cosX = MathUtils.cos(rotX);
        float sinX = MathUtils.sin(rotX);
        float focal = 4.2f;
        for (int i = 0; i < 8; ++i) {
            float x = V[i][0];
            float y = V[i][1];
            float z = V[i][2];
            float x1 = x * cosY - z * sinY;
            float z1 = x * sinY + z * cosY;
            float y1 = y * cosX - z1 * sinX;
            float z2 = y * sinX + z1 * cosX;
            float k = focal / (focal + z2);
            this.proj[i][0] = ox + x1 * size * k;
            this.proj[i][1] = oy + y1 * size * k;
            this.proj[i][2] = z2;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.duration <= 0.0f) {
            return;
        }
        float sc = Settings.scale;
        float p = MathUtils.clamp(1.0f - this.duration / this.life, 0.0f, 1.0f);
        float out = Interpolation.pow3Out.apply(p);
        float fade = p < 0.60f ? 1.0f : 1.0f - (p - 0.60f) / 0.40f;
        if (fade <= 0.01f) {
            return;
        }

        float size = this.hw * 1.55f;

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        // ---- 1. 炸壳：旧颜色的方块碎片向外飞散 ----
        for (int i = 0; i < SHARDS; ++i) {
            float d = (0.35f + 1.05f * out) * this.spd[i];
            float x = this.cx + MathUtils.cos(this.ang[i]) * size * d;
            float y = this.cy + MathUtils.sin(this.ang[i]) * size * d;
            float s = this.size[i] * sc * (1.0f - 0.45f * p);
            // 碎片本身是一小块方形光斑（用 band 画两条正交短带，读作方块）
            band(sb, x - s, y, x + s, y, s * 1.1f, this.oldColor, fade * 0.75f);
            band(sb, x, y - s, x, y + s, s * 1.1f, this.oldColor, fade * 0.75f);
            Draw.glow(sb, x, y, s * 1.8f, s * 1.8f, 0.0f, this.oldColor, fade * 0.45f);
        }

        // ---- 2. 冲击环 ----
        {
            float rr = size * (0.5f + 1.6f * out);
            Draw.ring(sb, this.cx, this.cy, rr, rr,
                    MathUtils.lerp(10.0f, 2.4f, p) * sc, brighten(this.newColor),
                    fade * 0.70f, 48);
        }

        // ---- 3. 新壳：旋转立方体从零膨胀到满 ----
        float grow = Interpolation.pow2Out.apply(MathUtils.clamp(p / 0.55f, 0.0f, 1.0f));
        if (grow > 0.02f) {
            float baseSize = size * grow;
            float rotY = p * 2.4f;
            float rotX = 0.42f + p * 0.5f;
            float thick = Math.max(3.6f, 6.0f * sc);

            for (int layer = 0; layer < SHELLS; ++layer) {
                float ls = baseSize * (1.0f - 0.18f * layer);
                project(rotY + layer * 0.4f, rotX, ls, this.cx, this.cy);
                float alpha = fade * (0.85f - 0.28f * layer) * (1.0f - 0.25f * p);
                Color c = layer == 0 ? this.newColor : brighten(this.newColor);
                for (int[] e : EDGES) {
                    band(sb, this.proj[e[0]][0], this.proj[e[0]][1],
                            this.proj[e[1]][0], this.proj[e[1]][1],
                            thick * (1.0f - 0.22f * layer), c, alpha);
                }
                // 顶点节点
                for (int i = 0; i < 8; ++i) {
                    Draw.glow(sb, this.proj[i][0], this.proj[i][1],
                            thick * 2.2f, thick * 2.2f, 0.0f, c, alpha * 0.80f);
                }
            }
        }

        sb.setBlendFunction(src, dst);
    }

    private static Color brighten(Color c) {
        return new Color(
                Math.min(1.0f, c.r * 0.5f + 0.5f),
                Math.min(1.0f, c.g * 0.5f + 0.5f),
                Math.min(1.0f, c.b * 0.5f + 0.5f),
                1.0f);
    }

    @Override
    public void dispose() {
    }
}
