package cardforge.zh;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;

/** 极简的矩形文字按钮，用于铸卡界面的顶部栏、底栏与卡池弹窗。 */
final class TextButton {
    private static final Color BG_IDLE = new Color(0.16f, 0.16f, 0.20f, 0.92f);
    private static final Color BG_HOVER = new Color(0.32f, 0.30f, 0.38f, 0.96f);
    private static final Color BG_ACTIVE = new Color(0.46f, 0.36f, 0.12f, 0.96f);
    private static final Color BG_DISABLED = new Color(0.11f, 0.11f, 0.14f, 0.55f);
    private static final Color BORDER_ON = new Color(1.0f, 0.89f, 0.50f, 0.55f);
    private static final Color BORDER_OFF = new Color(1.0f, 1.0f, 1.0f, 0.14f);
    private static final Color TEXT_OFF = new Color(0.62f, 0.62f, 0.66f, 0.92f);

    private String label;
    private float x;
    private float y;
    private float w;
    private float h;
    boolean enabled = true;
    boolean highlighted;
    boolean hovered;

    TextButton(String label, float x, float y, float w, float h) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    void setLabel(String label) {
        this.label = label;
    }

    void setBounds(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    boolean contains(float px, float py) {
        return px >= x - w * 0.5f && px <= x + w * 0.5f && py >= y - h * 0.5f && py <= y + h * 0.5f;
    }

    /** 更新悬停状态；被点击时返回 true。 */
    boolean update(float mx, float my, boolean clicked) {
        hovered = enabled && contains(mx, my);
        return hovered && clicked;
    }

    void clearHover() {
        hovered = false;
    }

    void render(SpriteBatch sb) {
        if (!enabled) {
            sb.setColor(BG_DISABLED);
        } else if (highlighted) {
            sb.setColor(BG_ACTIVE);
        } else if (hovered) {
            sb.setColor(BG_HOVER);
        } else {
            sb.setColor(BG_IDLE);
        }
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x - w * 0.5f, y - h * 0.5f, w, h);

        sb.setColor(enabled && (highlighted || hovered) ? BORDER_ON : BORDER_OFF);
        float line = Math.max(2.0f, 2.0f * Settings.scale);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x - w * 0.5f, y + h * 0.5f - line, w, line);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x - w * 0.5f, y - h * 0.5f, w, line);

        sb.setColor(Color.WHITE);
        FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, label == null ? "" : label, x, y,
                enabled ? Settings.CREAM_COLOR : TEXT_OFF);
    }
}
