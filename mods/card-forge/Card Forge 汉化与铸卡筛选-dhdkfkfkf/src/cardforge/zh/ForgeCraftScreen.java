package cardforge.zh;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * 雷神之锤的铸卡界面重写版。
 *
 * <p>原界面每帧维护滚动条、对全部卡牌做悬停判定并插值卡牌位置，卡牌一多就会明显抖动；
 * 这里改为固定格位 + 直接赋值坐标，没有插值与百分比回写，因此不会抖动。</p>
 *
 * <p>滚动约定：{@code scroll} 取值 0（第一行贴住网格顶部）到 {@code contentHeight - viewHeight}
 * （末行贴住网格底部）。数值变大 = 内容上移 = 看到后面的卡，和滑块从上往下拖的方向一致。</p>
 *
 * <p>输入由常驻的 {@link CraftPointer} 接管：这个安卓运行时里触摸不会产生鼠标按键状态，
 * 拖动时也不一定刷新触摸事件的坐标，所以按下 / 抬起取触摸事件，
 * 滚动条的拖动位置则在触摸坐标与 {@code InputHelper.mY} 之间取本帧真正变化的那个（见 {@link #updatePointer()}）。</p>
 *
 * <p>搜索词在界面右上角显示（“输入中…” / “搜索：xx”）。系统输入法的预览条由加载器绘制、
 * 里面的文字删不掉，因此删字统一用界面上的“退格 / 清除”按钮。</p>
 */
public final class ForgeCraftScreen extends Screen {
    private static final Color BACKGROUND = new Color(0.05f, 0.05f, 0.08f, 0.96f);
    private static final Color PICKER_BG = new Color(0.03f, 0.03f, 0.05f, 0.97f);
    private static final Color SELECT_FRAME = new Color(1.0f, 0.82f, 0.30f, 0.85f);
    private static final Color HOVER_FRAME = new Color(0.95f, 0.95f, 1.0f, 0.55f);
    private static final Color TRACK_COLOR = new Color(1.0f, 1.0f, 1.0f, 0.32f);
    private static final Color TRACK_EDGE = new Color(1.0f, 1.0f, 1.0f, 0.14f);
    private static final Color THUMB_COLOR = new Color(1.0f, 0.90f, 0.60f, 0.92f);
    private static final Color THUMB_ACTIVE = new Color(1.0f, 0.97f, 0.82f, 1.0f);
    private static final Color THUMB_GRIP = new Color(0.35f, 0.28f, 0.10f, 0.85f);
    private static final Color ARROW_BG = new Color(0.20f, 0.20f, 0.26f, 0.94f);
    private static final Color ARROW_BG_ON = new Color(0.52f, 0.42f, 0.16f, 1.0f);
    private static final Color ARROW_BORDER = new Color(1.0f, 1.0f, 1.0f, 0.30f);
    private static final Color QUERY_COLOR = new Color(1.0f, 0.94f, 0.72f, 1.0f);

    private static final int TARGET_NONE = 0;
    private static final int TARGET_GRID = 1;
    private static final int TARGET_BAR = 2;
    private static final int TARGET_POOL = 5;
    private static final int TARGET_SEARCH = 6;
    private static final int TARGET_CLEAR = 7;
    private static final int TARGET_CANCEL = 8;
    private static final int TARGET_CONFIRM = 9;
    private static final int TARGET_BACKSPACE = 10;

    private static final String KEYBOARD_REQUEST_PROP = "amethyst.in_game_keyboard_request";

    private static ForgeCraftScreen openInstance;

    private final List<AbstractCard> library = new ArrayList<>();
    private final List<AbstractCard> filtered = new ArrayList<>();
    private final List<AbstractCard> selected = new ArrayList<>();
    private final List<CardPool> pools = new ArrayList<>();
    private final List<TextButton> pickerButtons = new ArrayList<>();

    private CraftPointer input;

    private int poolIndex;
    private String query = "";
    private boolean pickerOpen;

    private boolean typing;
    private float typingElapsed;
    private boolean typingGotInput;
    private SearchTextInput textBuffer;
    private volatile String pendingExternalQuery;

    private float margin;
    private float buttonH;
    private float labelY;
    private float poolRowY;
    private float topRowEndX;
    private float gridTop;
    private float gridBottom;
    private float bottomY;
    /** 0 = 第一行贴住网格顶部；正数 = 内容上移（看到后面的卡）。 */
    private float scroll;
    private float colStep;
    private float rowStep;
    private float gridCenterX;
    private float cardScale;
    private float gridRightReserve;
    private int cols;

    private TextButton poolButton;
    private TextButton searchButton;
    private TextButton backspaceButton;
    private TextButton clearButton;
    private TextButton confirmButton;
    private TextButton cancelButton;

    private int pressTarget = TARGET_NONE;
    private boolean pressing;
    private AbstractCard pressedCard;
    private AbstractCard hoveredCard;

    /** 本帧指针 Y：触摸事件与 InputHelper.mY 中真正变化的那个。 */
    private float pointerY;
    private float lastRawY;
    private float lastCursorY;
    private boolean pointerReady;
    /** 拖竖条时指针相对滑块中心的偏移，避免一按就跳。 */
    private float barGrabOffset;
    private boolean barDragging;

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
        screen.input = CraftPointer.install();
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
        gridRightReserve = Math.max(78.0f * scale, width * 0.058f);

        float availW = Math.max(160.0f * scale, width - 2.0f * margin - gridRightReserve);
        float availH = Math.max(140.0f * scale, gridTop - gridBottom);
        float gap = 12.0f * scale;
        float byHeight = (availH / 2.0f) * 0.90f / AbstractCard.IMG_HEIGHT;
        float byWidth = (availW / 6.0f) * 0.90f / AbstractCard.IMG_WIDTH;
        cardScale = Math.max(0.28f, Math.min(0.72f, Math.min(byHeight, byWidth)));
        colStep = AbstractCard.IMG_WIDTH * cardScale + gap;
        rowStep = AbstractCard.IMG_HEIGHT * cardScale + gap;
        cols = Math.max(3, Math.min(10, (int) Math.floor(availW / colStep)));
        gridCenterX = margin + availW * 0.5f;
    }

    private void createButtons() {
        float availW = Settings.WIDTH - 2.0f * margin;
        float gap = 14.0f * Settings.scale;
        float w1 = availW * 0.36f;
        float w2 = availW * 0.14f;
        float w3 = availW * 0.13f;
        float w4 = availW * 0.13f;
        float x = margin;
        poolButton = new TextButton("", x + w1 * 0.5f, poolRowY, w1, buttonH);
        x += w1 + gap;
        searchButton = new TextButton(Texts.t("搜索", "Search"), x + w2 * 0.5f, poolRowY, w2, buttonH);
        x += w2 + gap;
        backspaceButton = new TextButton(Texts.t("退格", "Del"), x + w3 * 0.5f, poolRowY, w3, buttonH);
        x += w3 + gap;
        clearButton = new TextButton(Texts.t("清除", "Clear"), x + w4 * 0.5f, poolRowY, w4, buttonH);
        topRowEndX = x + w4;

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
        backspaceButton.enabled = !query.isEmpty();
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
        float bh = Math.min(70.0f * scale, (Settings.HEIGHT * 0.70f) / rows);
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
        clampScroll();
        for (int i = 0; i < count; i++) {
            AbstractCard card = filtered.get(i);
            int col = i % cols;
            int row = i / cols;
            card.current_x = card.target_x = gridCenterX + (col - (cols - 1) * 0.5f) * colStep;
            // scroll 变大 = 内容上移 = 看到后面的卡
            card.current_y = card.target_y = gridTop - rowStep * 0.5f - row * rowStep + scroll;
            card.drawScale = card.targetDrawScale = cardScale;
            card.angle = card.targetAngle = 0.0f;
            card.transparency = 1.0f;
            card.targetTransparency = 1.0f;
            card.fadingOut = false;
            card.isSelected = selected.contains(card);
        }
    }

    private float viewHeight() {
        return Math.max(1.0f, gridTop - gridBottom);
    }

    private int contentRows() {
        int perRow = Math.max(1, cols);
        return Math.max(1, (filtered.size() + perRow - 1) / perRow);
    }

    /** 内容总高度：每次按当前卡数与格距重新算，避免缓存过期导致“看起来不能滚”。 */
    private float contentHeight() {
        return contentRows() * rowStep;
    }

    /** 能滚多远（正数）：0 表示全部卡牌都在一屏里。 */
    private float maxScroll() {
        return Math.max(0.0f, contentHeight() - viewHeight());
    }

    private void clampScroll() {
        float max = maxScroll();
        if (scroll < 0.0f) {
            scroll = 0.0f;
        }
        if (scroll > max) {
            scroll = max;
        }
    }

    private void scrollBy(float delta) {
        if (delta == 0.0f) {
            return;
        }
        scroll += delta;
        layoutCards();
    }

    /** 一次翻一屏：sign = +1 往后翻（看到后面的卡），-1 往前翻。 */
    private void pageBy(float sign) {
        scrollBy(sign * viewHeight() * 0.88f);
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

    // ------------------------------------------------------------------ 滚动条几何

    private boolean canScroll() {
        return maxScroll() > 0.5f && barTop() > barBottom();
    }

    private float barCenterX() {
        return Settings.WIDTH - margin - gridRightReserve * 0.5f;
    }

    private float arrowSize() {
        return Math.max(52.0f * Settings.scale, 34.0f);
    }

    private float barTop() {
        return gridTop - arrowSize() * 1.15f;
    }

    private float barBottom() {
        return gridBottom + arrowSize() * 1.15f;
    }

    private float trackWidth() {
        return Math.max(12.0f * Settings.scale, 7.0f);
    }

    private float thumbWidth() {
        return Math.max(30.0f * Settings.scale, 18.0f);
    }

    private float thumbHeight() {
        float trackH = Math.max(1.0f, barTop() - barBottom());
        float ratio = viewHeight() / Math.max(contentHeight(), 1.0f);
        float minimum = Math.min(trackH, Math.max(96.0f * Settings.scale, 40.0f));
        return Math.min(trackH, Math.max(trackH * Math.min(1.0f, ratio), minimum));
    }

    /** 滑块进度：0 = 最上（第一行），1 = 最下（末行）。 */
    private float barProgress() {
        float max = maxScroll();
        if (max <= 0.5f) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, scroll / max));
    }

    private float thumbBottom() {
        float trackH = Math.max(1.0f, barTop() - barBottom());
        float thumbH = thumbHeight();
        return barTop() - thumbH - (trackH - thumbH) * barProgress();
    }

    private float thumbCenter() {
        return thumbBottom() + thumbHeight() * 0.5f;
    }

    private static boolean inRect(float cx, float cy, float sizeX, float sizeY, float px, float py) {
        return px >= cx - sizeX * 0.5f && px <= cx + sizeX * 0.5f
                && py >= cy - sizeY * 0.5f && py <= cy + sizeY * 0.5f;
    }

    private boolean inPageUp(float mx, float my) {
        return canScroll() && inRect(barCenterX(), gridTop - arrowSize() * 0.55f,
                arrowSize() * 1.25f, arrowSize() * 1.25f, mx, my);
    }

    private boolean inPageDown(float mx, float my) {
        return canScroll() && inRect(barCenterX(), gridBottom + arrowSize() * 0.55f,
                arrowSize() * 1.25f, arrowSize() * 1.25f, mx, my);
    }

    /** 竖条整段（含滑块）都可点：点在滑块上就按抓取处理，点在轨道上就直接跳过去。 */
    private boolean inBar(float mx, float my) {
        if (!canScroll()) {
            return false;
        }
        float halfHit = Math.max(thumbWidth() * 0.9f, 30.0f * Settings.scale);
        return Math.abs(mx - barCenterX()) <= halfHit
                && my >= barBottom() - arrowSize() * 0.35f
                && my <= barTop() + arrowSize() * 0.35f;
    }

    private boolean inThumb(float my) {
        float bottom = thumbBottom();
        return my >= bottom - 8.0f * Settings.scale && my <= bottom + thumbHeight() + 8.0f * Settings.scale;
    }

    /** 让滑块中心落到 targetCenterY（世界坐标，y 向上）处；往下拖 = 看后面的卡。 */
    private void applyBarCenter(float targetCenterY) {
        float max = maxScroll();
        if (max <= 0.5f) {
            return;
        }
        float top = barTop();
        float thumbH = thumbHeight();
        float travel = Math.max(1.0f, (top - barBottom()) - thumbH);
        float centerTop = top - thumbH * 0.5f;
        float t = (centerTop - targetCenterY) / travel;
        t = Math.max(0.0f, Math.min(1.0f, t));
        scroll = max * t;
        layoutCards();
    }

    // ------------------------------------------------------------------ 输入

    @Override
    public void update() {
        if (input == null) {
            return;
        }
        float delta = Gdx.graphics.getDeltaTime();

        String pending = pendingExternalQuery;
        if (pending != null) {
            pendingExternalQuery = null;
            setQuery(pending);
            stopTyping();
        }
        if (typing) {
            typingElapsed += delta;
        }

        // 常驻处理器：其它界面、失焦、恢复焦点都可能把它换掉，这里始终保持我们的在位。
        if (Gdx.input.getInputProcessor() != input) {
            try {
                Gdx.input.setInputProcessor(input);
            } catch (Throwable ignored) {
            }
        }

        float mx = input.x;
        float my = input.y;
        updatePointer();

        if (pickerOpen) {
            updatePicker(mx, my);
            input.consume();
            return;
        }

        updateHovers(mx, my);

        // ▲ / ▼ 按下即翻页（点按最可靠，不依赖拖动）。
        if (input.pressed && inPageUp(mx, my)) {
            pageBy(-1.0f);
            input.consume();
            return;
        }
        if (input.pressed && inPageDown(mx, my)) {
            pageBy(1.0f);
            input.consume();
            return;
        }

        if (input.pressed) {
            beginPress(mx, my);
        } else if (pressing && (!input.down || input.released)) {
            finishPress(mx, my);
        } else if (pressing) {
            continuePress();
        }

        if (!pressing && input.scrollAmount != 0) {
            // 滚轮向下（负值）= 看后面的卡
            scrollBy(-input.scrollAmount * Settings.SCROLL_SPEED * 0.6f);
        }

        input.consume();
    }

    /**
     * 取本帧真正变化的指针高度：触摸事件坐标，或 {@code InputHelper.mY}。
     * 这个运行时在拖动时通常只刷新其中之一，取“变了的那一个”才能让滑块跟手。
     * 两者都是 y 向上的世界坐标（{@code HEIGHT - 屏幕Y}），可以直接混用。
     */
    private void updatePointer() {
        float rawY = input.y;
        float cursorY = InputHelper.mY;
        if (!pointerReady) {
            pointerY = rawY;
            lastRawY = rawY;
            lastCursorY = cursorY;
            pointerReady = true;
            return;
        }
        if (Math.abs(cursorY - lastCursorY) > 0.5f) {
            pointerY = cursorY;
        } else if (Math.abs(rawY - lastRawY) > 0.5f) {
            pointerY = rawY;
        }
        lastRawY = rawY;
        lastCursorY = cursorY;
    }

    private void updateHovers(float mx, float my) {
        poolButton.updateHover(mx, my);
        searchButton.updateHover(mx, my);
        backspaceButton.updateHover(mx, my);
        clearButton.updateHover(mx, my);
        confirmButton.updateHover(mx, my);
        cancelButton.updateHover(mx, my);
        hoveredCard = cardAt(mx, my);
    }

    private void beginPress(float mx, float my) {
        pressing = true;
        barDragging = false;
        pressedCard = null;

        // 按下这一帧的触摸坐标是准的，用它对齐指针基准。
        pointerY = my;
        lastRawY = my;
        lastCursorY = InputHelper.mY;
        pointerReady = true;

        if (inBar(mx, my)) {
            pressTarget = TARGET_BAR;
            if (inThumb(my)) {
                barGrabOffset = pointerY - thumbCenter();
                barDragging = true;
            } else {
                barGrabOffset = 0.0f;
                applyBarCenter(pointerY);
            }
            return;
        }
        if (searchButton.contains(mx, my)) {
            pressTarget = TARGET_SEARCH;
            return;
        }
        if (poolButton.contains(mx, my)) {
            pressTarget = TARGET_POOL;
            return;
        }
        if (backspaceButton.enabled && backspaceButton.contains(mx, my)) {
            pressTarget = TARGET_BACKSPACE;
            return;
        }
        if (clearButton.enabled && clearButton.contains(mx, my)) {
            pressTarget = TARGET_CLEAR;
            return;
        }
        if (cancelButton.contains(mx, my)) {
            pressTarget = TARGET_CANCEL;
            return;
        }
        if (confirmButton.enabled && confirmButton.contains(mx, my)) {
            pressTarget = TARGET_CONFIRM;
            return;
        }
        if (my >= gridBottom && my <= gridTop) {
            pressTarget = TARGET_GRID;
            pressedCard = cardAt(mx, my);
            return;
        }
        pressTarget = TARGET_NONE;
    }

    private void continuePress() {
        if (pressTarget != TARGET_BAR) {
            return;
        }
        // 按住竖条拖动：滑块跟着手指走。
        applyBarCenter(pointerY - barGrabOffset);
    }

    private void finishPress(float mx, float my) {
        int target = pressTarget;
        AbstractCard card = pressedCard;
        pressTarget = TARGET_NONE;
        pressing = false;
        barDragging = false;
        pressedCard = null;
        hoveredCard = cardAt(mx, my);

        if (target == TARGET_NONE || target == TARGET_BAR) {
            return;
        }
        if (target == TARGET_GRID) {
            if (card != null && cardAt(mx, my) == card) {
                toggleSelection(card);
            }
            return;
        }
        if (target == TARGET_SEARCH && !searchButton.contains(mx, my)) {
            return;
        }
        if (target == TARGET_POOL && !poolButton.contains(mx, my)) {
            return;
        }
        if (target == TARGET_BACKSPACE && !backspaceButton.contains(mx, my)) {
            return;
        }
        if (target == TARGET_CLEAR && !clearButton.contains(mx, my)) {
            return;
        }
        if (target == TARGET_CANCEL && !cancelButton.contains(mx, my)) {
            return;
        }
        if (target == TARGET_CONFIRM && !confirmButton.contains(mx, my)) {
            return;
        }

        switch (target) {
            case TARGET_POOL:
                stopTyping();
                openPicker();
                break;
            case TARGET_SEARCH:
                if (typing) {
                    stopTyping();
                } else {
                    startTyping();
                }
                break;
            case TARGET_BACKSPACE:
                if (!query.isEmpty()) {
                    applyQueryFromButton(query.substring(0, query.length() - 1));
                }
                break;
            case TARGET_CLEAR:
                applyQueryFromButton("");
                break;
            case TARGET_CANCEL:
                close();
                break;
            case TARGET_CONFIRM:
                confirm();
                break;
            default:
                break;
        }
    }

    private void updatePicker(float mx, float my) {
        boolean hitAny = false;
        for (int i = 0; i < pickerButtons.size(); i++) {
            TextButton button = pickerButtons.get(i);
            button.updateHover(mx, my);
            if (button.hovered) {
                hitAny = true;
            }
            if (button.hovered && input.released) {
                selectPool(i);
                return;
            }
        }
        if (!hitAny && input.released) {
            pickerOpen = false;
            pickerButtons.clear();
        }
        pressTarget = TARGET_NONE;
        pressing = false;
        barDragging = false;
        clearMainHovers();
    }

    private void clearMainHovers() {
        poolButton.clearHover();
        searchButton.clearHover();
        backspaceButton.clearHover();
        clearButton.clearHover();
        confirmButton.clearHover();
        cancelButton.clearHover();
        hoveredCard = null;
    }

    /** 键盘/加载器回传的文本。 */
    private void setQuery(String value) {
        query = value == null ? "" : value;
        applyFilter();
        refreshButtonLabels();
    }

    /** 界面上“退格 / 清除”按钮改的文本：同时同步给键盘缓冲，免得后面的字接在旧内容上。 */
    private void applyQueryFromButton(String value) {
        setQuery(value);
        if (textBuffer != null) {
            textBuffer.reset(query);
        }
    }

    // ------------------------------------------------------------------ 系统输入法

    /** 系统键盘桥是否可用（Amethyst 运行时会提供这个属性）。 */
    private static String keyboardRequestPath() {
        try {
            String path = System.getProperty(KEYBOARD_REQUEST_PROP, "").trim();
            return path.isEmpty() ? null : path;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String base64(String text) {
        byte[] bytes;
        try {
            bytes = text.getBytes("UTF-8");
        } catch (Throwable t) {
            bytes = text.getBytes();
        }
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i + 2 < bytes.length) {
            int n = ((bytes[i] & 0xFF) << 16) | ((bytes[i + 1] & 0xFF) << 8) | (bytes[i + 2] & 0xFF);
            out.append(alphabet.charAt((n >> 18) & 63))
                    .append(alphabet.charAt((n >> 12) & 63))
                    .append(alphabet.charAt((n >> 6) & 63))
                    .append(alphabet.charAt(n & 63));
            i += 3;
        }
        int rest = bytes.length - i;
        if (rest == 1) {
            int n = (bytes[i] & 0xFF) << 16;
            out.append(alphabet.charAt((n >> 18) & 63))
                    .append(alphabet.charAt((n >> 12) & 63))
                    .append("==");
        } else if (rest == 2) {
            int n = ((bytes[i] & 0xFF) << 16) | ((bytes[i + 1] & 0xFF) << 8);
            out.append(alphabet.charAt((n >> 18) & 63))
                    .append(alphabet.charAt((n >> 12) & 63))
                    .append(alphabet.charAt((n >> 6) & 63))
                    .append('=');
        }
        return out.toString();
    }

    /**
     * 按运行时的“系统键盘预览”协议写请求文件：来源、时间戳、初值、允许字符、长度上限。
     * 这是 TogetherInSpire 聊天框在同一运行时里唤起手机输入法所用的格式。
     */
    private static void writeKeyboardRequest(String path, String initial, int limit) {
        try {
            File file = new File(path);
            File parent = file.getParentFile();
            if (parent != null && !parent.isDirectory()) {
                parent.mkdirs();
            }
            FileWriter writer = new FileWriter(file, false);
            writer.write("system_keyboard_preview:card_forge_search");
            writer.write(10);
            writer.write(Long.toString(System.currentTimeMillis()));
            writer.write(10);
            writer.write(base64(initial == null ? "" : initial));
            writer.write(10);
            writer.write(base64(""));
            writer.write(10);
            writer.write(Integer.toString(limit));
            writer.write(10);
            writer.close();
        } catch (Throwable ignored) {
        }
    }

    /** 其它安卓后端（含桌面）用 libGDX 的系统文本框弹出输入法。 */
    private void requestTextDialog() {
        try {
            Gdx.input.getTextInput(new Input.TextInputListener() {
                @Override
                public void input(String text) {
                    pendingExternalQuery = text == null ? "" : text;
                }

                @Override
                public void canceled() {
                }
            }, Texts.t("搜索卡牌", "Search cards"), query, Texts.t("卡牌名称", "Card name"));
        } catch (Throwable ignored) {
        }
    }

    /**
     * 开始输入：用 {@link SearchKeyboardTrigger} 触发一次系统键盘请求，然后立刻把常驻处理器装回去。
     *
     * <p>这样即使运行时在弹键盘时切换了输入处理器、或失焦后调用 regainInputFocus 换掉处理器，
     * 字符最终仍会落到 {@link CraftPointer} 上（它每帧都会把自己装回去）。</p>
     */
    private void startTyping() {
        if (typing) {
            return;
        }
        typing = true;
        typingElapsed = 0.0f;
        typingGotInput = false;
        textBuffer = new SearchTextInput(query, new SearchTextInput.Listener() {
            @Override
            public void changed(String text) {
                typingGotInput = true;
                setQuery(text);
            }

            @Override
            public void committed(String text) {
                typingGotInput = true;
                setQuery(text);
                stopTyping();
            }

            @Override
            public void done() {
                stopTyping();
            }
        });
        if (input != null) {
            input.text = textBuffer;
        }
        try {
            Gdx.input.setInputProcessor(new SearchKeyboardTrigger(input));
        } catch (Throwable ignored) {
        }
        try {
            if (input != null) {
                Gdx.input.setInputProcessor(input);
            }
            Gdx.input.setOnscreenKeyboardVisible(true);
        } catch (Throwable ignored) {
        }
        String path = keyboardRequestPath();
        if (path != null) {
            writeKeyboardRequest(path, query, -1);
        } else {
            requestTextDialog();
        }
        refreshButtonLabels();
    }

    private void stopTyping() {
        if (!typing) {
            return;
        }
        typing = false;
        typingElapsed = 0.0f;
        textBuffer = null;
        if (input != null) {
            input.text = null;
        }
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
        if (input != null) {
            input.uninstall();
        }
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

        FontHelper.renderFontCentered(sb, FontHelper.charTitleFont, Texts.t("铸造卡牌", "Craft Cards"),
                Settings.WIDTH * 0.5f, labelY, Settings.GOLD_COLOR);

        poolButton.render(sb);
        searchButton.render(sb);
        backspaceButton.render(sb);
        clearButton.render(sb);
        renderQuery(sb);

        renderCards(sb);
        renderScrollBar(sb);

        cancelButton.render(sb);
        confirmButton.render(sb);
        FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, statusText(),
                Settings.WIDTH * 0.5f, bottomY, Settings.CREAM_COLOR);
    }

    /** 右上角搜索词显示：正在输入时显示“输入中…”，有内容时显示“搜索：xx”。 */
    private void renderQuery(SpriteBatch sb) {
        String text;
        if (!query.isEmpty()) {
            text = Texts.t("搜索：", "Search: ") + query;
        } else if (typing) {
            text = Texts.t("输入中…", "Typing…");
        } else {
            return;
        }
        float left = topRowEndX + 12.0f * Settings.scale;
        float right = Settings.WIDTH - margin - gridRightReserve;
        if (right - left < 40.0f * Settings.scale) {
            return;
        }
        FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, text,
                (left + right) * 0.5f, poolRowY, QUERY_COLOR);
    }

    private String statusText() {
        if (Texts.useChinese()) {
            StringBuilder sb = new StringBuilder();
            sb.append("共 ").append(filtered.size()).append(" 张 · 已选 ").append(selected.size()).append(" 张");
            if (typing) {
                sb.append(" · 打完字按输入法的回车");
            }
            return sb.toString();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Cards: ").append(filtered.size()).append("  Selected: ").append(selected.size());
        return sb.toString();
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

    /** 竖条 + 滑块 + 两端 ▲▼：始终画在右侧预留区里，不会被卡牌盖住。 */
    private void renderScrollBar(SpriteBatch sb) {
        float scale = Settings.scale;
        float x = barCenterX();
        float trackW = trackWidth();
        float thumbW = thumbWidth();
        float progress = barProgress();

        renderArrowButton(sb, x, gridTop - arrowSize() * 0.55f, "▲", progress > 0.001f);
        renderArrowButton(sb, x, gridBottom + arrowSize() * 0.55f, "▼", progress < 0.999f);

        if (!canScroll()) {
            return;
        }
        float top = barTop();
        float bottom = barBottom();
        float trackH = top - bottom;
        if (trackH <= 6.0f) {
            return;
        }

        sb.setColor(TRACK_EDGE);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x - trackW * 0.5f - 2.0f * scale, bottom - 2.0f * scale,
                trackW + 4.0f * scale, trackH + 4.0f * scale);
        sb.setColor(TRACK_COLOR);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x - trackW * 0.5f, bottom, trackW, trackH);

        float thumbH = thumbHeight();
        float thumbY = thumbBottom();
        sb.setColor(barDragging || pressTarget == TARGET_BAR ? THUMB_ACTIVE : THUMB_COLOR);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x - thumbW * 0.5f, thumbY, thumbW, thumbH);

        // 滑块上的三道抓握纹，让它看起来是可以拖的。
        sb.setColor(THUMB_GRIP);
        float gripH = Math.max(2.0f, 2.0f * scale);
        float gripW = thumbW * 0.55f;
        for (int i = -1; i <= 1; i++) {
            sb.draw(ImageMaster.WHITE_SQUARE_IMG,
                    x - gripW * 0.5f, thumbY + thumbH * 0.5f + i * 10.0f * scale - gripH * 0.5f,
                    gripW, gripH);
        }
        sb.setColor(Color.WHITE);
    }

    private void renderArrowButton(SpriteBatch sb, float cx, float cy, String glyph, boolean active) {
        float size = arrowSize();
        sb.setColor(active ? ARROW_BG_ON : ARROW_BG);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, cx - size * 0.5f, cy - size * 0.5f, size, size);
        sb.setColor(ARROW_BORDER);
        float line = Math.max(2.0f, 2.0f * Settings.scale);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, cx - size * 0.5f, cy + size * 0.5f - line, size, line);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, cx - size * 0.5f, cy - size * 0.5f, size, line);
        sb.setColor(Color.WHITE);
        FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, glyph, cx, cy,
                active ? Color.WHITE : Settings.CREAM_COLOR);
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
