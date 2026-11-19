package acustartrelics;

import basemod.IUIElement;
import basemod.ModLabel;
import basemod.ModPanel;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.relics.AbstractRelic;

import java.util.ArrayList;
import java.util.List;

/**
 * mod 设置面板里的遗物图标网格。
 *
 * 为了不受 BaseMod 背景图装饰边框的影响，这个元素会先在 BaseMod 面板矩形内画一层自己的深色底板
 * （含 1px 描边与标题分隔线），标题、筛选、图标、按钮、说明文字全部落在这一层底板上，
 * 因此文字与背景始终是一套视觉，不会出现「文字浮在别人的边框上」。
 *
 * 所有坐标都是「面板图片内的像素」（见 PanelLayout），y 向上。
 */
public class RelicPickerElement implements IUIElement {

    private static final int COLS = 12;
    private static final int ROWS = 4;
    private static final int PAGE_SIZE = COLS * ROWS;

    private static final float CELL = 84f;
    private static final float ART_SCALE = 1.4f;
    private static final float SPACING = 106f;
    private static final float GRID_LEFT = 48f;
    private static final float GRID_X = GRID_LEFT + CELL / 2f;
    private static final float GRID_TOP = 530f;
    private static final float ROW_STEP = 100f;

    private static final float FILTER_X = GRID_LEFT;
    private static final float FILTER_Y = 600f;
    private static final float FILTER_W = 110f;
    private static final float FILTER_H = 46f;
    private static final float FILTER_STEP = 118f;

    private static final float BTN_W = 150f;
    private static final float BTN_H = 50f;
    private static final float BTN_Y = 56f;
    private static final float PREV_X = GRID_LEFT;
    private static final float NEXT_X = 218f;
    private static final float CLEAR_X = 388f;
    private static final float PAGE_X = 558f;

    private static final float STATUS_Y = 130f;

    /** 底板（相对面板图片）。 */
    private static final float SURFACE_X = 28f;
    private static final float SURFACE_Y = 28f;
    private static final float SURFACE_W = 1308f;
    private static final float SURFACE_H = 704f;
    private static final float DIVIDER_Y = 682f;

    private static final int BTN_PREV = 0;
    private static final int BTN_NEXT = 1;
    private static final int BTN_CLEAR = 2;
    private static final int BTN_FILTER_BASE = 100;

    private static final AbstractRelic.RelicTier[] TIERS = new AbstractRelic.RelicTier[]{
            null,
            AbstractRelic.RelicTier.STARTER,
            AbstractRelic.RelicTier.COMMON,
            AbstractRelic.RelicTier.UNCOMMON,
            AbstractRelic.RelicTier.RARE,
            AbstractRelic.RelicTier.SHOP,
            AbstractRelic.RelicTier.BOSS,
            AbstractRelic.RelicTier.SPECIAL
    };

    private final ModLabel pageLabel;
    private final ModLabel statusLabel;
    private final ModLabel hoverLabel;

    private final List<AbstractRelic> filtered = new ArrayList<AbstractRelic>();
    private final List<AbstractRelic> pageRelics = new ArrayList<AbstractRelic>();

    private final Color surfaceColor = new Color(0.07f, 0.10f, 0.11f, 0.94f);
    private final Color surfaceBorder = new Color(0.22f, 0.55f, 0.57f, 0.90f);
    private final Color dividerColor = new Color(1f, 1f, 1f, 0.14f);
    private final Color selectedFill = new Color(0.26f, 0.92f, 0.42f, 0.30f);
    private final Color selectedBorder = new Color(0.42f, 1f, 0.55f, 0.95f);
    private final Color hoverFill = new Color(1f, 1f, 1f, 0.18f);
    private final Color buttonColor = new Color(0.16f, 0.21f, 0.23f, 0.95f);
    private final Color buttonActive = new Color(0.20f, 0.52f, 0.42f, 0.95f);

    private int filter = 0;
    private int page = 0;
    private int pages = 1;
    private AbstractRelic hovered;
    private int pressedEntry = -1;
    private int pressedButton = -1;

    public RelicPickerElement(ModPanel panel) {
        pageLabel = new ModLabel("", PanelLayout.x(PAGE_X), PanelLayout.y(BTN_Y + 12f), Color.LIGHT_GRAY, panel, me -> {
        });
        statusLabel = new ModLabel("", PanelLayout.x(GRID_LEFT), PanelLayout.y(STATUS_Y), Color.LIGHT_GRAY, panel, me -> {
        });
        hoverLabel = new ModLabel("", PanelLayout.x(700f), PanelLayout.y(690f), Color.GOLD, panel, me -> {
        });
        panel.addUIElement(pageLabel);
        panel.addUIElement(statusLabel);
        panel.addUIElement(hoverLabel);
        rebuildFilter();
    }

    private void rebuildFilter() {
        AbstractRelic.RelicTier tier = TIERS[Math.max(0, Math.min(TIERS.length - 1, filter))];
        filtered.clear();
        for (AbstractRelic relic : StartRelicSettings.getCatalog()) {
            if (tier == null || relic.tier == tier) {
                filtered.add(relic);
            }
        }
        page = 0;
        rebuildPage();
    }

    private void rebuildPage() {
        pages = Math.max(1, (filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page < 0) {
            page = 0;
        }
        if (page >= pages) {
            page = pages - 1;
        }
        pageRelics.clear();
        int start = page * PAGE_SIZE;
        int end = Math.min(filtered.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            pageRelics.add(filtered.get(i));
        }
        refreshTexts();
    }

    private void refreshTexts() {
        pageLabel.text = Texts.page(page + 1, pages);
        statusLabel.text = Texts.selected(StartRelicSettings.getSelectedCount(), filtered.size());
        hoverLabel.text = hovered == null ? "" : hovered.name;
    }

    private static float cellImgX(int index) {
        return GRID_X + (index % COLS) * SPACING;
    }

    private static float cellImgY(int index) {
        return GRID_TOP - (index / COLS) * ROW_STEP;
    }

    private static float filterImgX(int index) {
        return FILTER_X + index * FILTER_STEP;
    }

    private static boolean inRect(float mouseX, float mouseY, float imgX, float imgY, float w, float h) {
        float s = Settings.scale;
        float left = PanelLayout.x(imgX) * s;
        float bottom = PanelLayout.y(imgY) * s;
        return mouseX >= left && mouseX <= left + w * s && mouseY >= bottom && mouseY <= bottom + h * s;
    }

    @Override
    public void update() {
        float mx = InputHelper.mX;
        float my = InputHelper.mY;

        int hoverEntry = -1;
        for (int i = 0; i < pageRelics.size(); i++) {
            if (inRect(mx, my, cellImgX(i) - CELL / 2f, cellImgY(i) - CELL / 2f, CELL, CELL)) {
                hoverEntry = i;
                break;
            }
        }
        hovered = hoverEntry >= 0 ? pageRelics.get(hoverEntry) : null;

        int hoverButton = -1;
        for (int i = 0; i < Texts.FILTERS.length; i++) {
            if (inRect(mx, my, filterImgX(i), FILTER_Y, FILTER_W, FILTER_H)) {
                hoverButton = BTN_FILTER_BASE + i;
                break;
            }
        }
        if (hoverButton < 0) {
            if (inRect(mx, my, PREV_X, BTN_Y, BTN_W, BTN_H)) {
                hoverButton = BTN_PREV;
            } else if (inRect(mx, my, NEXT_X, BTN_Y, BTN_W, BTN_H)) {
                hoverButton = BTN_NEXT;
            } else if (inRect(mx, my, CLEAR_X, BTN_Y, BTN_W, BTN_H)) {
                hoverButton = BTN_CLEAR;
            }
        }

        if (InputHelper.justClickedLeft) {
            pressedEntry = hoverEntry;
            pressedButton = hoverButton;
        }
        if (InputHelper.justReleasedClickLeft) {
            if (pressedEntry >= 0 && pressedEntry == hoverEntry) {
                onEntryClicked(pressedEntry);
            } else if (pressedButton >= 0 && pressedButton == hoverButton) {
                onButtonClicked(pressedButton);
            }
            pressedEntry = -1;
            pressedButton = -1;
        }

        refreshTexts();
    }

    private void onEntryClicked(int index) {
        if (index < 0 || index >= pageRelics.size()) {
            return;
        }
        StartRelicSettings.toggle(pageRelics.get(index).relicId);
        CardCrawlGame.sound.playA("UI_CLICK_1", -0.1f);
    }

    private void onButtonClicked(int button) {
        if (button >= BTN_FILTER_BASE) {
            int index = button - BTN_FILTER_BASE;
            if (index != filter) {
                filter = index;
                CardCrawlGame.sound.playA("UI_CLICK_1", -0.1f);
                rebuildFilter();
            }
            return;
        }
        if (button == BTN_PREV) {
            if (page > 0) {
                page--;
                CardCrawlGame.sound.playA("UI_CLICK_1", -0.1f);
                rebuildPage();
            }
        } else if (button == BTN_NEXT) {
            if (page < pages - 1) {
                page++;
                CardCrawlGame.sound.playA("UI_CLICK_1", -0.1f);
                rebuildPage();
            }
        } else if (button == BTN_CLEAR) {
            StartRelicSettings.clearSelection();
            CardCrawlGame.sound.playA("UI_CLICK_1", -0.1f);
            rebuildPage();
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        float s = Settings.scale;

        // 自己的底板 + 描边 + 标题分隔线：文字与图标都落在这层上
        drawPlate(sb, SURFACE_X, SURFACE_Y, SURFACE_W, SURFACE_H, surfaceColor);
        drawBorder(sb, PanelLayout.x(SURFACE_X) * s, PanelLayout.y(SURFACE_Y) * s,
                SURFACE_W * s, SURFACE_H * s, surfaceBorder, 1.5f * s);
        drawPlate(sb, GRID_LEFT, DIVIDER_Y, SURFACE_W - 40f, 2f, dividerColor);

        for (int i = 0; i < Texts.FILTERS.length; i++) {
            boolean active = i == filter;
            drawPlate(sb, filterImgX(i), FILTER_Y, FILTER_W, FILTER_H, active ? buttonActive : buttonColor);
            FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, Texts.FILTERS[i],
                    (PanelLayout.x(filterImgX(i)) + FILTER_W / 2f) * s,
                    (PanelLayout.y(FILTER_Y) + FILTER_H / 2f) * s,
                    active ? Color.WHITE : Color.LIGHT_GRAY);
        }

        float art = CELL * ART_SCALE * s;
        float cellSize = CELL * s;
        for (int i = 0; i < pageRelics.size(); i++) {
            AbstractRelic relic = pageRelics.get(i);
            float cx = PanelLayout.x(cellImgX(i)) * s;
            float cy = PanelLayout.y(cellImgY(i)) * s;
            boolean selected = StartRelicSettings.isSelected(relic.relicId);

            if (selected) {
                sb.setColor(selectedFill);
                sb.draw(ImageMaster.WHITE_SQUARE_IMG, cx - cellSize / 2f, cy - cellSize / 2f, cellSize, cellSize);
            }
            if (relic == hovered) {
                sb.setColor(hoverFill);
                sb.draw(ImageMaster.WHITE_SQUARE_IMG, cx - cellSize / 2f, cy - cellSize / 2f, cellSize, cellSize);
            }
            Texture img = relic.img;
            if (img != null) {
                sb.setColor(Color.WHITE);
                sb.draw(img, cx - art / 2f, cy - art / 2f, art, art);
            }
            if (selected) {
                drawBorder(sb, cx - cellSize / 2f, cy - cellSize / 2f, cellSize, cellSize, selectedBorder, 2.5f * s);
            }
        }

        drawButton(sb, PREV_X, Texts.PREV);
        drawButton(sb, NEXT_X, Texts.NEXT);
        drawButton(sb, CLEAR_X, Texts.CLEAR);

        sb.setColor(Color.WHITE);
    }

    private void drawPlate(SpriteBatch sb, float imgX, float imgY, float w, float h, Color color) {
        float s = Settings.scale;
        sb.setColor(color);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, PanelLayout.x(imgX) * s, PanelLayout.y(imgY) * s, w * s, h * s);
    }

    private void drawButton(SpriteBatch sb, float imgX, String text) {
        float s = Settings.scale;
        drawPlate(sb, imgX, BTN_Y, BTN_W, BTN_H, buttonColor);
        FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, text,
                (PanelLayout.x(imgX) + BTN_W / 2f) * s, (PanelLayout.y(BTN_Y) + BTN_H / 2f) * s, Color.WHITE);
    }

    private void drawBorder(SpriteBatch sb, float x, float y, float w, float h, Color color, float thickness) {
        sb.setColor(color);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x, y, w, thickness);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x, y + h - thickness, w, thickness);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x, y, thickness, h);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x + w - thickness, y, thickness, h);
    }

    @Override
    public int renderLayer() {
        return ModPanel.MIDDLE_LAYER;
    }

    @Override
    public int updateOrder() {
        return ModPanel.DEFAULT_UPDATE;
    }
}
