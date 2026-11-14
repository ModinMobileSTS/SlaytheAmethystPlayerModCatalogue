package heartfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 蕾忍突进时的「速度线」：在纸片人身后（左侧）拖出一束横向的暗红速度线，
 * 整体高度与蕾忍模型高度相近，只在突进期间出现、随即淡出。
 * 线条从模型左边缘向左延伸，末端淡、靠近模型的一端更亮，做出动漫速度线的拖尾感。
 */
public class DashSpeedLinesEffect extends AbstractGameEffect {

    private static final int LINES = 11;
    private static final float DUR = 0.30f;
    private static final float IN_DUR = 0.03f;
    private static final float HOLD = 0.45f;

    private static final Color LINE = new Color(0.40f, 0.012f, 0.018f, 1.0f);
    private static final Color HEAD = new Color(0.72f, 0.060f, 0.050f, 1.0f);

    private final Texture tex = ImageMaster.WHITE_SQUARE_IMG;
    private final AbstractPlayer p;
    private final float modelW;
    private final float modelH;
    private final float[] ry = new float[LINES];
    private final float[] rlen = new float[LINES];
    private final float[] rthick = new float[LINES];
    private final float[] ralpha = new float[LINES];
    private float timer = 0.0f;

    public DashSpeedLinesEffect(AbstractPlayer p, float dashDist) {
        this.duration = DUR;
        this.startingDuration = DUR;
        this.p = p;
        Texture img = p.img;
        this.modelW = img != null
                ? (float) img.getWidth() * Settings.scale
                : 170.0f * Settings.scale;
        this.modelH = img != null
                ? (float) img.getHeight() * Settings.scale
                : 230.0f * Settings.scale;

        for (int i = 0; i < LINES; i++) {
            this.ry[i] = MathUtils.random(0.06f, 0.94f);
            this.rlen[i] = this.modelW * MathUtils.random(0.55f, 1.9f)
                    + dashDist * MathUtils.random(0.05f, 0.35f);
            this.rthick[i] = MathUtils.random(3.0f, 10.0f) * Settings.scale;
            this.ralpha[i] = MathUtils.random(0.28f, 0.62f);
        }
        this.renderBehind = false;
    }

    @Override
    public void update() {
        this.timer += Gdx.graphics.getDeltaTime();
        if (this.timer >= DUR) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.p == null) {
            return;
        }
        float k = Math.min(1.0f, this.timer / DUR);
        float env = k < HOLD ? 1.0f : (1.0f - (k - HOLD) / (1.0f - HOLD));
        env *= Math.min(1.0f, this.timer / IN_DUR);
        if (env <= 0.01f) {
            return;
        }

        // 模型左边缘：速度线全部从这条线往左拖
        float edge = this.p.drawX + this.p.animX - this.modelW * 0.5f;

        sb.setBlendFunction(770, 771);
        for (int i = 0; i < LINES; i++) {
            float y = this.p.drawY + this.modelH * this.ry[i];
            float len = this.rlen[i];
            float th = this.rthick[i];
            float a = this.ralpha[i] * env;
            if (len <= 2.0f) {
                continue;
            }
            // 整条速度线（末端渐淡靠靠近模型的那段加亮来表现）
            this.drawLine(sb, edge - len, y - th * 0.5f, len, th, LINE, a);
            this.drawLine(sb, edge - len * 0.45f, y - th * 0.5f, len * 0.45f, th, HEAD, a * 0.75f);
        }
        sb.setColor(Color.WHITE);
        sb.setBlendFunction(770, 771);
    }

    private void drawLine(SpriteBatch sb, float x, float y, float w, float h,
                          Color base, float a) {
        Color c = base.cpy();
        c.a = Math.max(0.0f, Math.min(1.0f, a));
        sb.setColor(c);
        sb.draw(this.tex, x, y, w, h,
                0, 0, this.tex.getWidth(), this.tex.getHeight(), false, false);
    }

    @Override
    public void dispose() {
    }
}
