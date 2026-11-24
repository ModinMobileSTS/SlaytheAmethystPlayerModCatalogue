package neowpreview;

import basemod.IUIElement;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;

import java.util.function.Consumer;

/** 设置面板里的开关：点一下立刻生效，不需要重开游戏。 */
public class ConfigToggle implements IUIElement {

    private static final float BOX = 28.0f * Settings.scale;

    private final String label;
    private final float x;
    private final float y;
    private final Consumer<Boolean> change;
    private final Hitbox hb;
    private boolean value;

    public ConfigToggle(String label, float posX, float posY, boolean initial, Consumer<Boolean> change) {
        this.label = label;
        this.change = change;
        this.x = posX * Settings.scale;
        this.y = posY * Settings.scale;
        this.value = initial;
        this.hb = new Hitbox(520.0f * Settings.scale, 44.0f * Settings.scale);
        this.hb.move(this.x + 240.0f * Settings.scale, this.y);
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.value ? Settings.GREEN_TEXT_COLOR : Color.DARK_GRAY);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, this.x, this.y - BOX * 0.5f, BOX, BOX);
        sb.setColor(Color.WHITE);
        FontHelper.renderFontLeft(sb, FontHelper.tipBodyFont, this.label,
                this.x + BOX + 14.0f * Settings.scale, this.y, Color.WHITE);
        FontHelper.renderFontCentered(sb, FontHelper.tipBodyFont, this.value ? Lang.on() : Lang.off(),
                this.x + 470.0f * Settings.scale, this.y,
                this.value ? Settings.GREEN_TEXT_COLOR : Settings.RED_TEXT_COLOR);
        if (this.hb.hovered) {
            this.hb.render(sb);
        }
    }

    @Override
    public void update() {
        this.hb.update();
        if (this.hb.hovered && InputHelper.justClickedLeft) {
            this.value = !this.value;
            if (this.change != null) {
                this.change.accept(this.value);
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

    public boolean getValue() {
        return this.value;
    }

    public void setValue(boolean newValue) {
        this.value = newValue;
    }
}
