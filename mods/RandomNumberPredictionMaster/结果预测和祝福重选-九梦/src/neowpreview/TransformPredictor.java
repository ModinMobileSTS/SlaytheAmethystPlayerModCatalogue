package neowpreview;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.events.city.DrugDealer;
import com.megacrit.cardcrawl.events.shrines.Designer;
import com.megacrit.cardcrawl.events.shrines.Transmogrifier;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.neow.NeowEvent;
import com.megacrit.cardcrawl.neow.NeowReward;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.relics.Astrolabe;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.screens.select.GridCardSelectScreen;

import java.util.List;

/**
 * 变牌（Transform）结果预览。
 *
 * <p>原版在“选择要变形的牌”确认界面里，右侧那张牌每 0.1 秒随机换一张，纯属占位动画。
 * 这里用随机数副本复刻 {@code AbstractDungeon.transformCard}，把那张牌直接换成真正会得到的牌；
 * 对于没有确认界面（多选、阿斯特罗莱布等）的变形，则在网格界面右下角显示同样的结果。</p>
 *
 * <p>推算全程只读随机数与卡池，不消耗真正的随机数。</p>
 */
public final class TransformPredictor {

    /** 当前这次网格选牌是星盘（阿斯特罗莱布）发起的变形：固定用 miscRng，并且自动升级。 */
    private static boolean astrolabeGrid = false;

    private static AbstractCard cachedOrigin = null;
    private static int cachedSelected = -1;
    private static AbstractCard cachedResult = null;

    private TransformPredictor() {
    }

    // ------------------------------------------------------------------ 状态

    /** 网格选牌界面打开：判断这次是不是星盘发起的（只读界面信息，不改原版星盘代码）。 */
    public static void onGridOpened(GridCardSelectScreen grid) {
        astrolabeGrid = isAstrolabeGrid(grid);
        clearCache();
    }

    /**
     * 星盘的变形选牌：原版 {@code Astrolabe.onEquip} 是用
     * {@code gridSelectScreen.open(可删牌, 3, 描述 + 遗物名, false, false, false, false)} 打开的，
     * 所以这里靠“不是变形/升级/删牌 + 要选 3 张 + 持有星盘 + 提示文字里带遗物名”来认定，
     * 不去 patch 原版星盘类。
     */
    private static boolean isAstrolabeGrid(GridCardSelectScreen grid) {
        if (grid == null || grid.forTransform || grid.forUpgrade || grid.forPurge) {
            return false;
        }
        Object cards = Reflect.get(grid, "numCards");
        if (!(cards instanceof Integer) || ((Integer) cards).intValue() != 3) {
            return false;
        }
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null || !p.hasRelic(Astrolabe.ID)) {
            return false;
        }
        AbstractRelic relic = p.getRelic(Astrolabe.ID);
        Object tipObj = Reflect.get(grid, "tipMsg");
        String tip = tipObj instanceof String ? (String) tipObj : null;
        return relic != null && tip != null && relic.name != null
                && !relic.name.isEmpty() && tip.contains(relic.name);
    }

    /** 网格选牌界面关闭。 */
    public static void onGridClosed() {
        astrolabeGrid = false;
        clearCache();
    }

    /** 当前是否正等着玩家挑“要变形的牌”。 */
    public static boolean isPending() {
        GridCardSelectScreen grid = AbstractDungeon.gridSelectScreen;
        if (grid == null || AbstractDungeon.screen != AbstractDungeon.CurrentScreen.GRID) {
            return false;
        }
        if (grid.forTransform || astrolabeGrid || isEventTransformGrid(grid)) {
            return true;
        }
        NeowReward.NeowRewardType type = neowActivatedType();
        return type == NeowReward.NeowRewardType.TRANSFORM_CARD
                || type == NeowReward.NeowRewardType.TRANSFORM_TWO_CARDS;
    }

    /**
     * 事件/神龛里的“选牌后变形”：原版用
     * {@code gridSelectScreen.open(..., false, false, false, false)} 打开网格，
     * 选完在 update() 里调 {@code AbstractDungeon.transformCard(..., miscRng)}。
     * 靠“当前事件类型 + 要选几张”识别，避免和删牌、升级的网格混淆。
     */
    private static boolean isEventTransformGrid(GridCardSelectScreen grid) {
        if (grid.forTransform || grid.forUpgrade || grid.forPurge) {
            return false;
        }
        Object numObj = Reflect.get(grid, "numCards");
        if (!(numObj instanceof Integer)) {
            return false;
        }
        int num = ((Integer) numObj).intValue();
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        Object event = room == null ? null : room.event;
        if (event instanceof Transmogrifier) {
            return num == 1;
        }
        if (event instanceof DrugDealer) {
            return num == 2;
        }
        if (event instanceof Designer) {
            return num == 2;
        }
        return false;
    }

    private static void clearCache() {
        cachedOrigin = null;
        cachedSelected = -1;
        cachedResult = null;
    }

    // ------------------------------------------------------------------ 推算

    /** 同一张牌 + 同样的已选数量只推算一次，避免每帧复制卡牌。 */
    public static AbstractCard result(AbstractCard origin) {
        if (origin == null) {
            return null;
        }
        GridCardSelectScreen grid = AbstractDungeon.gridSelectScreen;
        int selected = grid == null || grid.selectedCards == null ? 0 : grid.selectedCards.size();
        if (cachedOrigin == origin && cachedSelected == selected && cachedResult != null) {
            return cachedResult;
        }
        AbstractCard predicted = predict(origin);
        if (predicted == null) {
            return null;
        }
        cachedOrigin = origin;
        cachedSelected = selected;
        cachedResult = predicted;
        return predicted;
    }

    /** 按原版顺序推算：先把已经选好的牌依次变形消耗随机数，再算当前这张。 */
    public static AbstractCard predict(AbstractCard origin) {
        if (origin == null) {
            return null;
        }
        Random base = baseRng();
        if (base == null) {
            return null;
        }
        try {
            Random rng = base.copy();
            boolean autoUpgrade = autoUpgrade();
            GridCardSelectScreen grid = AbstractDungeon.gridSelectScreen;
            if (grid != null && grid.selectedCards != null) {
                for (AbstractCard picked : grid.selectedCards) {
                    if (picked != null && picked != origin) {
                        transform(picked, autoUpgrade, rng);
                    }
                }
            }
            return transform(origin, autoUpgrade, rng);
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean autoUpgrade() {
        return astrolabeGrid;
    }

    /** 变形用的随机数：涅奥自己的变形祝福用 NeowEvent.rng，阿斯特罗莱布与其它事件/遗物都用 miscRng。 */
    private static Random baseRng() {
        try {
            if (astrolabeGrid) {
                // AbstractDungeon.transformCard(card, true, AbstractDungeon.miscRng)
                return AbstractDungeon.miscRng;
            }
            NeowReward.NeowRewardType type = neowActivatedType();
            if ((type == NeowReward.NeowRewardType.TRANSFORM_CARD
                    || type == NeowReward.NeowRewardType.TRANSFORM_TWO_CARDS)
                    && NeowEvent.rng != null) {
                return NeowEvent.rng;
            }
            return AbstractDungeon.miscRng;
        } catch (Throwable t) {
            return null;
        }
    }

    private static NeowEvent currentNeow() {
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        if (room != null && room.event instanceof NeowEvent) {
            return (NeowEvent) room.event;
        }
        if (NeowState.eventRef != null && NeowState.isActive(NeowState.eventRef)) {
            return NeowState.eventRef;
        }
        return null;
    }

    /** 涅奥事件里当前被点掉的那个祝福类型。 */
    private static NeowReward.NeowRewardType neowActivatedType() {
        NeowEvent event = currentNeow();
        if (event == null) {
            return null;
        }
        Object rewards = Reflect.get(event, "rewards");
        if (!(rewards instanceof List)) {
            return null;
        }
        for (Object o : (List<?>) rewards) {
            if (o instanceof NeowReward && Boolean.TRUE.equals(Reflect.get(o, "activated"))) {
                return ((NeowReward) o).type;
            }
        }
        return null;
    }

    /** 复刻 AbstractDungeon.transformCard（只读卡池，只消耗传入的随机数副本）。 */
    private static AbstractCard transform(AbstractCard card, boolean autoUpgrade, Random rng) {
        AbstractCard result;
        switch (card.color) {
            case COLORLESS:
                result = AbstractDungeon.returnTrulyRandomColorlessCardFromAvailable(card, rng).makeCopy();
                break;
            case CURSE:
                result = CardLibrary.getCurse(card, rng).makeCopy();
                break;
            default:
                result = AbstractDungeon.returnTrulyRandomCardFromAvailable(card, rng).makeCopy();
                break;
        }
        if (result != null && autoUpgrade && result.canUpgrade()) {
            result.upgrade();
        }
        return result;
    }

    // ------------------------------------------------------------------ 绘制

    /** 覆盖原版确认界面右侧的“随机翻牌”，换成真正的结果。 */
    public static void patchConfirmCard(GridCardSelectScreen grid) {
        if (grid == null || !grid.forTransform || !grid.confirmScreenUp) {
            return;
        }
        AbstractCard origin = (AbstractCard) Reflect.get(grid, "hoveredCard");
        AbstractCard predicted = result(origin);
        if (predicted == null) {
            return;
        }
        predicted.angle = 0.0f;
        predicted.drawScale = 1.0f;
        predicted.targetDrawScale = 1.0f;
        grid.upgradePreviewCard = predicted;
    }

    /** 没有确认界面的变形选牌：把选中牌会变成的那张悬浮显示在它旁边。 */
    public static void renderBeside(SpriteBatch sb) {
        GridCardSelectScreen grid = AbstractDungeon.gridSelectScreen;
        if (sb == null || grid == null || grid.confirmScreenUp || !isPending()) {
            return;
        }
        AbstractCard origin = (AbstractCard) Reflect.get(grid, "hoveredCard");
        if (origin == null || origin.hb == null || !origin.hb.hovered) {
            return;
        }
        AbstractCard predicted = result(origin);
        if (predicted == null) {
            return;
        }
        float imageScale = Config.imageScale();
        float w = AbstractCard.IMG_WIDTH * imageScale;
        float h = AbstractCard.IMG_HEIGHT * imageScale;
        float gap = 14.0f * Settings.scale;
        float margin = 24.0f * Settings.scale;
        float originHalf = AbstractCard.IMG_WIDTH * origin.drawScale * 0.5f;
        // 悬浮在悬停牌的右侧；右边放不下就翻到左侧
        float cx = origin.current_x + originHalf + w * 0.5f + gap;
        if (cx + w * 0.5f > Settings.WIDTH - margin) {
            cx = origin.current_x - originHalf - w * 0.5f - gap;
        }
        cx = Math.max(w * 0.5f + margin, Math.min(Settings.WIDTH - w * 0.5f - margin, cx));
        float cy = origin.current_y - 24.0f * Settings.scale;
        cy = Math.max(h * 0.5f + margin, Math.min(Settings.HEIGHT - h * 0.5f - margin, cy));
        sb.setColor(Color.WHITE);
        predicted.angle = 0.0f;
        predicted.current_x = cx;
        predicted.current_y = cy;
        predicted.drawScale = imageScale;
        predicted.targetDrawScale = imageScale;
        predicted.render(sb);
    }
}
