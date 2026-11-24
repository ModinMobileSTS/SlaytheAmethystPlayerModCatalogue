package therailgunfix;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.helpers.ImageMaster;

final class RailgunCards {
    private RailgunCards() {
    }

    static boolean isRailgunCard(AbstractCard card) {
        return card != null && card.cardID != null && card.cardID.startsWith("TheRailgun:");
    }

    static void drawPanel(SpriteBatch sb, float centerX, float centerY, float width, float height, float angle) {
        Color previous = sb.getColor().cpy();
        sb.setColor(Color.BLACK);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG,
                centerX - width / 2f, centerY - height / 2f,
                width / 2f, height / 2f,
                width, height,
                1f, 1f, angle,
                0, 0, 1, 1,
                false, false);
        sb.setColor(previous);
    }
}
