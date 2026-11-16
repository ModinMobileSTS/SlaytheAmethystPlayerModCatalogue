package com.navsysfix;

import com.dsaita.NavSys.ui.PathInfoPanel;
import com.megacrit.cardcrawl.core.Settings;

/**
 * Geometry of the left-side NavSys UI and its extra buttons.
 *
 * Rows are painted at y = CONTENT_Y - 30 * (index + 1); everything that must
 * line up with them goes through here so one formula owns the layout.
 */
public final class PanelUi {

    public static final float ROW_SPACING = 30.0f;
    public static final float ROW_HEIGHT = 30.0f;
    public static final float ROW_WIDTH = 210.0f;
    public static final int MAX_ROWS = 15;

    /** Rows actually painted last frame. */
    public static int visibleRows = 0;

    private static final float BTN_W = 56.0f;
    private static final float BTN_H = 26.0f;
    private static final float MARK_OFFSET_X = 320.0f;
    private static final float TOGGLE_OFFSET_X = 390.0f;
    private static final float SETTINGS_OFFSET_X = 460.0f;
    private static final float BTN_OFFSET_Y = 30.0f;

    private PanelUi() {
    }

    /** The panel starts hidden; the setting decides whether it is shown. */
    public static boolean hidden() {
        return !Config.showPanel;
    }

    public static float rowTop(int index) {
        return PathInfoPanel.CONTENT_Y - ROW_SPACING * (index + 1);
    }

    public static float rowBottom(int index) {
        return rowTop(index) - ROW_HEIGHT;
    }

    public static float rowLeft() {
        return PathInfoPanel.CONTENT_X;
    }

    public static int rowCount() {
        return hidden() ? 0 : Math.min(visibleRows, MAX_ROWS);
    }

    public static boolean insideRow(int index, float x, float y) {
        return x >= rowLeft()
                && x <= rowLeft() + ROW_WIDTH
                && y <= rowTop(index)
                && y >= rowBottom(index);
    }

    public static int rowAt(float x, float y) {
        int rows = rowCount();
        for (int i = 0; i < rows; i++) {
            if (insideRow(i, x, y)) {
                return i;
            }
        }
        return -1;
    }

    public static float markButtonX() {
        return PathInfoPanel.CONTENT_X + MARK_OFFSET_X * Settings.xScale;
    }

    /** Show/hide the left panel; always drawn so the panel stays reachable. */
    public static float toggleButtonX() {
        return PathInfoPanel.CONTENT_X + TOGGLE_OFFSET_X * Settings.xScale;
    }

    /** Opens the settings overlay. */
    public static float settingsButtonX() {
        return PathInfoPanel.CONTENT_X + SETTINGS_OFFSET_X * Settings.xScale;
    }

    public static float buttonY() {
        return PathInfoPanel.CONTENT_Y - BTN_OFFSET_Y * Settings.yScale;
    }

    public static boolean insideMarkButton(float x, float y) {
        return !hidden() && insideButton(markButtonX(), x, y);
    }

    public static boolean insideToggleButton(float x, float y) {
        return insideButton(toggleButtonX(), x, y);
    }

    public static boolean insideSettingsButton(float x, float y) {
        return insideButton(settingsButtonX(), x, y);
    }

    private static boolean insideButton(float left, float x, float y) {
        float top = buttonY();
        return x >= left
                && x <= left + BTN_W * Settings.xScale
                && y <= top
                && y >= top - BTN_H * Settings.yScale;
    }
}
