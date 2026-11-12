package ironcladperks.mobilefix;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import ironcladperks.IroncladPerksMod;
import ironcladperks.perks.PerkSaveData;
import ironcladperks.screens.PerkTreeScreen;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Touch friendly controls for the perk tree screen, plus the extra buttons drawn on top of it.
 *
 * <p>The parent mod only understands left click (buy), right click (remove) and right-drag (pan),
 * so on a touch device the tree can neither be panned nor left. This class replaces the input
 * handling with gestures that work with one finger and adds an on-screen CLOSE button, a tap
 * mode switch and the bonus on/off switch.</p>
 */
public final class MobileTreeUI {
    private static final Logger logger = LogManager.getLogger(MobileTreeUI.class.getName());

    private static final int WIDGET_NONE = 0;
    private static final int WIDGET_CLOSE = 1;
    private static final int WIDGET_BONUS = 2;
    private static final int WIDGET_MODE = 3;

    private static final long TAP_MAX_MILLIS = 800L;
    private static final float NODE_SIZE = 72.0f;

    private static final Color CLOSE_FILL = new Color(0.32f, 0.11f, 0.11f, 0.96f);
    private static final Color CLOSE_BORDER = new Color(0.95f, 0.55f, 0.45f, 1.0f);
    private static final Color BONUS_ON_FILL = new Color(0.10f, 0.28f, 0.16f, 0.96f);
    private static final Color BONUS_ON_BORDER = new Color(0.40f, 0.90f, 0.50f, 1.0f);
    private static final Color BONUS_OFF_FILL = new Color(0.34f, 0.25f, 0.06f, 0.96f);
    private static final Color BONUS_OFF_BORDER = new Color(0.95f, 0.75f, 0.30f, 1.0f);
    private static final Color BUY_FILL = new Color(0.11f, 0.19f, 0.34f, 0.96f);
    private static final Color BUY_BORDER = new Color(0.60f, 0.80f, 1.00f, 1.0f);
    private static final Color REMOVE_FILL = new Color(0.38f, 0.17f, 0.09f, 0.96f);
    private static final Color REMOVE_BORDER = new Color(1.00f, 0.60f, 0.35f, 1.0f);
    private static final Color SHADOW = new Color(0.0f, 0.0f, 0.0f, 0.65f);
    private static final Color PAUSED_TEXT = new Color(0.95f, 0.75f, 0.30f, 1.0f);

    private static Texture pixel;
    private static boolean removeMode;
    private static boolean warned;

    private static boolean touching;
    private static int lastX;
    private static int lastY;
    private static float dragDistance;
    private static long pressMillis;
    private static boolean dragging;
    private static String pressedPerk;
    private static int pressedWidget = WIDGET_NONE;

    private MobileTreeUI() {
    }

    /** Simple screen space rectangle. */
    public static final class Rect {
        public final float x;
        public final float y;
        public final float w;
        public final float h;

        private Rect(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public boolean contains(int mx, int my) {
            return mx > x && mx < x + w && my > y && my < y + h;
        }
    }

    // ------------------------------------------------------------------ layout

    private static float scale() {
        return Settings.scale;
    }

    private static float dragThreshold() {
        return Math.max(14.0f * scale(), Settings.HEIGHT * 0.015f);
    }

    private static float buttonHeight() {
        float text = FontHelper.getHeight(FontHelper.tipBodyFont);
        return Math.max(58.0f * scale(), text + 20.0f * scale());
    }

    private static float buttonPad() {
        return 28.0f * scale();
    }

    private static float buttonWidth(String label) {
        float text = FontHelper.getWidth(FontHelper.tipBodyFont, label, 1.0f);
        return Math.max(150.0f * scale(), text + 40.0f * scale());
    }

    public static Rect closeRect() {
        String label = MobileStrings.close();
        float w = buttonWidth(label);
        float h = buttonHeight();
        return new Rect(Settings.WIDTH - w - buttonPad(), Settings.HEIGHT - h - buttonPad(), w, h);
    }

    public static Rect bonusRect() {
        String label = bonusLabel();
        float w = buttonWidth(label);
        float h = buttonHeight();
        return new Rect(buttonPad(), Settings.HEIGHT - h - buttonPad(), w, h);
    }

    public static Rect modeRect() {
        String label = modeLabel();
        float w = buttonWidth(label);
        float h = buttonHeight();
        return new Rect(buttonPad(), Settings.HEIGHT - 2.0f * h - buttonPad() - 14.0f * scale(), w, h);
    }

    private static String bonusLabel() {
        return PerkMobileConfig.bonusesEnabled() ? MobileStrings.bonusOn() : MobileStrings.bonusOff();
    }

    private static String modeLabel() {
        return removeMode ? MobileStrings.modeRemove() : MobileStrings.modeBuy();
    }

    // ------------------------------------------------------------------ input

    public static void resetGesture() {
        touching = false;
        dragging = false;
        dragDistance = 0.0f;
        pressedPerk = null;
        pressedWidget = WIDGET_NONE;
    }

    public static void update(PerkTreeScreen screen) {
        if (!PerkTreeReflect.available()) {
            if (!warned) {
                warned = true;
                logger.warn("PerkTreeMobile: tree internals unavailable, the touch controls stay disabled");
            }
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            screen.close();
            resetGesture();
            return;
        }

        int mx = InputHelper.mX;
        int my = InputHelper.mY;
        boolean down = InputHelper.isMouseDown || Gdx.input.isTouched();
        long now = System.currentTimeMillis();
        String hovered = syncHitboxes(screen);

        if (!touching) {
            if (down) {
                touching = true;
                dragging = false;
                dragDistance = 0.0f;
                lastX = mx;
                lastY = my;
                pressMillis = now;
                pressedPerk = hovered;
                pressedWidget = widgetAt(mx, my);
            }
            return;
        }

        if (down) {
            int dx = mx - lastX;
            int dy = my - lastY;
            dragDistance += Math.abs(dx) + Math.abs(dy);
            if (pressedWidget == WIDGET_NONE && !dragging && dragDistance > dragThreshold()) {
                dragging = true;
                PerkTreeReflect.setHovered(screen, null);
            }
            if (dragging) {
                PerkTreeReflect.setCameraX(screen,
                        clampX(screen, PerkTreeReflect.getCameraX(screen) + dx));
                PerkTreeReflect.setCameraY(screen,
                        clampY(screen, PerkTreeReflect.getCameraY(screen) + dy));
            }
            lastX = mx;
            lastY = my;
            return;
        }

        // finger lifted
        if (!dragging && dragDistance <= dragThreshold() && now - pressMillis <= TAP_MAX_MILLIS) {
            onTap(screen, pressedWidget, pressedPerk);
        }
        resetGesture();
    }

    private static int widgetAt(int mx, int my) {
        if (closeRect().contains(mx, my)) {
            return WIDGET_CLOSE;
        }
        if (bonusRect().contains(mx, my)) {
            return WIDGET_BONUS;
        }
        if (modeRect().contains(mx, my)) {
            return WIDGET_MODE;
        }
        return WIDGET_NONE;
    }

    private static void onTap(PerkTreeScreen screen, int widget, String perkId) {
        if (widget == WIDGET_CLOSE) {
            screen.close();
            return;
        }
        if (widget == WIDGET_BONUS) {
            PerkMobileConfig.toggleBonuses();
            return;
        }
        if (widget == WIDGET_MODE) {
            removeMode = !removeMode;
            logger.info("PerkTreeMobile: tap mode is now " + (removeMode ? "REMOVE" : "BUY"));
            return;
        }
        if (perkId == null) {
            return;
        }
        PerkSaveData data = IroncladPerksMod.getSaveData();
        if (data == null) {
            return;
        }
        boolean changed = removeMode ? data.removePerk(perkId) : data.purchasePerk(perkId);
        if (changed) {
            logger.info("PerkTreeMobile: " + (removeMode ? "removed" : "purchased") + " "
                    + perkId + " (rank " + data.getRank(perkId) + ")");
        }
    }

    private static String syncHitboxes(PerkTreeScreen screen) {
        Map<String, Hitbox> boxes = PerkTreeReflect.hitboxes(screen);
        Map<String, float[]> positions = PerkTreeReflect.positions(screen);
        float camX = PerkTreeReflect.getCameraX(screen);
        float camY = PerkTreeReflect.getCameraY(screen);
        String hovered = null;
        for (Map.Entry<String, Hitbox> entry : boxes.entrySet()) {
            Hitbox hitbox = entry.getValue();
            float[] pos = positions.get(entry.getKey());
            if (hitbox == null || pos == null) {
                continue;
            }
            hitbox.update(pos[0] + camX, pos[1] + camY);
            if (hitbox.hovered && hovered == null) {
                hovered = entry.getKey();
            }
        }
        PerkTreeReflect.setHovered(screen, hovered);
        return hovered;
    }

    private static float clampX(PerkTreeScreen screen, float value) {
        Map<String, float[]> positions = PerkTreeReflect.positions(screen);
        if (positions.isEmpty()) {
            return value;
        }
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        for (float[] pos : positions.values()) {
            if (pos == null) {
                continue;
            }
            minX = Math.min(minX, pos[0]);
            maxX = Math.max(maxX, pos[0] + NODE_SIZE * scale());
        }
        if (minX > maxX) {
            return value;
        }
        float margin = 80.0f * scale();
        float low = Math.min(0.0f, margin - maxX);
        float high = Math.max(0.0f, Settings.WIDTH - margin - minX);
        return Math.max(low, Math.min(high, value));
    }

    private static float clampY(PerkTreeScreen screen, float value) {
        Map<String, float[]> positions = PerkTreeReflect.positions(screen);
        if (positions.isEmpty()) {
            return value;
        }
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (float[] pos : positions.values()) {
            if (pos == null) {
                continue;
            }
            minY = Math.min(minY, pos[1]);
            maxY = Math.max(maxY, pos[1] + NODE_SIZE * scale());
        }
        if (minY > maxY) {
            return value;
        }
        float margin = 80.0f * scale();
        float low = Math.min(0.0f, margin - maxY);
        float high = Math.max(0.0f, Settings.HEIGHT - margin - minY);
        return Math.max(low, Math.min(high, value));
    }

    // ------------------------------------------------------------------ render

    public static void render(SpriteBatch sb) {
        if (!PerkTreeReflect.available()) {
            return;
        }
        sb.setColor(Color.WHITE);
        drawButton(sb, closeRect(), MobileStrings.close(), CLOSE_FILL, CLOSE_BORDER);

        boolean enabled = PerkMobileConfig.bonusesEnabled();
        drawButton(sb, bonusRect(), bonusLabel(),
                enabled ? BONUS_ON_FILL : BONUS_OFF_FILL,
                enabled ? BONUS_ON_BORDER : BONUS_OFF_BORDER);

        drawButton(sb, modeRect(), modeLabel(),
                removeMode ? REMOVE_FILL : BUY_FILL,
                removeMode ? REMOVE_BORDER : BUY_BORDER);

        if (!enabled) {
            FontHelper.renderFontCentered(sb, FontHelper.tipBodyFont, MobileStrings.bonusesPausedNote(),
                    Settings.WIDTH / 2.0f, 72.0f * scale(), PAUSED_TEXT);
        }
        sb.setColor(Color.WHITE);
    }

    private static void drawButton(SpriteBatch sb, Rect rect, String label, Color fill, Color border) {
        float s = scale();
        drawRect(sb, SHADOW, rect.x - 3.0f * s, rect.y - 3.0f * s, rect.w + 6.0f * s, rect.h + 6.0f * s);
        drawRect(sb, border, rect.x, rect.y, rect.w, rect.h);
        drawRect(sb, fill, rect.x + 2.0f * s, rect.y + 2.0f * s, rect.w - 4.0f * s, rect.h - 4.0f * s);
        FontHelper.renderFontCentered(sb, FontHelper.tipBodyFont, label,
                rect.x + rect.w / 2.0f, rect.y + rect.h / 2.0f + 3.0f * s, Color.WHITE);
        sb.setColor(Color.WHITE);
    }

    private static void drawRect(SpriteBatch sb, Color color, float x, float y, float w, float h) {
        sb.setColor(color);
        sb.draw(pixel(), x, y, w, h);
        sb.setColor(Color.WHITE);
    }

    private static Texture pixel() {
        if (pixel == null) {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            pixel = new Texture(pixmap);
            pixmap.dispose();
        }
        return pixel;
    }
}
