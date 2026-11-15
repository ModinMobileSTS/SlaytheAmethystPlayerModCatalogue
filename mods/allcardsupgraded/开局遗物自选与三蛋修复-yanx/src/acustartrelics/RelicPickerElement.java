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
 * mod 设置面板里的遗物图标网格：上方一排稀有度筛选，中间 12 列 × 4 行图标，
 * 下方翻页/清空，点击切换选中。所有坐标都是「设计坐标」，渲染与判定时乘 Settings.scale，
 * 与 BaseMod 自带控件保持一致。
 */
public class RelicPickerElement implements IUIElement {

    private static final int COLS = 12;
    private static final int PAGE_SIZE = COLS * 4;

    private static final float ICON = 70f;
    private static final float SPACING = 78f;
    private static final float GRID_LEFT = 340f;
    private static final float GRID_TOP = 712f;

    private static final float FILTER_Y = 760f;
    private static final float FILTER_W = 92f;
    private static final float FILTER_H = 44f;
    private static final float FILTER_GAP = 4f;

    private static final float BTN_W = 120f;
    private static final float BTN_H = 50f;
    private static final float BTN_Y = 372f;
    private static final float PREV_X = 330f;
    private static final float NEXT_X = 470f;
    private static final float CLEAR_X = 610f;

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
    private final ModLabel countLabel;
    private final ModLabel hoverLabel;

    private final List<AbstractRelic> filtered = new ArrayList<AbstractRelic>();
    private final List<AbstractRelic> pageRelics = new ArrayList<AbstractRelic>();

    private final Color selectedFill = new Color(0.26f, 0.92f, 0.42f, 0.26f);
    private final Color selectedBorder = new Color(0.36f, 1f, 0.5f, 0.95f);
    private final Color hoverFill = new Color(1f, 1f, 1f, 0.16f);
    private final Color buttonColor = new Color(0.13f, 0.15f, 0.19f, 0.90f);
    private final Color buttonActive = new Color(0.24f, 0.44f, 0.30f, 0.95f);

    private int filter = 0;
    private int page = 0;
    private int pages = 1;
    private AbstractRelic hovered;
    private int pressedEntry = -1;
    private int pressedButton = -1;

    public RelicPickerElement(ModPanel panel) {
        pageLabel = new ModLabel("", 750f, 388f, Color.LIGHT_GRAY, panel, me -> {
        });
        countLabel = new ModLabel("", 330f, 336f, Color.LIGHT_GRAY, panel, me -> {
        });
        hoverLabel = new ModLabel("", 330f, 300f, Color.GOLD, panel, me -> {
        });
        panel.addUIElement(pageLabel);
        panel.addUIElement(countLabel);
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
        countLabel.text = Texts.selected(StartRelicSettings.getSelectedCount(), filtered.size());
        hoverLabel.text = hovered == null ? "" : hovered.name;
    }

    private static float cellX(int index) {
        return GRID_LEFT + (index % COLS) * SPACING;
    }

    private static float cellY(int index) {
        return GRID_TOP - (index / COLS) * SPACING;
    }

    private static float filterX(int index) {
        return 330f + index * (FILTER_W + FILTER_GAP);
    }

    private static boolean inRect(float mouseX, float mouseY, float x, float y, float w, float h) {
        float s = Settings.scale;
        return mouseX >= x * s && mouseX <= (x + w) * s && mouseY >= y * s && mouseY <= (y + h) * s;
    }

    @Override
    public void update() {
        float mx = InputHelper.mX;
        float my = InputHelper.mY;

        int hoverEntry = -1;
        for (int i = 0; i < pageRelics.size(); i++) {
            if (inRect(mx, my, cellX(i) - ICON / 2f, cellY(i) - ICON / 2f, ICON, ICON)) {
                hoverEntry = i;
                break;
            }
        }
        hovered = hoverEntry >= 0 ? pageRelics.get(hoverEntry) : null;

        int hoverButton = -1;
        for (int i = 0; i < Texts.FILTERS.length; i++) {
            if (inRect(mx, my, filterX(i), FILTER_Y, FILTER_W, FILTER_H)) {
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
        float size = ICON * s;

        for (int i = 0; i < Texts.FILTERS.length; i++) {
            boolean active = i == filter;
            drawPlate(sb, filterX(i), FILTER_Y, FILTER_W, FILTER_H, active ? buttonActive : buttonColor);
            FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, Texts.FILTERS[i],
                    (filterX(i) + FILTER_W / 2f) * s, (FILTER_Y + FILTER_H / 2f) * s,
                    active ? Color.WHITE : Color.LIGHT_GRAY);
        }

        for (int i = 0; i < pageRelics.size(); i++) {
            AbstractRelic relic = pageRelics.get(i);
            float x = (cellX(i) - ICON / 2f) * s;
            float y = (cellY(i) - ICON / 2f) * s;

            boolean selected = StartRelicSettings.isSelected(relic.relicId);
            if (selected) {
                sb.setColor(selectedFill);
                sb.draw(ImageMaster.WHITE_SQUARE_IMG, x, y, size, size);
            }
            if (relic == hovered) {
                sb.setColor(hoverFill);
                sb.draw(ImageMaster.WHITE_SQUARE_IMG, x, y, size, size);
            }
            Texture img = relic.img;
            if (img != null) {
                sb.setColor(Color.WHITE);
                sb.draw(img, x, y, size, size);
            }
            if (selected) {
                drawBorder(sb, x, y, size, selectedBorder, 2.5f * s);
            }
        }

        drawButton(sb, PREV_X, Texts.PREV);
        drawButton(sb, NEXT_X, Texts.NEXT);
        drawButton(sb, CLEAR_X, Texts.CLEAR);

        sb.setColor(Color.WHITE);
    }

    private void drawPlate(SpriteBatch sb, float x, float y, float w, float h, Color color) {
        float s = Settings.scale;
        sb.setColor(color);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x * s, y * s, w * s, h * s);
    }

    private void drawButton(SpriteBatch sb, float x, String text) {
        float s = Settings.scale;
        drawPlate(sb, x, BTN_Y, BTN_W, BTN_H, buttonColor);
        FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, text,
                (x + BTN_W / 2f) * s, (BTN_Y + BTN_H / 2f) * s, Color.WHITE);
    }

    private void drawBorder(SpriteBatch sb, float x, float y, float size, Color color, float thickness) {
        sb.setColor(color);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x, y, size, thickness);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x, y + size - thickness, size, thickness);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x, y, thickness, size);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x + size - thickness, y, thickness, size);
    }

    @Override
    public int renderLayer() {
        return 1;
    }

    @Override
    public int updateOrder() {
        return 1;
    }
}
