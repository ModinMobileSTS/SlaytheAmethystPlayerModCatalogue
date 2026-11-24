package neowpreview;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PotionHelper;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 「拿到手才知道结果」类遗物的悬停预览：悬停遗物时直接画出它实际会给出的东西。
 *
 * <p>覆盖：磨刀石（升级两张攻击牌）、战纹涂料（升级两张技能牌）、潘多拉之盒（替换所有初始打击／防御）、
 * 召唤铃（普通／罕见／稀有遗物各一件）、坩埚（五瓶随机药水）、天球仪（四组卡牌奖励）、
 * 小房子（随机升级一张牌 + 一瓶随机药水）。</p>
 *
 * <p>所有推算只用随机数副本（miscRng / cardRng / cardRandomRng / potionRng / relicRng）与遗物池副本，
 * 不消耗真随机数、也不动真的池子，算完立刻还原。结果按「遗物实例 + 状态串」缓存，
 * 悬停期间不重复计算。</p>
 */
public final class RelicUpgradePreview {

    private static final String WHETSTONE = "Whetstone";
    private static final String WAR_PAINT = "War Paint";
    private static final String PANDORA = "Pandora's Box";
    private static final String CALLING_BELL = "Calling Bell";
    private static final String CAULDRON = "Cauldron";
    private static final String ORRERY = "Orrery";
    private static final String TINY_HOUSE = "Tiny House";

    private RelicUpgradePreview() {
    }

    /** 这个遗物能不能预测。 */
    public static boolean handles(AbstractRelic relic) {
        if (relic == null || relic.relicId == null) {
            return false;
        }
        String id = relic.relicId;
        return WHETSTONE.equals(id) || WAR_PAINT.equals(id) || PANDORA.equals(id)
                || CALLING_BELL.equals(id) || CAULDRON.equals(id) || ORRERY.equals(id)
                || TINY_HOUSE.equals(id);
    }

    /**
     * 悬停遗物时调用。
     *
     * @param anchorX      遗物图标中心 X
     * @param anchorTopY   遗物图标上边
     * @param anchorBottomY 遗物图标下边
     */
    public static void render(SpriteBatch sb, AbstractRelic relic, float anchorX, float anchorTopY, float anchorBottomY) {
        if (sb == null || AbstractDungeon.player == null || !handles(relic)) {
            return;
        }
        // 已经拿到手了就不再预测（顶栏悬停自己已有的遗物不显示）
        if (AbstractDungeon.player.hasRelic(relic.relicId)) {
            return;
        }
        Payload payload = payload(relic);
        if (payload.isEmpty()) {
            return;
        }
        if (payload.relics.isEmpty() && payload.potions.isEmpty() && payload.cards.size() <= 2) {
            drawCardsNearRelic(sb, payload.cards, anchorX, anchorTopY, anchorBottomY);
        } else {
            drawCentered(sb, payload);
        }
    }

    // ------------------------------------------------------------------ 结果推算

    private static Payload payload(AbstractRelic relic) {
        String key = relic.relicId + "|" + stateKey();
        return PreviewCache.CARDS.get(relic, key, () -> compute(relic));
    }

    /** 状态串：任一随机数计数器、牌组或遗物数量变了就重算。 */
    static String stateKey() {
        StringBuilder sb = new StringBuilder();
        sb.append(counter(AbstractDungeon.miscRng)).append(',')
                .append(counter(AbstractDungeon.cardRng)).append(',')
                .append(counter(AbstractDungeon.cardRandomRng)).append(',')
                .append(counter(AbstractDungeon.potionRng)).append(',')
                .append(counter(AbstractDungeon.relicRng)).append(',')
                .append(AbstractDungeon.player == null ? 0 : AbstractDungeon.player.relics.size()).append(',')
                .append(deckKey());
        return sb.toString();
    }

    private static int counter(Random rng) {
        try {
            return rng == null ? -1 : rng.counter;
        } catch (Throwable t) {
            return -1;
        }
    }

    private static String deckKey() {
        if (AbstractDungeon.player == null || AbstractDungeon.player.masterDeck == null) {
            return "0";
        }
        int size = AbstractDungeon.player.masterDeck.size();
        int upgradable = 0;
        for (AbstractCard card : AbstractDungeon.player.masterDeck.group) {
            if (card != null && card.canUpgrade()) {
                ++upgradable;
            }
        }
        return size + ":" + upgradable;
    }

    private static Payload compute(AbstractRelic relic) {
        Payload payload = new Payload();
        if (relic == null) {
            return payload;
        }
        try {
            String id = relic.relicId;
            if (WHETSTONE.equals(id)) {
                upgradeCards(payload, AbstractCard.CardType.ATTACK, 2);
            } else if (WAR_PAINT.equals(id)) {
                upgradeCards(payload, AbstractCard.CardType.SKILL, 2);
            } else if (PANDORA.equals(id)) {
                pandora(payload);
            } else if (CALLING_BELL.equals(id)) {
                callingBell(payload);
            } else if (CAULDRON.equals(id)) {
                payload.potions.addAll(NeowPredictor.randomPotions(5));
            } else if (ORRERY.equals(id)) {
                orrery(payload);
            } else if (TINY_HOUSE.equals(id)) {
                tinyHouse(payload);
            }
        } catch (Throwable ignored) {
            // 预测失败只放弃显示，不影响游戏本身
        }
        return payload;
    }

    /**
     * 复刻 Whetstone / WarPaint 的 onEquip：挑出还能升级的攻击牌／技能牌，
     * 用 {@code new Random(miscRng.randomLong())} 洗一遍，升级前两张。
     */
    private static void upgradeCards(Payload payload, AbstractCard.CardType type, int amount) {
        if (AbstractDungeon.player == null || AbstractDungeon.player.masterDeck == null) {
            return;
        }
        List<AbstractCard> upgradable = new ArrayList<>();
        for (AbstractCard card : AbstractDungeon.player.masterDeck.group) {
            if (card != null && card.canUpgrade() && card.type == type) {
                upgradable.add(card);
            }
        }
        if (upgradable.isEmpty()) {
            return;
        }
        long seed;
        try {
            Random rng = AbstractDungeon.miscRng;
            if (rng == null) {
                return;
            }
            seed = rng.copy().randomLong();
        } catch (Throwable t) {
            return;
        }
        Collections.shuffle(upgradable, new java.util.Random(seed));
        int take = Math.min(upgradable.size(), amount);
        for (int i = 0; i < take; ++i) {
            AbstractCard card = upgradable.get(i).makeStatEquivalentCopy();
            if (card != null && card.canUpgrade()) {
                card.upgrade();
            }
            if (card != null) {
                payload.cards.add(card);
            }
        }
    }

    /** 复刻 PandorasBox.onEquip：把每张初始打击／防御换成 {@code returnTrulyRandomCard()}（走 cardRandomRng）。 */
    private static void pandora(Payload payload) {
        if (AbstractDungeon.player == null || AbstractDungeon.player.masterDeck == null) {
            return;
        }
        int count = 0;
        for (AbstractCard card : AbstractDungeon.player.masterDeck.group) {
            if (card != null && (card.hasTag(AbstractCard.CardTags.STARTER_STRIKE)
                    || card.hasTag(AbstractCard.CardTags.STARTER_DEFEND))) {
                ++count;
            }
        }
        if (count <= 0) {
            return;
        }
        Random saved = AbstractDungeon.cardRandomRng;
        if (saved == null) {
            return;
        }
        AbstractDungeon.cardRandomRng = saved.copy();
        try {
            for (int i = 0; i < count; ++i) {
                AbstractCard card = AbstractDungeon.returnTrulyRandomCard();
                if (card != null) {
                    payload.cards.add(card.makeCopy());
                }
            }
        } catch (Throwable ignored) {
        } finally {
            AbstractDungeon.cardRandomRng = saved;
        }
    }

    /** 复刻 CallingBell：普通／罕见／稀有各一件无界面遗物（固定稀有度，不消耗 relicRng）。 */
    private static void callingBell(Payload payload) {
        AbstractRelic common = NeowPredictor.screenlessRelic(AbstractRelic.RelicTier.COMMON);
        AbstractRelic uncommon = NeowPredictor.screenlessRelic(AbstractRelic.RelicTier.UNCOMMON);
        AbstractRelic rare = NeowPredictor.screenlessRelic(AbstractRelic.RelicTier.RARE);
        if (common != null) {
            payload.relics.add(common);
        }
        if (uncommon != null) {
            payload.relics.add(uncommon);
        }
        if (rare != null) {
            payload.relics.add(rare);
        }
    }

    /** 复刻 Orrery.onEquip：连续四组卡牌奖励（{@code getRewardCards()} ×4，走 cardRng）。 */
    private static void orrery(Payload payload) {
        Random saved = AbstractDungeon.cardRng;
        if (saved == null) {
            return;
        }
        AbstractDungeon.cardRng = saved.copy();
        try {
            for (int i = 0; i < 4; ++i) {
                ArrayList<AbstractCard> cards = AbstractDungeon.getRewardCards();
                if (cards == null || cards.isEmpty()) {
                    break;
                }
                for (AbstractCard card : cards) {
                    if (card != null) {
                        payload.cards.add(card.makeCopy());
                    }
                }
            }
        } catch (Throwable ignored) {
        } finally {
            AbstractDungeon.cardRng = saved;
        }
    }

    /**
     * 复刻 TinyHouse.onEquip：先 {@code new Random(miscRng.randomLong())} 洗可升级牌并升级第一张
     * （原版多张时也只升 get(0)），再用同一个 miscRng 副本抽一瓶药水。
     */
    private static void tinyHouse(Payload payload) {
        if (AbstractDungeon.player == null || AbstractDungeon.player.masterDeck == null) {
            return;
        }
        Random rng;
        try {
            rng = AbstractDungeon.miscRng == null ? null : AbstractDungeon.miscRng.copy();
        } catch (Throwable t) {
            rng = null;
        }
        if (rng == null) {
            return;
        }
        List<AbstractCard> upgradable = new ArrayList<>();
        for (AbstractCard card : AbstractDungeon.player.masterDeck.group) {
            if (card != null && card.canUpgrade()) {
                upgradable.add(card);
            }
        }
        long seed = rng.randomLong();
        if (!upgradable.isEmpty()) {
            Collections.shuffle(upgradable, new java.util.Random(seed));
            AbstractCard card = upgradable.get(0).makeStatEquivalentCopy();
            if (card != null && card.canUpgrade()) {
                card.upgrade();
            }
            if (card != null) {
                payload.cards.add(card);
            }
        }
        try {
            AbstractPotion potion = PotionHelper.getRandomPotion(rng);
            if (potion != null) {
                payload.potions.add(potion);
            }
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------------ 绘制

    /** 一到两张牌：贴着遗物图标画，上方放不下就画到下方。 */
    private static void drawCardsNearRelic(SpriteBatch sb, List<AbstractCard> cards, float anchorX,
                                           float anchorTopY, float anchorBottomY) {
        int count = cards.size();
        if (count <= 0) {
            return;
        }
        float gap = 10.0f * Settings.scale;
        float maxW = Settings.WIDTH * 0.8f;
        float imageScale = Config.imageScale();
        float fit = (maxW - (count - 1) * gap) / (count * AbstractCard.IMG_WIDTH);
        float scale = Math.min(imageScale, Math.max(0.2f, fit));
        float w = AbstractCard.IMG_WIDTH * scale;
        float h = AbstractCard.IMG_HEIGHT * scale;
        float totalW = count * w + (count - 1) * gap;
        float margin = 16.0f * Settings.scale;
        float left = anchorX - totalW * 0.5f;
        left = Math.max(margin, Math.min(left, Settings.WIDTH - margin - totalW));
        float cy;
        if (anchorTopY + 14.0f * Settings.scale + h <= Settings.HEIGHT - margin) {
            cy = anchorTopY + 14.0f * Settings.scale + h * 0.5f;
        } else {
            cy = Math.max(h * 0.5f + margin, anchorBottomY - 14.0f * Settings.scale - h * 0.5f);
        }
        sb.setColor(Color.WHITE);
        for (int i = 0; i < count; ++i) {
            AbstractCard card = cards.get(i);
            if (card == null) {
                continue;
            }
            card.current_x = left + w * 0.5f + i * (w + gap);
            card.current_y = cy;
            card.drawScale = scale;
            card.targetDrawScale = scale;
            card.angle = 0.0f;
            card.render(sb);
        }
    }

    /**
     * 结果较大（潘多拉之盒、天球仪、召唤铃、坩埚、小房子）：画在屏幕偏上方，
     * 按宽度折行、必要时等比缩小，下方三分之一留给遗物图标和按钮（不被挡住）。
     */
    static void drawCentered(SpriteBatch sb, Payload payload) {
        float marginX = Settings.WIDTH * 0.06f;
        float maxW = Settings.WIDTH - marginX * 2.0f;
        float maxH = Settings.HEIGHT * 0.60f;
        float blockCenterY = Settings.HEIGHT * 0.68f;
        float gap = 10.0f * Settings.scale;
        int cardCount = payload.cards.size();
        int iconCount = payload.relics.size() + payload.potions.size();
        float scale = Config.imageScale();
        int perRow = 1;
        int rows = 0;
        for (int attempt = 0; attempt < 8; ++attempt) {
            perRow = perRow(cardCount, scale, maxW, gap);
            rows = cardCount == 0 ? 0 : (cardCount + perRow - 1) / perRow;
            float needW = Math.max(rowWidth(Math.min(cardCount, perRow), scale, gap),
                    iconRowWidth(iconCount, scale, gap));
            float needH = blockHeight(rows, iconCount, scale, gap);
            if ((needW <= maxW && needH <= maxH) || scale <= 0.28f) {
                break;
            }
            float fit = Math.min(needW <= 0.0f ? 1.0f : maxW / needW, needH <= 0.0f ? 1.0f : maxH / needH);
            scale = Math.max(0.28f, scale * fit * 0.98f);
        }
        float cardW = AbstractCard.IMG_WIDTH * scale;
        float cardH = AbstractCard.IMG_HEIGHT * scale;
        float iconSide = 120.0f * scale;
        float centerX = Settings.WIDTH * 0.5f;
        float y = blockCenterY + blockHeight(rows, iconCount, scale, gap) * 0.5f;
        sb.setColor(Color.WHITE);
        if (cardCount > 0) {
            int index = 0;
            for (int row = 0; row < rows; ++row) {
                int inRow = Math.min(perRow, cardCount - index);
                float rowW = inRow * cardW + (inRow - 1) * gap;
                float x = centerX - rowW * 0.5f;
                float cy = y - cardH * 0.5f;
                for (int i = 0; i < inRow; ++i) {
                    AbstractCard card = payload.cards.get(index++);
                    if (card != null) {
                        card.current_x = x + cardW * 0.5f;
                        card.current_y = cy;
                        card.drawScale = scale;
                        card.targetDrawScale = scale;
                        card.angle = 0.0f;
                        card.render(sb);
                    }
                    x += cardW + gap;
                }
                y -= cardH + gap;
            }
        }
        if (iconCount > 0) {
            float x = centerX - iconRowWidth(iconCount, scale, gap) * 0.5f;
            float cy = y - iconSide * 0.5f;
            for (AbstractRelic relic : payload.relics) {
                drawRelicIcon(sb, relic, x + iconSide * 0.5f, cy, iconSide);
                x += iconSide + gap;
            }
            for (AbstractPotion potion : payload.potions) {
                drawPotionIcon(sb, potion, x + iconSide * 0.5f, cy, iconSide);
                x += iconSide + gap;
            }
        }
    }

    private static int perRow(int cardCount, float scale, float maxW, float gap) {
        if (cardCount <= 0) {
            return 1;
        }
        float cardW = AbstractCard.IMG_WIDTH * scale;
        int fits = (int) Math.floor((maxW + gap) / (cardW + gap));
        return Math.max(1, Math.min(cardCount, fits));
    }

    private static float rowWidth(int count, float scale, float gap) {
        if (count <= 0) {
            return 0.0f;
        }
        return count * AbstractCard.IMG_WIDTH * scale + (count - 1) * gap;
    }

    private static float iconRowWidth(int iconCount, float scale, float gap) {
        if (iconCount <= 0) {
            return 0.0f;
        }
        return iconCount * 120.0f * scale + (iconCount - 1) * gap;
    }

    private static float blockHeight(int rows, int iconCount, float scale, float gap) {
        float h = rows * AbstractCard.IMG_HEIGHT * scale;
        if (rows > 1) {
            h += (rows - 1) * gap;
        }
        if (iconCount > 0) {
            if (rows > 0) {
                h += gap;
            }
            h += 120.0f * scale;
        }
        return h;
    }

    private static void drawRelicIcon(SpriteBatch sb, AbstractRelic relic, float centerX, float centerY, float side) {
        if (relic == null || relic.img == null) {
            return;
        }
        sb.setColor(Color.WHITE);
        sb.draw(relic.img, centerX - side * 0.5f, centerY - side * 0.5f, side, side);
    }

    private static void drawPotionIcon(SpriteBatch sb, AbstractPotion potion, float centerX, float centerY, float side) {
        if (potion == null) {
            return;
        }
        potion.posX = centerX;
        potion.posY = centerY;
        potion.scale = side / 64.0f;
        potion.hb.move(centerX, centerY);
        sb.setColor(Color.WHITE);
        potion.render(sb);
    }

    /** 一次预测的结果：牌面、遗物图标、药水图标。 */
    static final class Payload {
        final List<AbstractCard> cards = new ArrayList<>();
        final List<AbstractRelic> relics = new ArrayList<>();
        final List<AbstractPotion> potions = new ArrayList<>();

        boolean isEmpty() {
            return cards.isEmpty() && relics.isEmpty() && potions.isEmpty();
        }
    }
}
