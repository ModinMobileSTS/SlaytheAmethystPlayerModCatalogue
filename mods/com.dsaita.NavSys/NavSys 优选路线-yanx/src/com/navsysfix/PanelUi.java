package com.navsysfix;

import com.dsaita.NavSys.ui.PathInfoPanel;
import com.megacrit.cardcrawl.core.Settings;

/**
 * Geometry of the left-side NavSys route list.
 *
 * Rows are painted at y = CONTENT_Y - 30 * (index + 1); everything that must line
 * up with them goes through here so one formula owns the layout.
 *
 * The map has no extra buttons any more: whether the list is shown, whether the
 * two coloured routes are drawn and whether mark mode is on are all switches in
 * the mod's config page.
 */
public final class PanelUi {

    public static final float ROW_SPACING = 30.0f;
    public static final float ROW_HEIGHT = 30.0f;
    public static final float ROW_WIDTH = 210.0f;
    public static final int MAX_ROWS = 15;

    /** Rows actually painted last frame. */
    public static int visibleRows = 0;

    private PanelUi() {
    }

    /** The list starts hidden; the config page decides whether it is shown. */
    public static boolean hidden() {
        return !NavSysConfigPanel.showPanel;
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
}
