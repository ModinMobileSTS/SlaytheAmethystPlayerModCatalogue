package com.navsysfix;

import com.dsaita.NavSys.ui.PathInfoPanel;
import com.megacrit.cardcrawl.core.Settings;

/**
 * Geometry of the NavSys result panel.
 *
 * The panel rows are painted by PathInfoPanel.draw at
 *   y = CONTENT_Y - 30 * (index + 1)
 * which is the top of the text line (y grows upwards). Everything that has to
 * line up with those rows - the panel hitboxes, the extra mark-mode button,
 * touch hit testing - goes through this class so one formula owns the layout.
 */
public final class PanelUi {

    /** Vertical distance between two result rows, as used by PathInfoPanel.draw. */
    public static final float ROW_SPACING = 30.0f;
    /** Height of one clickable row band; equal to ROW_SPACING so bands tile. */
    public static final float ROW_HEIGHT = 30.0f;
    /** Width of one clickable row band (five number columns plus padding). */
    public static final float ROW_WIDTH = 210.0f;
    /** Number of result rows the panel can show (PathInfoPanel.MAX_RESULTS). */
    public static final int MAX_ROWS = 15;

    private static final float MARK_BTN_W = 52.0f;
    private static final float MARK_BTN_H = 26.0f;
    private static final float MARK_BTN_OFFSET_X = 320.0f;
    private static final float MARK_BTN_OFFSET_Y = 30.0f;

    private PanelUi() {
    }

    /** Top edge of row {@code index} in world coordinates. */
    public static float rowTop(int index) {
        return PathInfoPanel.CONTENT_Y - ROW_SPACING * (index + 1);
    }

    /** Bottom edge of row {@code index} in world coordinates. */
    public static float rowBottom(int index) {
        return rowTop(index) - ROW_HEIGHT;
    }

    /** Left edge of the result columns. */
    public static float rowLeft() {
        return PathInfoPanel.CONTENT_X;
    }

    public static boolean insideRow(int index, float x, float y) {
        return x >= rowLeft()
                && x <= rowLeft() + ROW_WIDTH
                && y <= rowTop(index)
                && y >= rowBottom(index);
    }

    /** Index of the result row under the pointer, or -1 when outside. */
    public static int rowAt(float x, float y) {
        for (int i = 0; i < MAX_ROWS; i++) {
            if (insideRow(i, x, y)) {
                return i;
            }
        }
        return -1;
    }

    /** Left edge of the mark-mode button, next to the eye button. */
    public static float markButtonX() {
        return PathInfoPanel.CONTENT_X + MARK_BTN_OFFSET_X * Settings.xScale;
    }

    /** Top edge of the mark-mode button, aligned with the eye button label. */
    public static float markButtonY() {
        return PathInfoPanel.CONTENT_Y - MARK_BTN_OFFSET_Y * Settings.yScale;
    }

    public static boolean insideMarkButton(float x, float y) {
        float bx = markButtonX();
        float by = markButtonY();
        return x >= bx
                && x <= bx + MARK_BTN_W * Settings.xScale
                && y <= by
                && y >= by - MARK_BTN_H * Settings.yScale;
    }
}
