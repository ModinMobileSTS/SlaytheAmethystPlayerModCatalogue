package com.navsysfix;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dsaita.NavSys.ui.PathInfoPanel;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;

/**
 * In-game settings for this patch, opened from the [C] button next to the
 * panel toggle while the map is open.
 *
 * Labels stay ASCII on purpose: the settings are drawn with the same bitmap
 * font the NavSys panel uses, which is only guaranteed to cover Latin glyphs,
 * so Chinese labels could turn into empty boxes.
 */
public final class SettingsOverlay {

    public static boolean open = false;

    private static final float ROW_H = 36.0f;
    private static final float WIDTH = 430.0f;
    private static final int ROWS = 10;

    /** Used for an "ON" value; the build has no Settings.GREEN_COLOR. */
    private static final Color ON_COLOR = new Color(0.45f, 0.9f, 0.45f, 1.0f);

    private static final String[] LABELS = {            "Panel (left list)",
            "Safe route (gold)",
            "Aggro route (red)",
            "Safe R",
            "Safe G",
            "Safe B",
            "Aggro R",
            "Aggro G",
            "Aggro B",
            "Close"
    };

    private SettingsOverlay() {
    }

    private static float rowH() {
        return ROW_H * Settings.yScale;
    }

    private static float width() {
        return WIDTH * Settings.xScale;
    }

    private static float left() {
        return PathInfoPanel.CONTENT_X;
    }

    private static float top() {
        return PathInfoPanel.CONTENT_Y;
    }

    private static float rowTop(int index) {
        return top() - rowH() * index;
    }

    private static float rowBottom(int index) {
        return rowTop(index) - rowH();
    }

    public static boolean covers(float x, float y) {
        if (!open) {
            return false;
        }
        float l = left();
        return x >= l && x <= l + width()
                && y <= top() && y >= rowBottom(ROWS - 1);
    }

    /** Returns true when the tap belonged to the overlay. */
    public static boolean handleTap(float x, float y) {
        if (!covers(x, y)) {
            return false;
        }
        int row = rowAt(y);
        if (row < 0) {
            return true;
        }
        boolean up = isPlus(x);
        switch (row) {
            case 0:
                Config.showPanel = !Config.showPanel;
                break;
            case 1:
                Config.showSafeRoute = !Config.showSafeRoute;
                break;
            case 2:
                Config.showAggroRoute = !Config.showAggroRoute;
                break;
            case 3:
                Config.safeR = Config.stepChannel(Config.safeR, up);
                break;
            case 4:
                Config.safeG = Config.stepChannel(Config.safeG, up);
                break;
            case 5:
                Config.safeB = Config.stepChannel(Config.safeB, up);
                break;
            case 6:
                Config.aggroR = Config.stepChannel(Config.aggroR, up);
                break;
            case 7:
                Config.aggroG = Config.stepChannel(Config.aggroG, up);
                break;
            case 8:
                Config.aggroB = Config.stepChannel(Config.aggroB, up);
                break;
            default:
                open = false;
                break;
        }
        RoutePainter.invalidate();
        Config.save();
        return true;
    }

    public static void draw(SpriteBatch sb) {
        if (!open) {
            return;
        }
        float l = left();
        float w = width();
        float bottom = rowBottom(ROWS - 1);
        float h = rowH();

        sb.setColor(new Color(0.04f, 0.03f, 0.02f, 0.94f));
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, l, bottom, w, top() - bottom);
        sb.setColor(new Color(0.72f, 0.57f, 0.3f, 0.95f));
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, l, top() - 2.0f * Settings.yScale, w, 2.0f * Settings.yScale);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, l, bottom, w, 2.0f * Settings.yScale);

        float pad = 12.0f * Settings.xScale;
        for (int i = 0; i < ROWS; i++) {
            if (i > 0) {
                sb.setColor(new Color(0.72f, 0.57f, 0.3f, 0.25f));
                sb.draw(ImageMaster.WHITE_SQUARE_IMG, l, rowBottom(i) - 1.0f, w, 1.0f);
            }
            float textY = rowTop(i) - h * 0.74f;
            FontHelper.renderFontLeftTopAligned(sb, FontHelper.blockInfoFont, LABELS[i],
                    l + pad, textY, Settings.CREAM_COLOR);
            String value = valueFor(i);
            if (value != null) {
                FontHelper.renderFontRightTopAligned(sb, FontHelper.blockInfoFont, value,
                        l + w - pad, textY, row9Style(i));
            }
        }
        sb.setColor(Color.WHITE);
    }

    private static Color row9Style(int row) {
        if (row == 9) {
            return Settings.GOLD_COLOR;
        }
        if (row <= 2) {
            boolean on = row == 0 ? Config.showPanel
                    : row == 1 ? Config.showSafeRoute : Config.showAggroRoute;
            return on ? ON_COLOR : Settings.CREAM_COLOR;
        }
        return Settings.CREAM_COLOR;
    }

    private static String valueFor(int row) {
        switch (row) {
            case 0:
                return Config.showPanel ? "ON" : "OFF";
            case 1:
                return Config.showSafeRoute ? "ON" : "OFF";
            case 2:
                return Config.showAggroRoute ? "ON" : "OFF";
            case 3:
                return "- " + Config.safeR + " +";
            case 4:
                return "- " + Config.safeG + " +";
            case 5:
                return "- " + Config.safeB + " +";
            case 6:
                return "- " + Config.aggroR + " +";
            case 7:
                return "- " + Config.aggroG + " +";
            case 8:
                return "- " + Config.aggroB + " +";
            default:
                return null;
        }
    }

    private static int rowAt(float y) {
        for (int i = 0; i < ROWS; i++) {
            if (y <= rowTop(i) && y >= rowBottom(i)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isPlus(float x) {
        float right = left() + width();
        float minusStart = right - 140.0f * Settings.xScale;
        float minusEnd = right - 70.0f * Settings.xScale;
        return !(x >= minusStart && x < minusEnd);
    }
}
