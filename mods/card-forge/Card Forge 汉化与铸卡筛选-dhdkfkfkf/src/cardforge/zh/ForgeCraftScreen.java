package cardforge.zh;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.eternal0.slay.the.spire.mods.ui.Screen;
import com.eternal0.slay.the.spire.mods.ui.ScreenManager;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * 雷神之锤的铸卡界面重写版。
 *
 * <p>原界面每帧维护滚动条、对全部卡牌做悬停判定并插值卡牌位置，卡牌数量一多就会明显抖动；
 * 这里改为固定格位 + 直接赋值坐标，不再有滚动条与插值，因此不会抖动。
 * 同时新增：按角色卡池筛选（默认当前角色）、按名字搜索、触摸拖动浏览。</p>
 */
public final class ForgeCraftScreen extends Screen {
    private static final Color BACKGROUND = new Color(0.05f, 0.05f, 0.08f, 0.96f);
    private static final Color PICKER_BG = new Color(0.03f, 0.03f, 0.05f, 0.97f);
    private static final Color SELECT_FRAME = new Color(1.0f, 0.82f, 0.30f, 0.85f);
    private static final Color HOVER_FRAME = new Color(0.95f, 0.95f, 1.0f, 0.55f);
    private static final Color TRACK_COLOR = new Color(1.0f, 1.0f, 1.0f, 0.12f);
    private static final Color THUMB_COLOR = new Color(1.0f, 0.92f, 0.65f, 0.55f);

    private static ForgeCraftScreen openInstance;

    private final List<AbstractCard> library = new ArrayList<>();
    private final List<AbstractCard> filtered = new ArrayList<>();
    private final List<AbstractCard> selected = new ArrayList<>();
    private final List<CardPool> pools = new ArrayList<>();
    private final List<TextButton> pickerButtons = new ArrayList<>();

    private int poolIndex;
    private String query = "";
    private boolean pickerOpen;

    private boolean typing;
    private InputProcessor savedProcessor;
    private SearchTextInput searchInput;

    private float margin;
    private float buttonH;
    private float labelY;
    private float poolRowY;
    private float topRowEndX;
    private float gridTop;
    private float gridBottom;
    private float bottomY;
    private float scroll;
    private float totalHeight;
    private float colStep;
    private float rowStep;
    private float gridCenterX;
    private float cardScale;
    private int cols;

    private TextButton poolButton;
    private TextButton searchButton;
    private TextButton clearButton;
    private TextButton confirmButton;
    private TextButton cancelButton;

    private boolean pressing;
    private boolean dragging;
    private AbstractCard pressedCard;
    private AbstractCard hoveredCard;
    private float pressY;
    private float lastY;

    private ForgeCraftScreen() {
        collectCards();
        collectPools();
        computeLayout();
        createButtons();
        applyFilter();
        refreshButtonLabels();
        if (AbstractDungeon.overlayMenu != null) {
            AbstractDungeon.overlayMenu.cancelButton.hide();
        }
        if (AbstractDungeon.dynamicBanner != null) {
            AbstractDungeon.dynamicBanner.hide();
        }
    }

    /** 打开铸卡界面；已经打开时忽略重复触发。 */
    public static void openScreen() {
        if (openInstance != null && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.NONE) {
            return;
        }
        openInstance = null;
        ForgeCraftScreen screen = new ForgeCraftScreen();
        ScreenManager.INSTANCE.show(screen);
        openInstance = screen;
    }

    // ------------------------------------------------------------------ 数据

    private void collectCards() {
        HashSet<String> seen = new HashSet<>();
        ArrayList<AbstractCard> all = CardLibrary.getAllCards();
        if (all == null) {
            return;
        }
        for (AbstractCard card : all) {
            if (card == null || card.cardID == null || !seen.add(card.cardID)) {
                continue;
            }
            AbstractCard copy = copyCard(card);
            if (copy != null) {
                library.add(copy);
            }
        }
        library.sort((a, b) -> {
            int byColor = a.color.compareTo(b.color);
            if (byColor != 0) {
                return byColor;
            }
            String an = a.name == null ? "" : a.name;
            String bn = b.name == null ? "" : b.name;
            return an.compareTo(bn);
        });
    }

    private static AbstractCard copyCard(AbstractCard card) {
        try {
            AbstractCard copy = card.makeCopy();
            if (copy != null) {
                return copy;
            }
        } catch (Throwable ignored) {
        }
        try {
            return card.makeStatEquivalentCopy();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void collectPools() {
        HashSet<AbstractCard.CardColor> used = new HashSet<>();

        AbstractCard.CardColor currentColor = null;
        AbstractPlayer player = AbstractDungeon.player;
        if (player != null) {
            try {
                currentColor = player.getCardColor();
            } catch (Throwable ignored) {
            }
        }
        if (currentColor != null && hasCards(currentColor)) {
            pools.add(new CardPool(Texts.t("当前角色", "Current"), currentColor));
            used.add(currentColor);
        }

        if (CardCrawlGame.characterManager != null) {
            ArrayList<AbstractPlayer> characters = CardCrawlGame.characterManager.getAllCharacters();
            if (characters != null) {
                for (AbstractPlayer character : characters) {
                    if (character == null) {
                        continue;
                    }
                    AbstractCard.CardColor color;
                    try {
                        color = character.getCardColor();
                    } catch (Throwable t) {
                        continue;
                    }
                    if (color == null || used.contains(color) || !hasCards(color)) {
                        continue;
                    }
                    String label = null;
                    try {
                        label = character.getLocalizedCharacterName();
                    } catch (Throwable ignored) {
                    }
                    if (label == null || label.trim().isEmpty()) {
                        label = color.name();
                    }
                    pools.add(new CardPool(shorten(label), color));
                    used.add(color);
                }
            }
        }

        if (hasCards(AbstractCard.CardColor.COLORLESS)) {
            pools.add(new CardPool(Texts.t("无色卡", "Colorless"), AbstractCard.CardColor.COLORLESS));
            used.add(AbstractCard.CardColor.COLORLESS);
        }
        if (hasCards(AbstractCard.CardColor.CURSE)) {
            pools.add(new CardPool(Texts.t("诅咒", "Curses"), AbstractCard.CardColor.CURSE));
            used.add(AbstractCard.CardColor.CURSE);
        }
        for (AbstractCard.CardColor color : AbstractCard.CardColor.values()) {
            if (used.contains(color) || !hasCards(color)) {
                continue;
            }
            pools.add(new CardPool(shorten(color.name()), color));
        }
        pools.add(new CardPool(Texts.t("全部卡牌", "All cards"), null));
    }

    private boolean hasCards(AbstractCard.CardColor color) {
        for (AbstractCard card : library) {
            if (card.color == color) {
                return true;
            }
        }
        return false;
    }

    private static String shorten(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() > 10 ? trimmed.substring(0, 9) + "…" : trimmed;
    }

    // ------------------------------------------------------------------ 布局

    private void computeLayout() {
        float width = Settings.WIDTH;
        float height = Settings.HEIGHT;
        float scale = Settings.scale;
        margin = Math.max(28.0f * scale, width * 0.02f);
        buttonH = Math.max(52.0f * scale, height * 0.058f);
        labelY = height - Math.max(34.0f * scale, height * 0.042f);
        poolRowY = height - Math.max(74.0f * scale, height * 0.088f) - buttonH * 0.5f;
        gridTop = poolRowY - buttonH * 0.5f - Math.max(18.0f * scale, height * 0.024f);
        bottomY = Math.max(buttonH * 0.5f + 14.0f * scale, height * 0.062f);
        gridBottom = bottomY + buttonH * 0.5f + Math.max(18.0f * scale, height * 0.024f);

        float availW = width - 2.0f * margin;
        float availH = Math.max(140.0f * scale, gridTop - gridBottom);
        float gap = 12.0f * scale;
        float byHeight = (availH / 2.0f) * 0.90f / AbstractCard.IMG_HEIGHT;
        float byWidth = (availW / 6.0f) * 0.90f / AbstractCard.IMG_WIDTH;
        cardScale = Math.max(0.30f, Math.min(0.72f, Math.min(byHeight, byWidth)));
        colStep = AbstractCard.IMG_WIDTH * cardScale + gap;
        rowStep = AbstractCard.IMG_HEIGHT * cardScale + gap;
        cols = Math.max(4, Math.min(10, (int) Math.floor(availW / colStep)));
        gridCenterX = width * 0.5f;
    }

    private void createButtons() {
        float availW = Settings.WIDTH - 2.0f * margin;
        float gap = 14.0f * Settings.scale;
        float w1 = availW * 0.40f;
        float w2 = availW * 0.15f;
        float w3 = availW * 0.14f;
        float x = margin;
        poolButton = new TextButton("", x + w1 * 0.5f, poolRowY, w1, buttonH);
        x += w1 + gap;
        searchButton = new TextButton(Texts.t("搜索", "Search"), x + w2 * 0.5f, poolRowY, w2, buttonH);
        x += w2 + gap;
        clearButton = new TextButton(Texts.t("清除", "Clear"), x + w3 * 0.5f, poolRowY, w3, buttonH);
        topRowEndX = x + w3;

        float wc = availW * 0.20f;
        float wok = availW * 0.26f;
        cancelButton = new TextButton(Texts.t("取消", "Cancel"), margin + wc * 0.5f, bottomY, wc, buttonH);
        confirmButton = new TextButton("", Settings.WIDTH - margin - wok * 0.5f, bottomY, wok, buttonH);
    }

    private void refreshButtonLabels() {
        CardPool pool = pools.isEmpty() ? null : pools.get(poolIndex);
        String poolName = pool == null ? "" : pool.label;
        poolButton.setLabel(Texts.t("卡池：", "Pool: ") + poolName);
        confirmButton.setLabel(Texts.t("确认铸造", "Craft") + " (" + selected.size() + ")");
        confirmButton.enabled = !selected.isEmpty();
        clearButton.enabled = !query.isEmpty();
        searchButton.setLabel(typing ? Texts.t("完成", "Done") : Texts.t("搜索", "Search"));
    }

    private void openPicker() {
        pickerOpen = true;
        pickerButtons.clear();
        int count = pools.size();
        if (count == 0) {
            return;
        }
        float scale = Settings.scale;
        int pickerCols = count <= 4 ? 2 : (count <= 9 ? 3 : 4);
        int rows = (count + pickerCols - 1) / pickerCols;
        float bh = Math.min(70.0f * scale, (Settings.HEIGHT * 0.60f) / rows);
        float bw = Math.min(460.0f * scale, (Settings.WIDTH * 0.86f) / pickerCols - 16.0f * scale);
        float gapX = 12.0f * scale;
        float gapY = 10.0f * scale;
        float totalW = pickerCols * bw + (pickerCols - 1) * gapX;
        float totalH = rows * bh + (rows - 1) * gapY;
        float startX = Settings.WIDTH * 0.5f - totalW * 0.5f + bw * 0.5f;
        float startY = Settings.HEIGHT * 0.5f + totalH * 0.5f - bh * 0.5f;
        for (int i = 0; i < count; i++) {
            int col = i % pickerCols;
            int row = i / pickerCols;
            TextButton button = new TextButton(pools.get(i).label,
                    startX + col * (bw + gapX), startY - row * (bh + gapY), bw, bh);
            button.highlighted = i == poolIndex;
            pickerButtons.add(button);
        }
    }

    private void selectPool(int index) {
        if (index < 0 || index >= pools.size()) {
            return;
        }
        poolIndex = index;
        pickerOpen = false;
        pickerButtons.clear();
        applyFilter();
        refreshButtonLabels();
    }

    // ------------------------------------------------------------------ 筛选与排版

    private void applyFilter() {
        filtered.clear();
        CardPool pool = pools.isEmpty() ? null : pools.get(poolIndex);
        String needle = query == null ? "" : query.trim().toLowerCase();
        for (AbstractCard card : library) {
            if (pool != null && !pool.accepts(card)) {
                continue;
            }
            if (!needle.isEmpty()) {
                String name = card.name == null ? "" : card.name.toLowerCase();
                String id = card.cardID == null ? "" : card.cardID.toLowerCase();
                if (!name.contains(needle) && !id.contains(needle)) {
                    continue;
                }
            }
            filtered.add(card);
        }
        scroll = 0.0f;
        layoutCards();
    }

    private void layoutCards() {
        int count = filtered.size();
        int rows = count == 0 ? 1 : (count + cols - 1) / cols;
        totalHeight = rows * rowStep;
        clampScroll();
        for (int i = 0; i < count; i++) {
            AbstractCard card = filtered.get(i);
            int col = i % cols;
            int row = i / cols;
            card.current_x = card.target_x = gridCenterX + (col - (cols - 1) * 0.5f) * colStep;
            card.current_y = card.target_y = gridTop - rowStep * 0.5f - row * rowStep + scroll;
            card.drawScale = card.targetDrawScale = cardScale;
            card.angle = card.targetAngle = 0.0f;
            card.transparency = 1.0f;
            card.targetTransparency = 1.0f;
            card.fadingOut = false;
            card.isSelected = selected.contains(card);
        }
    }

    private float minScroll() {
        return Math.min(0.0f, gridBottom - gridTop + totalHeight);
    }

    private void clampScroll() {
        float min = minScroll();
        if (scroll > 0.0f) {
            scroll = 0.0f;
        }
        if (scroll < min) {
            scroll = min;
        }
    }

    private AbstractCard cardAt(float x, float y) {
        float halfW = AbstractCard.IMG_WIDTH * cardScale * 0.5f;
        float halfH = AbstractCard.IMG_HEIGHT * cardScale * 0.5f;
        for (AbstractCard card : filtered) {
            if (card.current_y + halfH < gridBottom || card.current_y - halfH > gridTop) {
                continue;
            }
            if (Math.abs(x - card.current_x) <= halfW && Math.abs(y - card.current_y) <= halfH) {
                return card;
            }
        }
        return null;
    }

    private void toggleSelection(AbstractCard card) {
        if (selected.remove(card)) {
            card.isSelected = false;
        } else {
            selected.add(card);
            card.isSelected = true;
            CardCrawlGame.sound.play("CARD_SELECT");
        }
        refreshButtonLabels();
    }

    // ------------------------------------------------------------------ 输入

    @Override
    public void update() {
        float mx = InputHelper.mX;
        float my = InputHelper.mY;
        boolean clicked = InputHelper.justClickedLeft;

        if (pickerOpen) {
            updatePicker(mx, my, clicked);
            return;
        }

        if (poolButton.update(mx, my, clicked)) {
            stopTyping();
            openPicker();
            return;
        }
        if (clearButton.update(mx, my, clicked)) {
            setQuery("");
            return;
        }
        if (searchButton.update(mx, my, clicked)) {
            if (typing) {
                stopTyping();
            } else {
                startTyping();
            }
            return;
        }
        if (cancelButton.update(mx, my, clicked)) {
            close();
            return;
        }
        if (confirmButton.update(mx, my, clicked)) {
            confirm();
            return;
        }

        if (InputHelper.scrolledUp) {
            scroll -= 120.0f * Settings.scale;
            layoutCards();
        } else if (InputHelper.scrolledDown) {
            scroll += 120.0f * Settings.scale;
            layoutCards();
        }

        updateCardInput(mx, my, clicked);
    }

    private void updatePicker(float mx, float my, boolean clicked) {
        boolean hitAny = false;
        for (int i = 0; i < pickerButtons.size(); i++) {
            TextButton button = pickerButtons.get(i);
            if (button.update(mx, my, clicked)) {
                selectPool(i);
                return;
            }
            if (button.hovered) {
                hitAny = true;
            }
        }
        if (clicked && !hitAny) {
            pickerOpen = false;
            pickerButtons.clear();
        }
        clearMainHovers();
    }

    private void clearMainHovers() {
        poolButton.clearHover();
        searchButton.clearHover();
        clearButton.clearHover();
        confirmButton.clearHover();
        cancelButton.clearHover();
    }

    private void updateCardInput(float mx, float my, boolean clicked) {
        if (clicked) {
            pressing = true;
            dragging = false;
            pressedCard = cardAt(mx, my);
            hoveredCard = pressedCard;
            pressY = my;
            lastY = my;
            return;
        }
        if (!pressing) {
            hoveredCard = null;
            return;
        }
        if (InputHelper.isMouseDown) {
            if (!dragging && Math.abs(my - pressY) > 20.0f * Settings.scale) {
                dragging = true;
            }
            if (dragging) {
                scroll += my - lastY;
                lastY = my;
                layoutCards();
                hoveredCard = null;
            } else {
                hoveredCard = pressedCard;
            }
            return;
        }
        if (!dragging && pressedCard != null && cardAt(mx, my) == pressedCard) {
            toggleSelection(pressedCard);
        }
        pressing = false;
        dragging = false;
        pressedCard = null;
        hoveredCard = null;
    }

    private void setQuery(String value) {
        query = value == null ? "" : value;
        applyFilter();
        refreshButtonLabels();
    }

    /** 开始输入：换上一个“文本输入”处理器，运行时会据此弹出系统软键盘。 */
    private void startTyping() {
        if (typing) {
            return;
        }
        try {
            savedProcessor = Gdx.input.getInputProcessor();
            searchInput = new SearchTextInput(query, new SearchTextInput.Listener() {
                @Override
                public void changed(String text) {
                    setQuery(text);
                }

                @Override
                public void done() {
                    stopTyping();
                }
            }, savedProcessor);
            typing = true;
            Gdx.input.setInputProcessor(searchInput);
            Gdx.input.setOnscreenKeyboardVisible(true);
        } catch (Throwable t) {
            typing = false;
            searchInput = null;
        }
        refreshButtonLabels();
    }

    private void stopTyping() {
        if (!typing) {
            return;
        }
        typing = false;
        searchInput = null;
        try {
            if (savedProcessor != null) {
                Gdx.input.setInputProcessor(savedProcessor);
            }
            Gdx.input.setOnscreenKeyboardVisible(false);
        } catch (Throwable ignored) {
        }
        savedProcessor = null;
        refreshButtonLabels();
    }

    private void confirm() {
        int count = selected.size();
        if (count == 0) {
            close();
            return;
        }
        float offset = -15.0f * count;
        for (int i = 0; i < count; i++) {
            AbstractCard card = selected.get(i);
            AbstractCard reward = copyCard(card);
            if (reward == null) {
                continue;
            }
            AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(reward,
                    Settings.WIDTH * 0.5f + (offset + i * 30.0f) * Settings.scale - AbstractCard.IMG_WIDTH * 0.5f,
                    Settings.HEIGHT * 0.5f));
        }
        close();
    }

    private void close() {
        stopTyping();
        for (AbstractCard card : library) {
            card.isSelected = false;
            card.stopGlowing();
        }
        selected.clear();
        pickerOpen = false;
        pickerButtons.clear();
        openInstance = null;
        ScreenManager.INSTANCE.closeTopMost();
    }

    // ------------------------------------------------------------------ 绘制

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(BACKGROUND);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, Settings.WIDTH, Settings.HEIGHT);

        if (pickerOpen) {
            renderPicker(sb);
            return;
        }

        String title = typing
                ? Texts.t("输入卡牌名称，点“完成”结束", "Type a card name, then tap Done")
                : Texts.t("铸造卡牌", "Craft Cards");
        FontHelper.renderFontCentered(sb, FontHelper.charTitleFont, title,
                Settings.WIDTH * 0.5f, labelY, Settings.GOLD_COLOR);

        poolButton.render(sb);
        searchButton.render(sb);
        clearButton.render(sb);
        renderQuery(sb);

        renderCards(sb);
        renderScrollBar(sb);

        cancelButton.render(sb);
        confirmButton.render(sb);
        FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, statusText(),
                Settings.WIDTH * 0.5f, bottomY, Settings.CREAM_COLOR);
    }

    private void renderQuery(SpriteBatch sb) {
        String text = query.isEmpty()
                ? (typing ? Texts.t("输入中…", "Typing...") : "")
                : Texts.t("搜索：", "Search: ") + query;
        if (text.isEmpty()) {
            return;
        }
        float left = topRowEndX + 12.0f * Settings.scale;
        float right = Settings.WIDTH - margin;
        if (right - left < 40.0f * Settings.scale) {
            return;
        }
        FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, text,
                (left + right) * 0.5f, poolRowY, Settings.CREAM_COLOR);
    }

    private String statusText() {
        if (Texts.useChinese()) {
            return "共 " + filtered.size() + " 张 · 已选 " + selected.size() + " 张";
        }
        return "Cards: " + filtered.size() + "  Selected: " + selected.size();
    }

    private void renderCards(SpriteBatch sb) {
        float halfW = AbstractCard.IMG_WIDTH * cardScale * 0.5f;
        float halfH = AbstractCard.IMG_HEIGHT * cardScale * 0.5f;
        float pad = 6.0f * Settings.scale;
        for (AbstractCard card : filtered) {
            if (card.current_y + halfH < gridBottom || card.current_y - halfH > gridTop) {
                continue;
            }
            boolean isSelected = selected.contains(card);
            if (isSelected || card == hoveredCard) {
                sb.setColor(isSelected ? SELECT_FRAME : HOVER_FRAME);
                sb.draw(ImageMaster.WHITE_SQUARE_IMG,
                        card.current_x - halfW - pad, card.current_y - halfH - pad,
                        halfW * 2.0f + pad * 2.0f, halfH * 2.0f + pad * 2.0f);
            }
            sb.setColor(Color.WHITE);
            card.render(sb);
        }
        if (filtered.isEmpty()) {
            FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont,
                    Texts.t("没有符合条件的卡牌", "No matching cards"),
                    Settings.WIDTH * 0.5f, (gridTop + gridBottom) * 0.5f, Settings.CREAM_COLOR);
        }
    }

    private void renderScrollBar(SpriteBatch sb) {
        float viewH = gridTop - gridBottom;
        if (totalHeight <= viewH + 1.0f) {
            return;
        }
        float scale = Settings.scale;
        float barW = 6.0f * scale;
        float x = Settings.WIDTH - margin * 0.5f;
        sb.setColor(TRACK_COLOR);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x - barW * 0.5f, gridBottom, barW, viewH);
        float thumbH = Math.min(viewH, Math.max(viewH * viewH / totalHeight, 60.0f * scale));
        float min = minScroll();
        float t = min < 0.0f ? (scroll - min) / (-min) : 0.0f;
        t = Math.max(0.0f, Math.min(1.0f, t));
        float thumbY = gridTop - thumbH - (viewH - thumbH) * t;
        sb.setColor(THUMB_COLOR);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x - barW, thumbY, barW * 2.0f, thumbH);
        sb.setColor(Color.WHITE);
    }

    private void renderPicker(SpriteBatch sb) {
        sb.setColor(PICKER_BG);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, Settings.WIDTH, Settings.HEIGHT);
        FontHelper.renderFontCentered(sb, FontHelper.charTitleFont, Texts.t("选择卡池", "Choose Card Pool"),
                Settings.WIDTH * 0.5f, Settings.HEIGHT * 0.88f, Settings.GOLD_COLOR);
        for (TextButton button : pickerButtons) {
            button.render(sb);
        }
        FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont,
                Texts.t("点击空白处返回", "Tap outside to go back"),
                Settings.WIDTH * 0.5f, Settings.HEIGHT * 0.06f, Settings.CREAM_COLOR);
    }
}
