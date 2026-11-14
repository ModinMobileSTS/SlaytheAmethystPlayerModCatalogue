package heartfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 斩杀瞬间的血液飞溅 + 地面血迹。
 * 血滴是一次性抛物线飞散，地面血迹则是静态的 2D 血斑，会一直留在原地，
 * 直到战斗结束才快速淡出，所以看起来就是"地上留下了血"。
 *
 * 血量多少与怪物模型体积成正比：体积越大，血滴越多、血迹越多、摊得越开，
 * 大体型怪物被斩杀时不会只洒出一小滩。
 */
public class BloodSplatterEffect extends AbstractGameEffect {

    private static final float KEEP = 45.0f;
    private static final float CLOSE_FADE = 0.35f;
    private static final float GRAVITY = 1500.0f;

    /** 参照的普通怪物碰撞体积，用来换算"相对体型"。 */
    private static final float BASE_W = 200.0f;
    private static final float BASE_H = 200.0f;
    /** 整体放大系数。 */
    private static final float GROW = 1.25f;

    private final TextureAtlas.AtlasRegion img = ImageMaster.GLOW_SPARK_2;
    private final Color tmp = new Color();

    private final int drops;
    private final int stains;

    private final float[] ox;
    private final float[] oy;
    private final float[] vx;
    private final float[] vy;
    private final float[] life;
    private final float[] size;

    private final float[] sx;
    private final float[] sy;
    private final float[] sw;
    private final float[] sh;
    private final float[] srot;
    private final float[] sAt;

    private float timer = 0.0f;
    private float fade = 1.0f;
    private boolean closing = false;

    public BloodSplatterEffect(float cx, float cy, float groundY, float bodyW, float bodyH) {
        this.duration = KEEP;
        this.startingDuration = KEEP;
        float sc = Settings.scale;

        // 相对体型：体积开方，1.0 约等于普通怪物
        float baseArea = (BASE_W * sc) * (BASE_H * sc);
        float area = Math.max(bodyW * bodyH, 1.0f);
        float ratio = (float) Math.sqrt(area / baseArea);
        if (ratio < 0.7f) {
            ratio = 0.7f;
        }
        if (ratio > 2.6f) {
            ratio = 2.6f;
        }

        this.drops = clampInt(Math.round(18.0f * ratio), 14, 46);
        this.stains = clampInt(Math.round(9.0f * ratio), 7, 26);

        this.ox = new float[this.drops];
        this.oy = new float[this.drops];
        this.vx = new float[this.drops];
        this.vy = new float[this.drops];
        this.life = new float[this.drops];
        this.size = new float[this.drops];

        this.sx = new float[this.stains];
        this.sy = new float[this.stains];
        this.sw = new float[this.stains];
        this.sh = new float[this.stains];
        this.srot = new float[this.stains];
        this.sAt = new float[this.stains];

        // 出体范围随体型放大
        float jitterW = Math.max(bodyW * 0.28f, 22.0f * sc);
        float jitterH = Math.max(bodyH * 0.28f, 32.0f * sc);
        float speedMul = 0.85f + 0.30f * ratio;

        for (int i = 0; i < this.drops; i++) {
            this.ox[i] = cx + MathUtils.random(-jitterW, jitterW);
            this.oy[i] = cy + MathUtils.random(-jitterH, jitterH);
            this.vx[i] = MathUtils.random(-1.0f, 1.0f)
                    * MathUtils.random(240.0f, 700.0f) * sc * speedMul;
            this.vy[i] = MathUtils.random(120.0f, 460.0f) * sc * speedMul;
            this.life[i] = MathUtils.random(0.22f, 0.46f) * (0.9f + 0.2f * ratio);
            this.size[i] = MathUtils.random(10.0f, 26.0f) * GROW * (0.8f + 0.3f * ratio);
        }

        // 地面血迹：数量、大小、摊开半径都随体型放大
        float spread = 190.0f * sc * GROW * (0.7f + 0.5f * ratio);
        float stainMul = GROW * (0.75f + 0.40f * ratio);
        for (int i = 0; i < this.stains; i++) {
            this.sx[i] = cx + MathUtils.random(-spread, spread);
            this.sy[i] = groundY + MathUtils.random(2.0f, 22.0f) * sc * (0.8f + 0.4f * ratio);
            float w = MathUtils.random(46.0f, 130.0f) * stainMul;
            this.sw[i] = w;
            this.sh[i] = w * MathUtils.random(0.30f, 0.55f);
            this.srot[i] = MathUtils.random(0.0f, 180.0f);
            this.sAt[i] = MathUtils.random(0.05f, 0.26f);
        }
    }

    private static int clampInt(int v, int lo, int hi) {
        if (v < lo) {
            return lo;
        }
        return Math.min(v, hi);
    }

    @Override
    public void update() {
        float dt = Gdx.graphics.getDeltaTime();
        this.timer += dt;

        AbstractRoom room = AbstractDungeon.getCurrRoom();
        if (!this.closing && (room == null || room.phase != AbstractRoom.RoomPhase.COMBAT)) {
            this.closing = true;
        }
        if (this.closing) {
            this.fade -= dt / CLOSE_FADE;
            if (this.fade <= 0.0f) {
                this.fade = 0.0f;
                this.isDone = true;
            }
        } else if (this.timer >= KEEP) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        float sc = Settings.scale;
        sb.setBlendFunction(770, 771);

        // 地面血迹：落地后就一直留在原地
        for (int i = 0; i < this.stains; i++) {
            if (this.timer < this.sAt[i]) {
                continue;
            }
            float k = Math.min(1.0f, (this.timer - this.sAt[i]) / 0.12f);
            float grow = (0.55f + 0.45f * k) * sc;
            float w = this.sw[i] * grow;
            float h = this.sh[i] * grow;
            this.tmp.set(0.30f, 0.020f, 0.030f, 0.62f * this.fade * k);
            sb.setColor(this.tmp);
            sb.draw(this.img, this.sx[i] - w * 0.5f, this.sy[i] - h * 0.5f,
                    w * 0.5f, h * 0.5f, w, h, 1.0f, 1.0f, this.srot[i]);
        }

        // 血滴：抛物线飞散
        for (int i = 0; i < this.drops; i++) {
            if (this.timer >= this.life[i]) {
                continue;
            }
            float t = this.timer;
            float x = this.ox[i] + this.vx[i] * t;
            float y = this.oy[i] + this.vy[i] * t - 0.5f * GRAVITY * sc * t * t;
            float vyNow = this.vy[i] - GRAVITY * sc * t;
            float rot = (float) Math.toDegrees(Math.atan2(vyNow, this.vx[i]));
            float al = this.fade * (1.0f - t / this.life[i]);
            float sz = this.size[i] * sc;

            sb.setColor(0.0f, 0.0f, 0.0f, 0.55f * al);
            sb.draw(this.img, x - sz * 0.5f, y - sz * 0.5f,
                    sz * 0.5f, sz * 0.5f, sz, sz, 2.4f, 1.0f, rot);

            this.tmp.set(0.72f, 0.05f, 0.07f, al);
            sb.setColor(this.tmp);
            sb.draw(this.img, x - sz * 0.5f, y - sz * 0.5f,
                    sz * 0.5f, sz * 0.5f, sz, sz, 2.0f, 0.85f, rot);
        }

        sb.setColor(Color.WHITE);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}
