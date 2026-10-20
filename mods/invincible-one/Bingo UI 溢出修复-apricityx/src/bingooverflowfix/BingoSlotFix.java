package bingooverflowfix;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import demoMod.invincibleOne.InvincibleOneMod;
import demoMod.invincibleOne.relics.TaskGoal;
import demoMod.invincibleOne.ui.BingoItem;
import demoMod.invincibleOne.ui.BingoSlot;

/**
 * A {@link BingoSlot} replacement whose layout and text cannot overflow the screen.
 *
 * The parent's own slots are swapped for this class inside the live list (see
 * {@link BingoOverflowFix}), so the parent's {@code BingoPanel} calls these overrides and every
 * gameplay rule of the mod keeps running unchanged.
 *
 * Fixed here:
 * <ul>
 *   <li>the cell size, which the parent derives from {@code Settings.WIDTH} only, making the 5x5 grid
 *       0.5 * WIDTH tall and pushing the first and last rows off screen on wide phones;</li>
 *   <li>the description, which the parent renders at a fixed line height without clipping, so long
 *       task lines spill into the cell below;</li>
 *   <li>the hover side effect that overwrote {@code targetX} after the lerp had already run, which
 *       snapped the cell towards the left edge for a frame;</li>
 *   <li>the hitbox, which the parent sized to {@code 0.1 * WIDTH} and centred half a cell away from
 *       the drawn frame.</li>
 * </ul>
 *
 * The parent's {@code x} / {@code y} / {@code targetX} / {@code targetY} are private, so this class
 * keeps its own copies; both methods that touch them are overridden, so the parent's copies are never
 * read while this class is installed.
 */
public class BingoSlotFix extends BingoSlot {

    private static final float PADDING_RATIO = 0.06f;
    private static final float BASE_LINE_SPACING = 26.0f;

    private float fixX;
    private float fixY;
    private float fixTargetX;
    private float fixTargetY;

    public BingoSlotFix(BingoItem item) {
        super(item);
    }

    /** Used when swapping a live slot for a fixed one, so nothing pops. */
    public void snapTo(float x, float y) {
        this.fixX = x;
        this.fixY = y;
        this.fixTargetX = x;
        this.fixTargetY = y;
    }

    @Override
    public void update() {
        // The parent recomputes the index every frame, so slots stay correct after a save reload.
        int index = InvincibleOneMod.bingoPanel.getSlots().indexOf(this);
        if (index < 0) {
            index = 0;
        }
        boolean hidden = InvincibleOneMod.bingoPanel.hide;

        this.fixTargetX = BingoLayout.originX(index % 5);
        this.fixTargetY = BingoLayout.originY(index / 5);

        if (hidden) {
            if (AbstractDungeon.player != null && AbstractDungeon.player.hasRelic(TaskGoal.ID)) {
                AbstractRelic relic = AbstractDungeon.player.getRelic(TaskGoal.ID);
                this.fixTargetX = relic.currentX;
                this.fixTargetY = relic.currentY;
            } else {
                this.fixTargetX = 0.0f;
                this.fixTargetY = 0.0f;
            }
        }

        this.fixX = MathHelper.orbLerpSnap(this.fixX, this.fixTargetX);
        this.fixY = MathHelper.orbLerpSnap(this.fixY, this.fixTargetY);

        float cell = BingoLayout.cellSize();
        if (this.hb != null) {
            this.hb.resize(cell, cell);
            this.hb.move(BingoLayout.centerX(this.fixX), BingoLayout.centerY(this.fixY));
            this.hb.update();
        }

        BingoItem item = this.getBingoItem();
        if (item == null) {
            return;
        }

        if (!hidden && this.hb != null && this.hb.hovered && InputHelper.justClickedLeft) {
            CardCrawlGame.sound.playA("UI_CLICK_1", -0.1f);
            this.hb.clickStarted = true;
            if (canInteract && item.getStatus() == 0 && !BingoOverflowFix.isRewardSlot(item)) {
                item.completeBingo();
                canInteract = false;
                InvincibleOneMod.bingoPanel.hide = true;
            }
        }

        item.update();
    }

    @Override
    public void render(SpriteBatch sb, float alpha) {
        BingoItem item = this.getBingoItem();
        if (sb == null || item == null) {
            return;
        }

        float cell = BingoLayout.cellSize();
        float scale = Settings.scale;
        // The decorative caps stay proportional when the cell is shorter than 32px * scale wants.
        float cap = Math.min(32.0f * scale, cell * 0.26f);
        float bodyHeight = Math.max(cell - cap * 2.0f, 1.0f);

        sb.setColor(1.0f, 1.0f, 1.0f, alpha);
        sb.draw(ImageMaster.KEYWORD_TOP, this.fixX, this.fixY + cell - cap, cell, cap);
        sb.draw(ImageMaster.KEYWORD_BODY, this.fixX, this.fixY + cap, cell, bodyHeight);
        sb.draw(ImageMaster.KEYWORD_BOT, this.fixX, this.fixY, cell, cap);

        Color fontColor;
        switch (item.getStatus()) {
            case 1:
                fontColor = Color.CHARTREUSE.cpy();
                break;
            case 2:
                fontColor = Color.RED.cpy();
                break;
            default:
                fontColor = Color.WHITE.cpy();
                break;
        }
        fontColor.a = alpha;

        float padding = Math.max(cell * PADDING_RATIO, 4.0f * scale);
        float usableWidth = Math.max(cell - padding * 3.0f, 1.0f);
        float usableHeight = Math.max(cell - padding * 2.0f, 1.0f);

        BingoLayout.Fitted fitted = BingoLayout.fit(
                item.getDescription(), usableWidth, usableHeight, BASE_LINE_SPACING * scale);

        BitmapFont font = FontHelper.tipBodyFont;
        float original = font.getData().scaleX;
        try {
            font.getData().setScale(fitted.scale);
            FontHelper.renderSmartText(sb, font, fitted.text,
                    this.fixX + padding, this.fixY + cell - padding,
                    usableWidth, fitted.lineSpacing, fontColor);
        } finally {
            font.getData().setScale(original);
        }

        if (this.hb != null) {
            this.hb.render(sb);
        }
    }
}
