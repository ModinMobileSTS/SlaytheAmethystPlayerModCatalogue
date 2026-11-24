package neowpreview;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.events.beyond.MindBloom;
import com.megacrit.cardcrawl.events.beyond.MysteriousSphere;
import com.megacrit.cardcrawl.events.beyond.SensoryStone;
import com.megacrit.cardcrawl.events.city.KnowingSkull;
import com.megacrit.cardcrawl.events.city.TheLibrary;
import com.megacrit.cardcrawl.events.city.TheMausoleum;
import com.megacrit.cardcrawl.events.exordium.BigFish;
import com.megacrit.cardcrawl.events.shrines.Bonfire;
import com.megacrit.cardcrawl.events.shrines.Designer;
import com.megacrit.cardcrawl.events.shrines.WeMeetAgain;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.screens.select.GridCardSelectScreen;
import com.megacrit.cardcrawl.ui.buttons.LargeDialogOptionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 第二批事件随机预测：大鱼、篝火精灵、尖端设计师、全知头骨、心灵绽放、神秘圆球、感知石、
 * 大图书馆、陵墓、我们又见面了。
 *
 * <p>做法统一：找出原版真正调用的随机函数，用「随机数副本 + 同样的取用顺序」算出结果，
 * 或者把 {@code cardRng}/{@code cardRandomRng} 换成副本后直接调原版方法，算完立刻还原 ——
 * 不消耗游戏随机数、不改原版逻辑。</p>
 *
 * <p>提示框统一成「问答」：标题是问题（选项文本 / 卡名），正文只给结论。</p>
 */
public final class EventPreviewMore {

    /** 网格界面的一次会话，以及最近一次悬停的卡（松手后仍然保留）。 */
    private static boolean gridOpen = false;
    private static AbstractCard gridCard = null;
    /** 一次最多预测这么多个结果（全知头骨连着买的情况）。 */
    private static final int MAX_PREDICT = 10;

    private EventPreviewMore() {
    }

    /** 图像/文本事件对话框画完后调用。 */
    public static void render(SpriteBatch sb, AbstractEvent event, List<LargeDialogOptionButton> options) {
        try {
            if (event instanceof BigFish) {
                relicBox(event, options, 2, "bigfish");
            } else if (event instanceof Designer) {
                designer(sb, event, options);
            } else if (event instanceof KnowingSkull) {
                knowingSkull(sb, event, options);
            } else if (event instanceof MindBloom) {
                rareRelicOption(event, options, "mindbloom", 3);
            } else if (event instanceof MysteriousSphere) {
                rareRelicOption(event, options, "sphere", 2);
            } else if (event instanceof SensoryStone) {
                sensoryStone(sb, event, options);
            } else if (event instanceof TheLibrary) {
                library(sb, event, options);
            } else if (event instanceof TheMausoleum) {
                mausoleum(event, options);
            } else if (event instanceof WeMeetAgain) {
                weMeetAgain(event, options);
            }
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------- 大鱼

    /** 原版「打开箱子」：{@code returnRandomScreenlessRelic(returnRandomRelicTier())} + 一张 Regret。 */
    private static void relicBox(AbstractEvent event, List<LargeDialogOptionButton> options, int index, String tag) {
        if (options.size() <= index) {
            return;
        }
        if (!isHovered(options.get(index))) {
            return;
        }
        ArrayList<PowerTip> tips = PreviewCache.TIPS.get(event, tag + "|" + stateKey(), () -> {
            ArrayList<PowerTip> t = new ArrayList<>();
            t.addAll(relicTips(NeowPredictor.screenlessRelic()));
            return t;
        });
        showTips(tips);
    }

    // ------------------------------------------------------- 尖端设计师

    /**
     * 原版构造时先 {@code miscRng.randomBoolean()} 决定「调整」是升级一张还是随机升级两张；
     * 随机升级两张时走 {@code upgradeTwoRandomCards()}：
     * {@code Collections.shuffle(可升级牌, new Random(miscRng.randomLong()))} 后取前两张。
     */
    private static void designer(SpriteBatch sb, AbstractEvent event, List<LargeDialogOptionButton> options) {
        if (options.size() < 4) {
            return;
        }
        int hovered = hoveredIndex(options, 4);
        if (hovered < 0) {
            return;
        }
        LargeDialogOptionButton button = options.get(hovered);
        if (hovered == 3) {
            Object hpLoss = Reflect.get(event, "hpLoss");
            if (hpLoss instanceof Integer) {
                String hp = hpAfter(((Integer) hpLoss).intValue());
                if (hp != null) {
                    ArrayList<PowerTip> tips = new ArrayList<>();
                    tips.add(new PowerTip(Designer.NAME, hp));
                    showTips(tips);
                }
            }
            return;
        }
        if (hovered != 0 || !Boolean.FALSE.equals(Reflect.get(event, "adjustmentUpgradesOne"))) {
            return;
        }
        List<List<AbstractCard>> rows = PreviewCache.CARDS.get(event, "designer|" + stateKey(),
                () -> rows(designerUpgrades()));
        drawCardRows(sb, rows);
    }

    /** 复刻 Designer.upgradeTwoRandomCards()：只会升级，不消耗别的随机数。 */
    private static List<AbstractCard> designerUpgrades() {
        List<AbstractCard> out = new ArrayList<>();
        AbstractPlayer player = AbstractDungeon.player;
        Random rng = AbstractDungeon.miscRng;
        if (player == null || player.masterDeck == null || rng == null) {
            return out;
        }
        List<AbstractCard> upgradable = new ArrayList<>();
        for (AbstractCard card : player.masterDeck.group) {
            if (card != null && card.canUpgrade()) {
                upgradable.add(card);
            }
        }
        if (upgradable.isEmpty()) {
            return out;
        }
        long seed;
        try {
            seed = rng.copy().randomLong();
        } catch (Throwable t) {
            return out;
        }
        Collections.shuffle(upgradable, new java.util.Random(seed));
        int take = Math.min(upgradable.size(), 2);
        for (int i = 0; i < take; ++i) {
            AbstractCard copy = upgradable.get(i).makeStatEquivalentCopy();
            if (copy.canUpgrade()) {
                copy.upgrade();
            }
            out.add(copy);
        }
        return out;
    }

    // --------------------------------------------------------- 全知头骨

    /**
     * 原版「问一句」的奖励：药水走 {@code PotionHelper.getRandomPotion()}（potionRng），
     * 卡牌走 {@code AbstractDungeon.returnColorlessCard(UNCOMMON)}（卡池随机）。
     * 多次购买时两串随机数互不干扰，所以能按顺序预测接下来若干次。
     */
    private static void knowingSkull(SpriteBatch sb, AbstractEvent event, List<LargeDialogOptionButton> options) {
        if (options.size() < 4) {
            return;
        }
        int hovered = hoveredIndex(options, 4);
        if (hovered < 0) {
            return;
        }
        AbstractPlayer player = AbstractDungeon.player;
        int reserve = intField(event, "leaveCost", 6);
        int hp = player == null ? 0 : player.currentHealth;
        if (hovered == 0) {
            if (player != null && player.hasRelic("Sozu")) {
                return;
            }
            int runs = affordableRuns(intField(event, "potionCost", 6), hp, reserve);
            int count = Math.min(MAX_PREDICT, Math.max(1, runs));
            ArrayList<PowerTip> tips = PreviewCache.TIPS.get(event,
                    "skullpotion|" + count + "|" + runs + "|" + stateKey(), () -> {
                        ArrayList<PowerTip> t = new ArrayList<>();
                        t.add(new PowerTip(KnowingSkull.NAME, Lang.skullRemaining(runs)));
                        if (runs > 0) {
                            for (AbstractPotion potion : NeowPredictor.randomPotions(count)) {
                                if (potion != null) {
                                    t.add(new PowerTip(potion.name, potion.description));
                                }
                            }
                        }
                        return t;
                    });
            showTips(tips);
        } else if (hovered == 1) {
            int runs = affordableRuns(intField(event, "goldCost", 6), hp, reserve);
            ArrayList<PowerTip> tips = new ArrayList<>();
            tips.add(new PowerTip(KnowingSkull.NAME, Lang.skullRemaining(runs)));
            showTips(tips);
        } else if (hovered == 2) {
            int runs = affordableRuns(intField(event, "cardCost", 6), hp, reserve);
            int count = Math.min(MAX_PREDICT, Math.max(1, runs));
            List<List<AbstractCard>> rows = PreviewCache.CARDS.get(event,
                    "skullcard|" + count + "|" + runs + "|" + stateKey(),
                    () -> rowsSplit(runs > 0 ? colorlessUncommons(count) : new ArrayList<>(), 5));
            drawCardRows(sb, rows);
            ArrayList<PowerTip> tips = new ArrayList<>();
            tips.add(new PowerTip(KnowingSkull.NAME, Lang.skullRemaining(runs)));
            showTips(tips);
        } else if (hovered == 3) {
            String hpText = hpAfter(reserve);
            if (hpText != null) {
                ArrayList<PowerTip> tips = new ArrayList<>();
                tips.add(new PowerTip(KnowingSkull.NAME, hpText));
                showTips(tips);
            }
        }
    }

    /** 还能买几次：每次买完这种奖励，这种奖励的代价 +1；离去固定要留 reserve 点血，且不能把自己买死。 */
    private static int affordableRuns(int cost, int hp, int reserve) {
        int pool = hp - Math.max(0, reserve);
        int runs = 0;
        int next = Math.max(1, cost);
        while (pool - next >= 1) {
            pool -= next;
            next += 1;
            ++runs;
        }
        return runs;
    }

    private static int intField(Object target, String name, int fallback) {
        Object value = Reflect.get(target, name);
        return value instanceof Integer ? ((Integer) value).intValue() : fallback;
    }

    /** 原版 {@code returnColorlessCard(UNCOMMON)}：把两个卡牌随机数换成副本再照原样连着调。 */
    private static List<AbstractCard> colorlessUncommons(int count) {
        List<AbstractCard> out = new ArrayList<>();
        Random cardRng = AbstractDungeon.cardRng;
        Random cardRandomRng = AbstractDungeon.cardRandomRng;
        try {
            if (cardRng != null) {
                AbstractDungeon.cardRng = cardRng.copy();
            }
            if (cardRandomRng != null) {
                AbstractDungeon.cardRandomRng = cardRandomRng.copy();
            }
            for (int i = 0; i < count; ++i) {
                AbstractCard card = AbstractDungeon.returnColorlessCard(AbstractCard.CardRarity.UNCOMMON);
                if (card == null) {
                    break;
                }
                out.add(card.makeCopy());
            }
        } catch (Throwable ignored) {
        } finally {
            AbstractDungeon.cardRng = cardRng;
            AbstractDungeon.cardRandomRng = cardRandomRng;
        }
        return out;
    }

    // --------------------------------------------------------- 心灵绽放

    /** 原版「战斗」分支：奖励里放了一件 {@code returnRandomRelic(RARE)}。 */
    private static void rareRelicOption(AbstractEvent event, List<LargeDialogOptionButton> options, String tag,
                                        int minOptions) {
        if (options.size() < minOptions) {
            return;
        }
        if (!isHovered(options.get(0))) {
            return;
        }
        boolean screenless = "sphere".equals(tag);
        ArrayList<PowerTip> tips = PreviewCache.TIPS.get(event, tag + "|" + stateKey(), () -> {
            ArrayList<PowerTip> t = new ArrayList<>();
            AbstractRelic relic = screenless
                    ? NeowPredictor.screenlessRelic(AbstractRelic.RelicTier.RARE)
                    : NeowPredictor.relic(AbstractRelic.RelicTier.RARE);
            t.addAll(relicTips(relic));
            return t;
        });
        showTips(tips);
    }

    // --------------------------------------------------------- 感知石

    /**
     * 原版三个回忆分别给 1、2、3 份无色卡牌奖励（{@code new RewardItem(CardColor.COLORLESS)}），
     * 每份奖励 = 一组卡里选一张；一次拿三份就是三行。
     */
    private static void sensoryStone(SpriteBatch sb, AbstractEvent event, List<LargeDialogOptionButton> options) {
        if (options.size() < 3) {
            return;
        }
        int hovered = hoveredIndex(options, 3);
        if (hovered < 0) {
            return;
        }
        int groups = hovered + 1;
        List<List<AbstractCard>> rows = PreviewCache.CARDS.get(event, "sensory|" + groups + "|" + stateKey(),
                () -> colorlessRewardGroups(groups));
        // 每行放两组：选 1/2/3 组时就排成 1/1/2 行
        drawGroupRows(sb, rows, 2);
    }

    /**
     * 复刻 {@code AbstractDungeon.getColorlessRewardCards()}（原版给无色卡奖励用的就是它），
     * 逐组连着算，随机数用副本，顺带把会写进静态状态的卡牌稀有度计数还原。
     */
    private static List<List<AbstractCard>> colorlessRewardGroups(int groups) {
        List<List<AbstractCard>> rows = new ArrayList<>();
        Random cardRng = AbstractDungeon.cardRng;
        Random cardRandomRng = AbstractDungeon.cardRandomRng;
        int blizz = AbstractDungeon.cardBlizzRandomizer;
        try {
            if (cardRng != null) {
                AbstractDungeon.cardRng = cardRng.copy();
            }
            if (cardRandomRng != null) {
                AbstractDungeon.cardRandomRng = cardRandomRng.copy();
            }
            for (int i = 0; i < groups; ++i) {
                ArrayList<?> reward = AbstractDungeon.getColorlessRewardCards();
                if (reward == null) {
                    break;
                }
                List<AbstractCard> row = new ArrayList<>();
                for (Object o : reward) {
                    if (o instanceof AbstractCard) {
                        row.add((AbstractCard) o);
                    }
                }
                rows.add(row);
            }
        } catch (Throwable ignored) {
        } finally {
            AbstractDungeon.cardRng = cardRng;
            AbstractDungeon.cardRandomRng = cardRandomRng;
            AbstractDungeon.cardBlizzRandomizer = blizz;
        }
        return rows;
    }

    // --------------------------------------------------------- 大图书馆

    /** 原版「阅读」：连续 {@code getCard(rollRarity())} 抽 20 张（同 ID 重抽），进网格让你选一张。 */
    private static void library(SpriteBatch sb, AbstractEvent event, List<LargeDialogOptionButton> options) {
        Object screenNum = Reflect.get(event, "screenNum");
        if (!(screenNum instanceof Integer) || ((Integer) screenNum).intValue() != 0 || options.size() < 2) {
            return;
        }
        if (!isHovered(options.get(0))) {
            return;
        }
        List<List<AbstractCard>> rows = PreviewCache.CARDS.get(event, "library|" + stateKey(),
                () -> rowsFit(libraryCards()));
        drawCardRows(sb, rows);
    }

    private static List<AbstractCard> libraryCards() {
        List<AbstractCard> out = new ArrayList<>();
        Random cardRng = AbstractDungeon.cardRng;
        Random cardRandomRng = AbstractDungeon.cardRandomRng;
        try {
            if (cardRng != null) {
                AbstractDungeon.cardRng = cardRng.copy();
            }
            if (cardRandomRng != null) {
                AbstractDungeon.cardRandomRng = cardRandomRng.copy();
            }
            for (int i = 0; i < 20; ++i) {
                AbstractCard card = AbstractDungeon.getCard(AbstractDungeon.rollRarity());
                if (card == null) {
                    break;
                }
                card = card.makeCopy();
                int guard = 0;
                while (containsId(out, card.cardID) && guard++ < 200) {
                    card = AbstractDungeon.getCard(AbstractDungeon.rollRarity());
                    if (card == null) {
                        break;
                    }
                    card = card.makeCopy();
                }
                if (card != null) {
                    out.add(card);
                }
            }
        } catch (Throwable ignored) {
        } finally {
            AbstractDungeon.cardRng = cardRng;
            AbstractDungeon.cardRandomRng = cardRandomRng;
        }
        return out;
    }

    private static boolean containsId(List<AbstractCard> cards, String id) {
        for (AbstractCard card : cards) {
            if (card != null && card.cardID != null && card.cardID.equals(id)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------- 陵墓

    /**
     * 原版「打开」：先 {@code miscRng.randomBoolean()} 决定会不会多拿一张 Writhe（飞升 15 起必中），
     * 再 {@code returnRandomScreenlessRelic(returnRandomRelicTier())} 给一件遗物。
     */
    private static void mausoleum(AbstractEvent event, List<LargeDialogOptionButton> options) {
        if (options.size() < 2) {
            return;
        }
        LargeDialogOptionButton button = options.get(0);
        if (!isHovered(button)) {
            return;
        }
        ArrayList<PowerTip> tips = PreviewCache.TIPS.get(event, "mausoleum|" + stateKey(), () -> {
            ArrayList<PowerTip> t = new ArrayList<>();
            boolean cursed;
            Random rng = AbstractDungeon.miscRng;
            if (AbstractDungeon.ascensionLevel >= 15) {
                cursed = true;
            } else {
                cursed = rng != null && rng.copy().randomBoolean();
            }
            t.add(new PowerTip(TheMausoleum.NAME, cursed ? Lang.curseYes() : Lang.curseNo()));
            t.addAll(relicTips(NeowPredictor.screenlessRelic()));
            return t;
        });
        showTips(tips);
    }

    // ----------------------------------------------------- 我们又见面了

    /** 原版三个交易选项都走 {@code relicReward()} → {@code returnRandomScreenlessRelic(returnRandomRelicTier())}。 */
    private static void weMeetAgain(AbstractEvent event, List<LargeDialogOptionButton> options) {
        if (options.size() < 3) {
            return;
        }
        int hovered = hoveredIndex(options, 3);
        if (hovered < 0) {
            return;
        }
        ArrayList<PowerTip> tips = PreviewCache.TIPS.get(event, "wemeet|" + hovered + "|" + stateKey(), () -> {
            ArrayList<PowerTip> t = new ArrayList<>();
            t.addAll(relicTips(NeowPredictor.screenlessRelic()));
            return t;
        });
        showTips(tips);
    }

    // ------------------------------------------------- 网格里的预览

    /**
     * 网格选牌界面的预测：篝火精灵献祭、尖端设计师的全套服务。
     *
     * <p>在 {@code GridCardSelectScreen.render} 之前调用，这样提示框先排进队列，
     * 不会被原版的卡牌提示顶掉；并且记住最近悬停过的牌，松手后文字仍然留着。</p>
     */
    public static void renderGrid(SpriteBatch sb) {
        try {
            if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.GRID) {
                gridOpen = false;
                gridCard = null;
                return;
            }
            GridCardSelectScreen grid = AbstractDungeon.gridSelectScreen;
            if (grid == null) {
                return;
            }
            if (!gridOpen) {
                gridOpen = true;
                gridCard = null;
            }
            AbstractRoom room = AbstractDungeon.getCurrRoom();
            if (room == null) {
                return;
            }
            AbstractCard hovered = (AbstractCard) Reflect.get(grid, "hoveredCard");
            boolean hovering = hovered != null && hovered.hb != null && hovered.hb.hovered;
            if (hovering) {
                gridCard = hovered;
            }
            if (gridCard == null) {
                return;
            }
            if (room.event instanceof Bonfire) {
                bonfireTip(room.event, gridCard);
            }
        } catch (Throwable ignored) {
        }
    }

    /** 网格界面上的卡面预览（画在网格之上，所以在 render 后缀里调用）。 */
    public static void renderGridCard(SpriteBatch sb) {
        try {
            if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.GRID || gridCard == null) {
                return;
            }
            AbstractRoom room = AbstractDungeon.getCurrRoom();
            if (room == null || !(room.event instanceof Designer)) {
                return;
            }
            designerGridTip(sb, room.event, gridCard);
        } catch (Throwable ignored) {
        }
    }

    /**
     * 篝火精灵的奖励只取决于「献上的牌的稀有度」（原版 setReward）：
     * 诅咒→Spirit Poop、基础→没有、普通/特殊→回 5 血、罕见→回满、稀有→回满 + 最大生命 +10。
     * 文字直接用原版 DESCRIPTIONS。
     */
    private static void bonfireTip(AbstractEvent event, AbstractCard card) {
        String key = "bonfire|" + card.cardID + "|" + card.rarity + "|" + hpKey();
        ArrayList<PowerTip> tips = PreviewCache.TIPS.get(event, key, () -> {
            ArrayList<PowerTip> t = new ArrayList<>();
            String body = bonfireBody(card.rarity);
            if (body == null) {
                return t;
            }
            t.add(new PowerTip(card.name, body));
            return t;
        });
        showTips(tips);
    }

    private static String bonfireBody(AbstractCard.CardRarity rarity) {
        switch (rarity) {
            case CURSE:
                return Bonfire.DESCRIPTIONS[3];
            case BASIC:
                return Bonfire.DESCRIPTIONS[4];
            case COMMON:
            case SPECIAL:
                return Bonfire.DESCRIPTIONS[5];
            case UNCOMMON:
                return Bonfire.DESCRIPTIONS[6];
            case RARE:
                return Bonfire.DESCRIPTIONS[7];
            default:
                return null;
        }
    }

    /**
     * 尖端设计师「全套服务」：先移除你选的那张，再
     * {@code Collections.shuffle(剩余可升级牌, new Random(miscRng.randomLong()))} 升级第一张。
     * 这里把「悬停的这张被移除」之后的结果算出来，只用文字说清会被升级的是哪张。
     */
    private static void designerGridTip(SpriteBatch sb, AbstractEvent event, AbstractCard card) {
        Object option = Reflect.get(event, "option");
        if (option == null || !String.valueOf(option).contains("REMOVE_AND_UPGRADE")) {
            return;
        }
        List<AbstractCard> predicted = PreviewCache.CARDS.get(event,
                "designergrid|" + card.cardID + "|" + stateKey(),
                () -> designerFullServiceUpgrade(card));
        if (predicted.isEmpty() || predicted.get(0) == null) {
            return;
        }
        drawBeside(sb, card, predicted.get(0));
    }

    private static List<AbstractCard> designerFullServiceUpgrade(AbstractCard removed) {
        List<AbstractCard> out = new ArrayList<>();
        AbstractPlayer player = AbstractDungeon.player;
        Random rng = AbstractDungeon.miscRng;
        if (player == null || player.masterDeck == null || rng == null || removed == null) {
            return out;
        }
        List<AbstractCard> upgradable = new ArrayList<>();
        for (AbstractCard card : player.masterDeck.group) {
            if (card != null && card != removed && card.canUpgrade()) {
                upgradable.add(card);
            }
        }
        if (upgradable.isEmpty()) {
            return out;
        }
        long seed;
        try {
            seed = rng.copy().randomLong();
        } catch (Throwable t) {
            return out;
        }
        Collections.shuffle(upgradable, new java.util.Random(seed));
        AbstractCard copy = upgradable.get(0).makeStatEquivalentCopy();
        if (copy.canUpgrade()) {
            copy.upgrade();
        }
        out.add(copy);
        return out;
    }

    // ---------------------------------------------------------------- 工具

    private static String stateKey() {
        AbstractPlayer player = AbstractDungeon.player;
        String hp = player == null ? "?" : String.valueOf(player.currentHealth);
        String gold = player == null ? "?" : String.valueOf(player.gold);
        return AbstractDungeon.floorNum + "/" + hp + "/" + gold;
    }

    private static String hpKey() {
        AbstractPlayer player = AbstractDungeon.player;
        return player == null ? "?" : String.valueOf(player.currentHealth);
    }

    private static boolean isHovered(LargeDialogOptionButton button) {
        return button != null && button.hb != null && !button.isDisabled && button.hb.hovered;
    }

    private static int hoveredIndex(List<LargeDialogOptionButton> options, int count) {
        for (int i = 0; i < count && i < options.size(); ++i) {
            if (isHovered(options.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private static String hpAfter(int damage) {
        AbstractPlayer player = AbstractDungeon.player;
        if (player == null) {
            return null;
        }
        int left = player.currentHealth - Math.max(0, damage);
        if (left < 0) {
            left = 0;
        }
        return Lang.hpLeft(left);
    }

    private static ArrayList<PowerTip> relicTips(AbstractRelic relic) {
        ArrayList<PowerTip> tips = new ArrayList<>();
        if (relic == null) {
            return tips;
        }
        String desc = relic.description;
        if (desc == null || desc.trim().isEmpty()) {
            if (relic.tips != null) {
                for (PowerTip tip : relic.tips) {
                    if (tip != null) {
                        tips.add(tip);
                    }
                }
            }
            return tips;
        }
        if (relic.img != null) {
            tips.add(new PowerTip(relic.name, desc, relic.img));
        } else {
            tips.add(new PowerTip(relic.name, desc));
        }
        return tips;
    }

    private static void showTips(ArrayList<PowerTip> tips) {
        if (tips == null || tips.isEmpty()) {
            return;
        }
        float y = (float) InputHelper.mY + TipHelper.calculateToAvoidOffscreen(tips, InputHelper.mY);
        TipHelper.queuePowerTips(470.0f * Settings.scale, y, tips);
    }

    private static List<List<AbstractCard>> rows(List<AbstractCard> cards) {
        List<List<AbstractCard>> rows = new ArrayList<>();
        if (cards != null && !cards.isEmpty()) {
            rows.add(cards);
        }
        return rows;
    }

    /**
     * 按「图片大小」设置能放下几张来分行：卡面保持设置里的尺寸，只折成多行，
     * 不再为了塞进一行而整体缩小（大图书馆一次 20 张）。
     */
    private static List<List<AbstractCard>> rowsFit(List<AbstractCard> cards) {
        if (cards == null || cards.isEmpty()) {
            return new ArrayList<>();
        }
        float imageScale = Config.imageScale();
        float gap = 8.0f * Settings.scale;
        float maxW = Settings.WIDTH * 0.92f;
        float cardW = AbstractCard.IMG_WIDTH * imageScale;
        int perRow = Math.max(1, (int) Math.floor((maxW + gap) / (cardW + gap)));
        return rowsSplit(cards, perRow);
    }

    /** 每行最多 perRow 张，拆成多行（全知头骨一次预测 10 张时就是两行）。 */
    private static List<List<AbstractCard>> rowsSplit(List<AbstractCard> cards, int perRow) {
        List<List<AbstractCard>> rows = new ArrayList<>();
        if (cards == null || cards.isEmpty()) {
            return rows;
        }
        int per = Math.max(1, perRow);
        List<AbstractCard> current = new ArrayList<>();
        for (AbstractCard card : cards) {
            current.add(card);
            if (current.size() >= per) {
                rows.add(current);
                current = new ArrayList<>();
            }
        }
        if (!current.isEmpty()) {
            rows.add(current);
        }
        return rows;
    }

    /** 把结果卡面悬浮在悬停牌旁边：右边放不下就翻到左边（和变牌预览同一套摆法）。 */
    private static void drawBeside(SpriteBatch sb, AbstractCard origin, AbstractCard predicted) {
        if (sb == null || origin == null || predicted == null) {
            return;
        }
        float imageScale = Config.imageScale();
        float w = AbstractCard.IMG_WIDTH * imageScale;
        float h = AbstractCard.IMG_HEIGHT * imageScale;
        float gap = 14.0f * Settings.scale;
        float margin = 24.0f * Settings.scale;
        float originHalf = AbstractCard.IMG_WIDTH * origin.drawScale * 0.5f;
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

    /**
     * 把多组卡面按「每行放几组」摆开：组内紧凑、组与组之间留大一点的缝。
     * 感知石选 1/2/3 组时就排成 1/1/2 行。
     */
    private static void drawGroupRows(SpriteBatch sb, List<List<AbstractCard>> groups, int groupsPerRow) {
        if (sb == null || groups == null || groups.isEmpty() || groupsPerRow <= 0) {
            return;
        }
        List<List<List<AbstractCard>>> rows = new ArrayList<>();
        List<List<AbstractCard>> current = new ArrayList<>();
        for (List<AbstractCard> group : groups) {
            if (group == null || group.isEmpty()) {
                continue;
            }
            current.add(group);
            if (current.size() >= groupsPerRow) {
                rows.add(current);
                current = new ArrayList<>();
            }
        }
        if (!current.isEmpty()) {
            rows.add(current);
        }
        if (rows.isEmpty()) {
            return;
        }
        float gap = 8.0f * Settings.scale;
        float groupGap = 34.0f * Settings.scale;
        float rowGap = 12.0f * Settings.scale;
        float maxW = Settings.WIDTH * 0.92f;
        float maxH = Settings.HEIGHT * 0.86f;
        float scale = Config.imageScale();
        for (List<List<AbstractCard>> row : rows) {
            int count = 0;
            float fixed = 0.0f;
            for (int i = 0; i < row.size(); ++i) {
                List<AbstractCard> group = row.get(i);
                count += group.size();
                fixed += Math.max(0, group.size() - 1) * gap;
                if (i > 0) {
                    fixed += groupGap;
                }
            }
            if (count > 0) {
                scale = Math.min(scale, Math.max(0.05f, (maxW - fixed) / (count * AbstractCard.IMG_WIDTH)));
            }
        }
        scale = Math.min(scale, Math.max(0.05f,
                (maxH - (rows.size() - 1) * rowGap) / (rows.size() * AbstractCard.IMG_HEIGHT)));
        float w = AbstractCard.IMG_WIDTH * scale;
        float h = AbstractCard.IMG_HEIGHT * scale;
        float totalH = rows.size() * h + (rows.size() - 1) * rowGap;
        float top = Settings.HEIGHT * 0.5f + totalH * 0.5f;
        sb.setColor(Color.WHITE);
        for (int r = 0; r < rows.size(); ++r) {
            List<List<AbstractCard>> row = rows.get(r);
            float rowW = 0.0f;
            for (int i = 0; i < row.size(); ++i) {
                List<AbstractCard> group = row.get(i);
                rowW += group.size() * w + Math.max(0, group.size() - 1) * gap;
                if (i > 0) {
                    rowW += groupGap;
                }
            }
            float x = Settings.WIDTH * 0.5f - rowW * 0.5f;
            float cy = top - r * (h + rowGap) - h * 0.5f;
            for (int i = 0; i < row.size(); ++i) {
                List<AbstractCard> group = row.get(i);
                if (i > 0) {
                    x += groupGap;
                }
                for (int j = 0; j < group.size(); ++j) {
                    AbstractCard card = group.get(j);
                    if (card != null) {
                        card.current_x = x + w * 0.5f;
                        card.current_y = cy;
                        card.drawScale = scale;
                        card.targetDrawScale = scale;
                        card.angle = 0.0f;
                        card.render(sb);
                    }
                    x += w;
                    if (j < group.size() - 1) {
                        x += gap;
                    }
                }
            }
        }
    }

    private static void drawCardRows(SpriteBatch sb, List<List<AbstractCard>> rows) {
        drawCardRows(sb, rows, 1.0f);
    }

    /** 把若干行卡面按屏幕宽高自动缩放、居中显示（最多一行 10 张），maxScale 用来压小一点。 */
    private static void drawCardRows(SpriteBatch sb, List<List<AbstractCard>> rows, float maxScale) {
        if (sb == null || rows == null || rows.isEmpty()) {
            return;
        }
        List<List<AbstractCard>> visible = new ArrayList<>();
        int maxPerRow = 1;
        for (List<AbstractCard> row : rows) {
            if (row != null && !row.isEmpty()) {
                visible.add(row);
                maxPerRow = Math.max(maxPerRow, row.size());
            }
        }
        if (visible.isEmpty()) {
            return;
        }
        int rowCount = visible.size();
        float maxW = Settings.WIDTH * 0.92f;
        float maxH = Settings.HEIGHT * 0.86f;
        float gap = 8.0f * Settings.scale;
        float rowGap = 12.0f * Settings.scale;
        float scaleW = maxW / (maxPerRow * AbstractCard.IMG_WIDTH);
        float scaleH = (maxH - (rowCount - 1) * rowGap) / (rowCount * AbstractCard.IMG_HEIGHT);
        float scale = Math.min(Config.imageScale(), Math.min(Math.min(scaleW, scaleH), maxScale));
        if (scale <= 0.0f) {
            return;
        }
        float w = AbstractCard.IMG_WIDTH * scale;
        float h = AbstractCard.IMG_HEIGHT * scale;
        float totalH = rowCount * h + (rowCount - 1) * rowGap;
        float top = Settings.HEIGHT * 0.5f + totalH * 0.5f;
        sb.setColor(Color.WHITE);
        for (int r = 0; r < rowCount; ++r) {
            List<AbstractCard> row = visible.get(r);
            float rowW = row.size() * w + (row.size() - 1) * gap;
            float left = Settings.WIDTH * 0.5f - rowW * 0.5f;
            float centerY = top - r * (h + rowGap) - h * 0.5f;
            for (int i = 0; i < row.size(); ++i) {
                AbstractCard card = row.get(i);
                if (card == null) {
                    continue;
                }
                card.current_x = left + w * 0.5f + i * (w + gap);
                card.current_y = centerY;
                card.drawScale = scale;
                card.targetDrawScale = scale;
                card.angle = 0.0f;
                card.render(sb);
            }
        }
    }
}
