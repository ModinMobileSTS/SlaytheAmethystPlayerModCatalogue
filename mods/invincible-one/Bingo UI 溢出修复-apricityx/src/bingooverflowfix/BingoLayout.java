package bingooverflowfix;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import java.util.HashMap;
import java.util.Map;

/**
 * Geometry and text-fitting helpers shared by the bingo patches.
 *
 * The parent mod sized every bingo cell from {@code Settings.WIDTH} only (side = 0.1 * WIDTH, row
 * spacing = 0.1 * WIDTH), so the 5x5 grid is always 0.5 * WIDTH tall no matter how tall the screen
 * is. On wide phone screens (19.5:9 and above) that grid is taller than the screen, so the first and
 * the last row plus their text are cut off. Everything here derives the cell size from BOTH axes.
 */
public final class BingoLayout {

    /** Design resolution of the game; used as the reference for small-screen shrinkage. */
    private static final float DESIGN_WIDTH = 1920.0f;
    private static final float DESIGN_HEIGHT = 1080.0f;

    /** A cell may use at most 10% of the width and at most 18% of the height (0.90 / 5 rows). */
    private static final float CELL_WIDTH_RATIO = 0.10f;
    private static final float CELL_HEIGHT_RATIO = 0.18f;

    /** Horizontal anchor, identical to the parent mod so the panel does not move sideways. */
    private static final float GRID_LEFT_RATIO = 0.40f;

    /** Smallest factor applied to the body font before falling back to truncation. */
    private static final float MIN_TEXT_SCALE = 0.50f;
    private static final float SCALE_STEP = 0.05f;

    private static final Map<String, Fitted> CACHE = new HashMap<String, Fitted>();
    private static final int CACHE_LIMIT = 256;

    private BingoLayout() {
    }

    /** Side length of one bingo cell, in world units. Height aware. */
    public static float cellSize() {
        float w = Settings.WIDTH;
        float h = Settings.HEIGHT;
        if (w <= 0.0f || h <= 0.0f) {
            return 1.0f;
        }
        float cell = Math.min(w * CELL_WIDTH_RATIO, h * CELL_HEIGHT_RATIO);
        // On low resolution screens the fixed pixel paddings eat into the cell, so shrink together
        // with the design-to-screen ratio, exactly like Settings.scale does.
        float design = Math.min(w / DESIGN_WIDTH, h / DESIGN_HEIGHT);
        if (design < 1.0f) {
            cell *= design;
        }
        return Math.max(cell, 1.0f);
    }

    public static float originX(int column) {
        return Settings.WIDTH * GRID_LEFT_RATIO + column * cellSize();
    }

    /** Vertically centres the 5x5 grid on the screen. */
    public static float originY(int line) {
        float cell = cellSize();
        return Settings.HEIGHT * 0.5f + 2.5f * cell - (line + 1) * cell;
    }

    public static float centerX(float cellOrigin) {
        return cellOrigin + cellSize() * 0.5f;
    }

    public static float centerY(float cellOrigin) {
        return cellOrigin + cellSize() * 0.5f;
    }

    /**
     * Shrinks (and as a last resort truncates) a task description until it fits inside its cell.
     *
     * Measured with {@link FontHelper#getSmartHeight}, i.e. the same routine the renderer uses, so
     * " NL " breaks, wrapping and colour markup are accounted for exactly. The unmodified factor is
     * 1.0, which reproduces the parent mod's look whenever the text already fits.
     */
    public static Fitted fit(String text, float usableWidth, float usableHeight, float lineSpacing) {
        if (text == null) {
            text = "";
        }
        String key = text + '|' + (int) usableWidth + '|' + (int) usableHeight + '|'
                + (int) (lineSpacing * 100.0f) + '|' + (int) (Settings.scale * 1000.0f);
        Fitted cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        BitmapFont font = FontHelper.tipBodyFont;
        float base = font.getData().scaleX;
        if (base <= 0.0f) {
            base = 1.0f;
        }

        String draw = text;
        float factor = 1.0f;
        float spacing = lineSpacing;
        try {
            boolean fits = false;
            for (factor = 1.0f; factor >= MIN_TEXT_SCALE - 0.0001f; factor -= SCALE_STEP) {
                spacing = lineSpacing * factor;
                if (measure(font, text, usableWidth, spacing, base * factor) <= usableHeight) {
                    fits = true;
                    break;
                }
            }
            if (!fits) {
                factor = MIN_TEXT_SCALE;
                spacing = lineSpacing * factor;
                float scaled = base * factor;
                while (draw.length() > 1
                        && measure(font, draw, usableWidth, spacing, scaled) > usableHeight) {
                    draw = draw.substring(0, draw.length() - 1);
                }
            }
        } finally {
            font.getData().setScale(base);
        }

        Fitted result = new Fitted(draw, base * factor, spacing);
        if (CACHE.size() > CACHE_LIMIT) {
            CACHE.clear();
        }
        CACHE.put(key, result);
        return result;
    }

    private static float measure(BitmapFont font, String text, float width, float spacing, float scale) {
        float previous = font.getData().scaleX;
        try {
            font.getData().setScale(scale);
            return FontHelper.getSmartHeight(font, text, width, spacing);
        } finally {
            font.getData().setScale(previous);
        }
    }

    /** Text to draw plus the exact font scale and line spacing it must be drawn with. */
    public static final class Fitted {
        public final String text;
        public final float scale;
        public final float lineSpacing;

        Fitted(String text, float scale, float lineSpacing) {
            this.text = text;
            this.scale = scale;
            this.lineSpacing = lineSpacing;
        }
    }
}
