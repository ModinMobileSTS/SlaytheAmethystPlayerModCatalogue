package neowpreview;

import basemod.IUIElement;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;

import java.util.function.IntConsumer;

/**
 * 百分比滑条。BaseMod 自带的 ModSlider 只能从 0 起步，
 * 这里自己画一个：最小值可以是 50%，而且让 {@code center}（100%）正好落在滑条正中间，
 * 左右两段各自线性。
 */
public class PercentSlider implements IUIElement {

    private static final float SLIDE_W = 230.0f * Settings.scale;

    private final String label;
    private final String suffix;
    private final int min;
    private final int max;
    private final int center;
    private final IntConsumer change;

    private final float x;
    private final float y;
    private final float sliderX;
    private final Hitbox hb;
    private final Hitbox bgHb;

    private float handleX;
    private boolean grabbed = false;
    private int value;

    public PercentSlider(String label, float posX, float posY, int min, int max, int center,
                         int initial, IntConsumer change) {
        this(label, posX, posY, min, max, center, initial, change, "%");
    }

    /** suffix 是数值后面显示的单位，比如 "%" 或空串。 */
    public PercentSlider(String label, float posX, float posY, int min, int max, int center,
                         int initial, IntConsumer change, String suffix) {
        this.label = label;
        this.suffix = suffix == null ? "" : suffix;
        this.min = min;
        this.max = max;
        this.center = center;
        this.change = change;
        this.x = posX * Settings.scale;
        this.y = posY * Settings.scale;
        this.sliderX = this.x - 11.0f * Settings.scale;
        this.value = clamp(initial);
        this.handleX = handleFor(this.value);
        this.hb = new Hitbox(42.0f * Settings.scale, 38.0f * Settings.scale);
        this.hb.move(this.handleX, this.y);
        this.bgHb = new Hitbox(300.0f * Settings.scale, 38.0f * Settings.scale);
        this.bgHb.move(this.sliderX + SLIDE_W * 0.5f, this.y);
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        sb.draw(ImageMaster.OPTION_SLIDER_BG, this.sliderX, this.y - 12.0f, 0.0f, 12.0f, 250.0f, 24.0f,
                Settings.scale, Settings.scale, 0.0f, 0, 0, 250, 24, false, false);
        sb.draw(ImageMaster.OPTION_SLIDER, this.handleX - 22.0f, this.y - 22.0f, 22.0f, 22.0f, 44.0f, 44.0f,
                Settings.scale, Settings.scale, 0.0f, 0, 0, 44, 44, false, false);
        FontHelper.renderFontCentered(sb, FontHelper.tipBodyFont, this.label,
                this.sliderX - 55.0f * Settings.scale, this.y, Color.WHITE);
        FontHelper.renderFontCentered(sb, FontHelper.tipBodyFont, this.value + this.suffix,
                this.sliderX + SLIDE_W + 55.0f * Settings.scale, this.y,
                this.grabbed ? Settings.GREEN_TEXT_COLOR : Settings.BLUE_TEXT_COLOR);
    }

    @Override
    public void update() {
        this.hb.move(this.handleX, this.y);
        this.hb.update();
        this.bgHb.update();
        if (this.grabbed) {
            if (InputHelper.isMouseDown) {
                float target = MathHelper.fadeLerpSnap(this.handleX, InputHelper.mX);
                this.handleX = Math.min(this.sliderX + SLIDE_W + 11.0f * Settings.scale,
                        Math.max(target, this.sliderX + 11.0f * Settings.scale));
            } else {
                this.grabbed = false;
            }
        } else if (InputHelper.justClickedLeft && (this.hb.hovered || this.bgHb.hovered)) {
            this.grabbed = true;
        }
        int current = valueFor(this.handleX);
        if (current != this.value) {
            this.value = current;
            if (this.change != null) {
                this.change.accept(current);
            }
        }
    }

    @Override
    public int renderLayer() {
        return 1;
    }

    @Override
    public int updateOrder() {
        return 1;
    }

    public int getPercent() {
        return this.value;
    }

    public void setPercent(int percent) {
        this.value = clamp(percent);
        this.handleX = handleFor(this.value);
    }

    /** 100% 固定映射到滑条正中间，左半段是 min..100，右半段是 100..max。 */
    private float ratioFor(int percent) {
        if (percent <= this.center) {
            return 0.5f * (percent - (float) this.min) / (float) (this.center - this.min);
        }
        return 0.5f + 0.5f * (percent - (float) this.center) / (float) (this.max - this.center);
    }

    private float handleFor(int percent) {
        return this.sliderX + SLIDE_W * ratioFor(percent) + 11.0f * Settings.scale;
    }

    private int valueFor(float handle) {
        float t = (handle - 11.0f * Settings.scale - this.sliderX) / SLIDE_W;
        t = Math.max(0.0f, Math.min(1.0f, t));
        int percent;
        if (t <= 0.5f) {
            percent = Math.round(this.min + (t / 0.5f) * (this.center - this.min));
        } else {
            percent = Math.round(this.center + ((t - 0.5f) / 0.5f) * (this.max - this.center));
        }
        return clamp(percent);
    }

    private int clamp(int percent) {
        return Math.max(this.min, Math.min(this.max, percent));
    }
}
