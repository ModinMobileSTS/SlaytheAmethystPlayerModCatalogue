package com.navsysfix.patches;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dsaita.NavSys.pathing.PathInfo;
import com.dsaita.NavSys.ui.PathInfoPanel;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.navsysfix.PanelUi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Replaces PathInfoPanel.draw().
 *
 * Behaviour changes over the original:
 *   - it always presents the two best routes instead of up to fifteen rows, so
 *     the panel keeps a stable, readable size and the two "current" options are
 *     always the ones on screen;
 *   - it draws nothing at all while the left-side UI is hidden;
 *   - it no longer filters the list by NavSys' node markers. Markers are gone
 *     with mark mode, and filtering by them could leave the list empty (or show
 *     a different route than the one drawn on the map) whenever a stale entry
 *     was still stored.
 *
 * The title, icons and number columns are reproduced exactly as before
 * (elite / rest / merchant / enemy / event), only the row selection differs.
 */
@SpirePatch2(clz = PathInfoPanel.class, method = "draw", paramtypez = {SpriteBatch.class, List.class})
public class PatchPathInfoPanelDraw {

    /** How many routes the panel keeps on screen. */
    private static final int VISIBLE_ROUTES = 2;

    private static final float ICON_SPACING = 40.0f;
    private static final float ICON_SIZE = 25.0f;
    private static final float ICON_CENTER_OFFSET = 12.5f;

    @SpirePrefixPatch
    public static SpireReturn Prefix(Object[] __args) {
        SpriteBatch sb = (SpriteBatch) __args[0];
        List<?> incoming = (List<?>) __args[1];

        PathInfoPanel.SCREEN_X = 20.0f * Settings.xScale;
        PathInfoPanel.CONTENT_X = PathInfoPanel.SCREEN_X + 5.0f;
        PathInfoPanel.CONTENT_Y = (float) Settings.HEIGHT - Settings.yScale * 200.0f;

        List<PathInfo> shown = new ArrayList<>();
        if (!PanelUi.hidden() && incoming != null) {
            List<PathInfo> all = collect(incoming);
            shown = bestRoutes(all);
            render(sb, all.size(), shown);
        }
        PanelUi.visibleRows = shown.size();
        return SpireReturn.Return(shown);
    }

    private static List<PathInfo> collect(List<?> incoming) {
        List<PathInfo> result = new ArrayList<>();
        for (Object candidate : incoming) {
            if (candidate instanceof PathInfo) {
                result.add((PathInfo) candidate);
            }
        }
        return result;
    }

    /** Best routes first, duplicates removed, at most VISIBLE_ROUTES entries. */
    private static List<PathInfo> bestRoutes(List<PathInfo> all) {
        List<PathInfo> sorted = new ArrayList<>(all);
        Collections.sort(sorted);

        List<PathInfo> unique = new ArrayList<>();
        for (PathInfo info : sorted) {
            boolean duplicate = false;
            for (PathInfo kept : unique) {
                if (kept.equals(info)) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                unique.add(info);
            }
            if (unique.size() >= VISIBLE_ROUTES) {
                break;
            }
        }
        return unique;
    }

    private static void render(SpriteBatch sb, int found, List<PathInfo> shown) {
        sb.setColor(Color.WHITE);
        String title = "NavSys - " + found + " found (max: " + VISIBLE_ROUTES + ")";
        FontHelper.renderFontLeftTopAligned(sb, FontHelper.blockInfoFont, title,
                PathInfoPanel.SCREEN_X, PathInfoPanel.CONTENT_Y, Settings.GOLD_COLOR);

        Texture[] icons = {
                ImageMaster.MAP_NODE_ELITE,
                ImageMaster.MAP_NODE_REST,
                ImageMaster.MAP_NODE_MERCHANT,
                ImageMaster.MAP_NODE_ENEMY,
                ImageMaster.MAP_NODE_EVENT
        };

        for (int i = 0; i < shown.size(); i++) {
            float top = PanelUi.rowTop(i);
            sb.setColor(Color.WHITE);
            for (int column = 0; column < icons.length; column++) {
                float x = PathInfoPanel.CONTENT_X + 5.0f + column * ICON_SPACING;
                sb.draw(icons[column], x, top - ICON_CENTER_OFFSET, ICON_SIZE, ICON_SIZE);
            }

            PathInfo info = shown.get(i);
            Color color = i == PathInfoPanel.selectedIndex ? Color.YELLOW : Settings.CREAM_COLOR;
            int[] counts = {
                    info.getEliteNodeCount(),
                    info.getRestSiteNodeCount(),
                    info.getShopNodeCount(),
                    info.getMonsterNodeCount(),
                    info.getEventNodeCount()
            };
            for (int column = 0; column < counts.length; column++) {
                float x = PathInfoPanel.CONTENT_X + column * ICON_SPACING;
                FontHelper.renderFontLeft(sb, FontHelper.blockInfoFont,
                        String.valueOf(counts[column]), x, top, color);
            }
        }
    }
}
