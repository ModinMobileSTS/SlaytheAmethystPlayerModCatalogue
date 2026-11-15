package annualmemberchoice;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.relics.MembershipCard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * State and drawing for the "claim the membership card?" prompt that is shown in the
 * Neow room at the start of every run.
 */
public final class MembershipChoice {

    private static final Logger logger = LogManager.getLogger("AnnualMemberChoice");

    /** Vanilla shop relic that the parent mod used to hand out automatically. */
    private static final String RELIC_ID = "Membership Card";

    private static final Color DIM = new Color(0.0f, 0.0f, 0.0f, 0.72f);
    private static final Color PANEL_BG = new Color(0.055f, 0.075f, 0.105f, 0.97f);
    private static final Color BODY_TEXT = new Color(0.86f, 0.89f, 0.94f, 1.0f);
    private static final Color SKIP_BORDER = new Color(0.45f, 0.52f, 0.62f, 1.0f);
    private static final Color SKIP_BORDER_HOVER = new Color(0.78f, 0.85f, 0.93f, 1.0f);
    private static final Color CLAIM_BORDER_HOVER = new Color(1.0f, 0.93f, 0.62f, 1.0f);
    private static final Color CLAIM_FILL = new Color(0.21f, 0.17f, 0.09f, 1.0f);
    private static final Color CLAIM_FILL_HOVER = new Color(0.33f, 0.26f, 0.12f, 1.0f);
    private static final Color SKIP_FILL = new Color(0.11f, 0.14f, 0.19f, 1.0f);
    private static final Color SKIP_FILL_HOVER = new Color(0.17f, 0.22f, 0.29f, 1.0f);
    private static final Color SKIP_TEXT = new Color(0.86f, 0.90f, 0.95f, 1.0f);

    /** The parent mod's automatic grant is skipped while this is set. */
    public static boolean armed = false;
    /** The prompt is currently on screen and swallowing input. */
    public static boolean active = false;
    /** The player already answered during this run. */
    public static boolean resolved = false;
    /** The Neow room of this run has already been inspected. */
    public static boolean considered = false;
    /** True only for a run that was started from scratch during this game session. */
    private static boolean newRun = false;

    private static float panelX;
    private static float panelY;
    private static float panelW;
    private static float panelH;
    private static float claimX;
    private static float claimY;
    private static float claimW;
    private static float claimH;
    private static float skipX;
    private static float skipY;
    private static float skipW;
    private static float skipH;

    private MembershipChoice() {
    }

    /** Called while the game builds the starter relic list of a new run. */
    public static void armForRun() {
        armed = true;
    }

    /** Called from the AbstractDungeon constructor of a brand new run. */
    public static void resetForRun() {
        armed = false;
        active = false;
        resolved = false;
        considered = false;
        newRun = true;
    }

    /**
     * True when a relic that is just being obtained is the parent mod's automatic gift.
     * Only {@link #armed} is checked here: it is set inside initializeStarterRelics itself,
     * so it cannot be stale, while {@link #resolved} may still belong to the previous run.
     */
    public static boolean shouldSuppress(AbstractRelic relic) {
        if (relic == null || !armed) {
            return false;
        }
        if (!RELIC_ID.equals(relic.relicId)) {
            return false;
        }
        logger.info("[AnnualMemberChoice] intercepted the automatic membership card");
        return true;
    }

    /** Decides once per run whether the prompt should be shown. */
    public static void consider() {
        if (considered) {
            return;
        }
        considered = true;
        if (AbstractDungeon.player == null || resolved
                || AbstractDungeon.player.hasRelic(RELIC_ID)) {
            armed = false;
            return;
        }
        active = true;
        logger.info("[AnnualMemberChoice] asking the player whether to claim the membership card");
    }

    /** Per-frame input handling, called from the head of AbstractDungeon.update. */
    public static void tick() {
        if (!active) {
            fallbackIfPromptNeverShown();
            if (armed && AbstractDungeon.floorNum > 1) {
                // Run is past its start, so the automatic grant can no longer happen.
                armed = false;
            }
            return;
        }
        layout();
        boolean claimHovered = over(claimX, claimY, claimW, claimH);
        boolean skipHovered = over(skipX, skipY, skipW, skipH);
        if (InputHelper.justClickedLeft) {
            if (claimHovered) {
                resolve(true);
            } else if (skipHovered) {
                resolve(false);
            }
        } else if (InputHelper.pressedEscape) {
            resolve(false);
        }
        // Starve the dungeon, the Neow dialog and the top panel of input while the prompt is up.
        InputHelper.justClickedLeft = false;
        InputHelper.justClickedRight = false;
        InputHelper.justReleasedClickLeft = false;
        InputHelper.justReleasedClickRight = false;
        InputHelper.scrolledUp = false;
        InputHelper.scrolledDown = false;
        InputHelper.pressedEscape = false;
    }

    /**
     * Safety net: if the Neow room never got the chance to ask (another mod replaced it, the
     * prompt could not be drawn...), behave like the unpatched parent mod and hand the card
     * out once the first real floor is entered.
     */
    private static void fallbackIfPromptNeverShown() {
        if (!newRun || considered || resolved || AbstractDungeon.floorNum < 1) {
            return;
        }
        considered = true;
        armed = false;
        resolved = true;
        if (AbstractDungeon.player != null && !AbstractDungeon.player.hasRelic(RELIC_ID)) {
            logger.info("[AnnualMemberChoice] prompt never shown, falling back to the original auto grant");
            grant();
        }
    }

    private static void resolve(boolean claim) {
        active = false;
        resolved = true;
        armed = false;
        if (claim) {
            grant();
        } else {
            logger.info("[AnnualMemberChoice] player declined the membership card");
        }
    }

    private static void grant() {
        if (AbstractDungeon.player == null) {
            return;
        }
        if (AbstractDungeon.player.hasRelic(RELIC_ID)) {
            return;
        }
        try {
            MembershipCard card = new MembershipCard();
            card.instantObtain(AbstractDungeon.player, AbstractDungeon.player.relics.size(), false);
            logger.info("[AnnualMemberChoice] membership card granted");
        } catch (Throwable t) {
            logger.error("[AnnualMemberChoice] failed to grant the membership card", t);
        }
    }

    /** Draws the prompt; hooked at the tail of AbstractDungeon.render. */
    public static void render(SpriteBatch sb) {
        if (!active || AbstractDungeon.player == null) {
            return;
        }
        layout();
        float s = Settings.scale;
        float cx = Settings.WIDTH / 2.0f;

        sb.setColor(DIM);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f,
                (float) Settings.WIDTH, (float) Settings.HEIGHT);

        float border = 3.0f * s;
        sb.setColor(Settings.GOLD_COLOR);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, panelX - border, panelY - border,
                panelW + border * 2.0f, panelH + border * 2.0f);

        sb.setColor(PANEL_BG);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, panelX, panelY, panelW, panelH);

        sb.setColor(new Color(0.85f, 0.72f, 0.35f, 0.8f));
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, panelX + panelW * 0.2f, panelY + panelH * 0.70f,
                panelW * 0.6f, 2.0f * s);

        sb.setColor(Color.WHITE);
        FontHelper.renderFontCentered(sb, FontHelper.cardTitleFont, titleText(), cx,
                panelY + panelH * 0.84f, Settings.GOLD_COLOR, 1.35f);
        FontHelper.renderFontCentered(sb, FontHelper.cardTitleFont, questionText(), cx,
                panelY + panelH * 0.56f, Color.WHITE, 1.0f);
        FontHelper.renderFontCentered(sb, FontHelper.cardTitleFont, effectText(), cx,
                panelY + panelH * 0.44f, BODY_TEXT, 0.9f);

        drawButton(sb, claimX, claimY, claimW, claimH, claimLabel(),
                over(claimX, claimY, claimW, claimH), true);
        drawButton(sb, skipX, skipY, skipW, skipH, skipLabel(),
                over(skipX, skipY, skipW, skipH), false);

        sb.setColor(Color.WHITE);
        FontHelper.cardTitleFont.setColor(Color.WHITE);
    }

    private static void drawButton(SpriteBatch sb, float x, float y, float w, float h,
            String label, boolean hovered, boolean primary) {
        float s = Settings.scale;
        Color border;
        if (primary) {
            border = hovered ? CLAIM_BORDER_HOVER : Settings.GOLD_COLOR;
        } else {
            border = hovered ? SKIP_BORDER_HOVER : SKIP_BORDER;
        }
        sb.setColor(border);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x, y, w, h);

        Color fill;
        if (primary) {
            fill = hovered ? CLAIM_FILL_HOVER : CLAIM_FILL;
        } else {
            fill = hovered ? SKIP_FILL_HOVER : SKIP_FILL;
        }
        sb.setColor(fill);
        float inset = 2.0f * s;
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x + inset, y + inset,
                w - inset * 2.0f, h - inset * 2.0f);

        sb.setColor(Color.WHITE);
        Color textColor;
        if (hovered) {
            textColor = Color.WHITE;
        } else {
            textColor = primary ? Settings.GOLD_COLOR : SKIP_TEXT;
        }
        FontHelper.renderFontCentered(sb, FontHelper.cardTitleFont, label,
                x + w / 2.0f, y + h / 2.0f, textColor, 0.95f);
        sb.setColor(Color.WHITE);
    }

    private static void layout() {
        float s = Settings.scale;
        panelW = Math.min((float) Settings.WIDTH * 0.9f, 1180.0f * s);
        panelH = Math.min((float) Settings.HEIGHT * 0.72f, 620.0f * s);
        float cx = Settings.WIDTH / 2.0f;
        panelX = cx - panelW / 2.0f;
        panelY = Settings.HEIGHT / 2.0f - panelH / 2.0f;

        claimW = panelW * 0.34f;
        claimH = 78.0f * s;
        claimX = cx - claimW - 18.0f * s;
        claimY = panelY + panelH * 0.10f;

        skipW = claimW;
        skipH = claimH;
        skipX = cx + 18.0f * s;
        skipY = claimY;
    }

    private static boolean over(float x, float y, float w, float h) {
        float mx = InputHelper.mX;
        float my = InputHelper.mY;
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static boolean chinese() {
        Settings.GameLanguage language = Settings.language;
        return language == Settings.GameLanguage.ZHS || language == Settings.GameLanguage.ZHT;
    }

    private static String titleText() {
        return chinese() ? "\u5e74\u5ea6\u5927\u4f1a\u5458" : "Annual Membership";
    }

    private static String questionText() {
        return chinese()
                ? "\u662f\u5426\u9886\u53d6\u300c\u4f1a\u5458\u5361\u300d\uff1f"
                : "Claim the Membership Card?";
    }

    private static String effectText() {
        return chinese()
                ? "\u5546\u5e97\u4e2d\u6240\u6709\u5546\u54c1\u4ef7\u683c\u51cf\u534a\u3002"
                : "All shop prices are reduced by 50%.";
    }

    private static String claimLabel() {
        return chinese() ? "\u9886\u53d6" : "Claim";
    }

    private static String skipLabel() {
        return chinese() ? "\u4e0d\u9886\u53d6" : "Skip";
    }
}
