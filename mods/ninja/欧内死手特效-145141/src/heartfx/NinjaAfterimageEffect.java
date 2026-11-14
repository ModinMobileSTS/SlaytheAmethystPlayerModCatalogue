package heartfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

import java.lang.reflect.Field;

/**
 * 蕾忍突进时的红色拖影：沿突进路径密集留 15 个淡出的红色"纸片人"剪影，
 * 越靠近当前位置越明显，远处逐渐稀薄，形成连续拖影。
 * 直接用玩家自己的站立贴图着色绘制，所以轮廓和本体完全一致。
 * 只对"普通贴图"角色启用（Spine 角色自动跳过），字段不可见时用反射兜底，
 * 反射失败则按普通贴图处理。
 */
public class NinjaAfterimageEffect extends AbstractGameEffect {

    private static final int GHOSTS = 15;
    private static final float DUR = 0.28f;
    private static final float[] FRACS = new float[GHOSTS];
    private static final float[] ALPHAS = new float[GHOSTS];
    private static final Color GHOST = new Color(0.80f, 0.10f, 0.08f, 1.0f);

    static {
        for (int i = 0; i < GHOSTS; i++) {
            float t = (float) i / (float) (GHOSTS - 1);
            FRACS[i] = t;
            // 尾部淡、靠近当前位置浓
            ALPHAS[i] = 0.08f + 0.54f * t;
        }
    }

    private static final Field ATLAS_FIELD = findAtlasField();

    private final Texture tex;
    private final float baseX;
    private final float baseY;
    private final float dist;
    private final boolean flipX;
    private final boolean flipY;
    private float timer = 0.0f;

    public NinjaAfterimageEffect(AbstractPlayer p, float dist) {
        this.duration = DUR;
        this.startingDuration = DUR;
        this.tex = p.img;
        this.baseX = p.drawX;
        this.baseY = p.drawY;
        this.dist = dist;
        this.flipX = p.flipHorizontal;
        this.flipY = p.flipVertical;
        this.renderBehind = false;
    }

    private static Field findAtlasField() {
        try {
            Field f = AbstractCreature.class.getDeclaredField("atlas");
            f.setAccessible(true);
            return f;
        } catch (Throwable t) {
            return null;
        }
    }

    /** 只有普通贴图角色才画残影，避免 Spine 角色画出错位的图。 */
    public static boolean canUse(AbstractPlayer p) {
        if (p == null || p.img == null) {
            return false;
        }
        if (ATLAS_FIELD == null) {
            return true;
        }
        try {
            return ATLAS_FIELD.get(p) == null;
        } catch (Throwable t) {
            return true;
        }
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
        if (this.tex == null) {
            return;
        }
        float k = Math.min(1.0f, this.timer / DUR);
        float fade = 1.0f - k;
        if (fade <= 0.0f) {
            return;
        }
        float w = (float) this.tex.getWidth() * Settings.scale;
        float h = (float) this.tex.getHeight() * Settings.scale;

        sb.setBlendFunction(770, 771);
        for (int i = 0; i < GHOSTS; i++) {
            float a = ALPHAS[i] * fade;
            if (a <= 0.01f) {
                continue;
            }
            Color c = GHOST.cpy();
            c.a = a;
            sb.setColor(c);
            float x = this.baseX + this.dist * FRACS[i] - w * 0.5f;
            sb.draw(this.tex, x, this.baseY, w, h,
                    0, 0, this.tex.getWidth(), this.tex.getHeight(),
                    this.flipX, this.flipY);
        }
        sb.setColor(Color.WHITE);
    }

    @Override
    public void dispose() {
    }
}
