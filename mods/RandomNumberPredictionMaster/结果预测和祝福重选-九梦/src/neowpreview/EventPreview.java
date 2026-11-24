package neowpreview;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.events.city.Addict;
import com.megacrit.cardcrawl.events.city.CursedTome;
import com.megacrit.cardcrawl.events.city.TheJoust;
import com.megacrit.cardcrawl.events.exordium.DeadAdventurer;
import com.megacrit.cardcrawl.events.exordium.ScrapOoze;
import com.megacrit.cardcrawl.events.exordium.ShiningLight;
import com.megacrit.cardcrawl.events.shrines.FaceTrader;
import com.megacrit.cardcrawl.events.shrines.WomanInBlue;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.ui.buttons.LargeDialogOptionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 事件里“点下去才知道结果”的随机预测：鼠标停在某个选项上时展示真实走向。
 *
 * <p>全部用随机数副本推算，不消耗真随机数。遗物一律显示原版遗物详情（图标 + 名字 + 效果），
 * 药水显示原版药水详情（名字 + 效果），卡牌直接画卡面。目前覆盖：</p>
 * <ul>
 *   <li>「微光」：随机升级的是哪 1-2 张牌</li>
 *   <li>「决斗」：主人（Noodles）和挑战者谁赢</li>
 *   <li>「拾荒史莱姆」：再伸手几次会成功 + 成功后是哪件遗物</li>
 *   <li>「死亡冒险家」：这次搜索会不会被伏击、不被伏击时拿到什么</li>
 *   <li>「命运之轮」：转盘会停在哪个结果</li>
 *   <li>「诅咒之书」：读完得到哪本书</li>
 *   <li>「换脸商人」：交易面孔得到哪张面具</li>
 *   <li>「蓝衣女子」：花钱买到的是哪几瓶药水</li>
 *   <li>「成瘾者」：付钱 / 拿 Shame 换到哪件遗物</li>
 * </ul>
 */
public final class EventPreview {

    private EventPreview() {
    }

    /** 由事件对话框的 render 后缀调用，optionList 为该对话框的选项列表。 */
    public static void render(SpriteBatch sb, Object optionListRaw) {
        if (sb == null || !(optionListRaw instanceof List) || AbstractDungeon.isScreenUp) {
            return;
        }
        @SuppressWarnings("unchecked")
        List<LargeDialogOptionButton> options = (List<LargeDialogOptionButton>) optionListRaw;
        if (options.isEmpty()) {
            return;
        }
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        if (room == null || room.event == null) {
            return;
        }
        AbstractEvent event = room.event;
        try {
            if (event instanceof ShiningLight) {
                shiningLight(sb, event, options);
            } else if (event instanceof TheJoust) {
                joust(event, options);
            } else if (event instanceof ScrapOoze) {
                scrapOoze(sb, event, options);
            } else if (event instanceof DeadAdventurer) {
                deadAdventurer(event, options);
            } else if (event instanceof CursedTome) {
                cursedTome(event, options);
            } else if (event instanceof FaceTrader) {
                faceTrader(event, options);
            } else if (event instanceof WomanInBlue) {
                womanInBlue(event, options);
            } else if (event instanceof Addict) {
                addict(event, options);
            } else {
                EventPreviewMore.render(sb, event, options);
            }
        } catch (Throwable ignored) {
        }
    }

    // ---------------------------------------------------------------- 微光

    private static void shiningLight(SpriteBatch sb, AbstractEvent event, List<LargeDialogOptionButton> options) {
        if (!"INTRO".equals(String.valueOf(Reflect.get(event, "screen"))) || options.size() < 2) {
            return;
        }
        LargeDialogOptionButton button = options.get(0);
        if (!isHovered(button)) {
            return;
        }
        List<AbstractCard> cards = PreviewCache.CARDS.get(event,
                "shining|" + AbstractDungeon.player.masterDeck.size(), EventPreview::shiningLightCards);
        if (!cards.isEmpty()) {
            drawCardsAbove(sb, cards, button);
        }
        Object damageObj = Reflect.get(event, "damage");
        if (damageObj instanceof Integer) {
            String hp = hpAfter(((Integer) damageObj).intValue());
            if (hp != null) {
                ArrayList<PowerTip> tips = new ArrayList<>();
                tips.add(new PowerTip(ShiningLight.NAME, hp));
                showTips(tips);
            }
        }
    }

    /**
     * 复刻 ShiningLight.upgradeCards()：把可升级的牌按牌组顺序取出来，
     * 用 {@code new Random(miscRng.randomLong())} 洗一遍，前两张就是会被升级的。
     * 这里用 miscRng 的副本取那个 long，不消耗真随机数。
     */
    private static List<AbstractCard> shiningLightCards() {
        List<AbstractCard> out = new ArrayList<>();
        if (AbstractDungeon.player == null || AbstractDungeon.player.masterDeck == null) {
            return out;
        }
        List<AbstractCard> upgradable = new ArrayList<>();
        for (AbstractCard card : AbstractDungeon.player.masterDeck.group) {
            if (card != null && card.canUpgrade()) {
                upgradable.add(card);
            }
        }
        if (upgradable.isEmpty()) {
            return out;
        }
        long seed;
        try {
            Random rng = AbstractDungeon.miscRng;
            if (rng == null) {
                return out;
            }
            seed = rng.copy().randomLong();
        } catch (Throwable t) {
            return out;
        }
        Collections.shuffle(upgradable, new java.util.Random(seed));
        int take = Math.min(upgradable.size(), 2);
        for (int i = 0; i < take; ++i) {
            AbstractCard card = upgradable.get(i).makeStatEquivalentCopy();
            if (card.canUpgrade()) {
                card.upgrade();
            }
            out.add(card);
        }
        return out;
    }

    // ---------------------------------------------------------------- 决斗

    private static void joust(AbstractEvent event, List<LargeDialogOptionButton> options) {
        if (!"EXPLANATION".equals(String.valueOf(Reflect.get(event, "screen"))) || options.size() < 2) {
            return;
        }
        int hovered = hoveredIndex(options, 2);
        if (hovered < 0 || AbstractDungeon.miscRng == null) {
            return;
        }
        LargeDialogOptionButton button = options.get(hovered);
        boolean ownerWins;
        try {
            ownerWins = AbstractDungeon.miscRng.copy().randomBoolean(0.3f);
        } catch (Throwable t) {
            return;
        }
        // 选项 0 押「杀人犯」赢，选项 1 押主人（Noodles）赢
        boolean win = hovered == 1 ? ownerWins : !ownerWins;
        ArrayList<PowerTip> tips = new ArrayList<>();
        tips.add(new PowerTip(TheJoust.NAME, win ? Lang.win() : Lang.lose()));
        showTips(tips);
    }

    // ---------------------------------------------------------- 拾荒史莱姆

    /**
     * 原版每次伸手掷 {@code miscRng.random(0, 99)}，达到 {@code 99 - relicObtainChance} 就成功；
     * 失败则几率 +10。这里在 miscRng 副本上推演到第一次成功的那一次。
     */
    private static void scrapOoze(SpriteBatch sb, AbstractEvent event, List<LargeDialogOptionButton> options) {
        Object screenNum = Reflect.get(event, "screenNum");
        if (!(screenNum instanceof Integer) || ((Integer) screenNum).intValue() != 0) {
            return;
        }
        if (!isHovered(options.get(0))) {
            return;
        }
        Object chanceObj = Reflect.get(event, "relicObtainChance");
        Random saved = AbstractDungeon.miscRng;
        if (!(chanceObj instanceof Integer) || saved == null) {
            return;
        }
        int chance = ((Integer) chanceObj).intValue();
        Object dmgObj = Reflect.get(event, "dmg");
        int dmg = dmgObj instanceof Integer ? ((Integer) dmgObj).intValue() : 3;
        int successAt = -1;
        int totalDamage = 0;
        try {
            Random rng = saved.copy();
            for (int attempt = 1; attempt <= 20; ++attempt) {
                totalDamage += dmg;
                if (rng.random(0, 99) >= 99 - chance) {
                    successAt = attempt;
                    break;
                }
                chance += 10;
                dmg += 1;
            }
        } catch (Throwable t) {
            return;
        }
        if (successAt < 0) {
            return;
        }
        int left = hpLeftInt(totalDamage);
        ArrayList<PowerTip> tips = new ArrayList<>();
        if (left >= 0) {
            tips.add(new PowerTip(ScrapOoze.NAME, Lang.oozeBody(left, successAt)));
        }
        tips.addAll(relicTips(NeowPredictor.screenlessRelic()));
        if (!tips.isEmpty()) {
            showTips(tips);
        }
    }

    // ---------------------------------------------------------- 死亡冒险家

    /**
     * 原版每次搜索先掷 {@code miscRng.random(0, 99)}，小于 encounterChance 就被精英伏击；
     * 否则从 rewards 头部取一项奖励（GOLD 固定 30 金币 / NOTHING / RELIC 随机遗物）。
     * rewards 在事件创建时就已经洗好，直接读即可。
     */
    private static void deadAdventurer(AbstractEvent event, List<LargeDialogOptionButton> options) {
        if (!"INTRO".equals(String.valueOf(Reflect.get(event, "screen")))) {
            return;
        }
        if (!isHovered(options.get(0))) {
            return;
        }
        Object chanceObj = Reflect.get(event, "encounterChance");
        Object rewardsObj = Reflect.get(event, "rewards");
        Random saved = AbstractDungeon.miscRng;
        if (!(chanceObj instanceof Integer) || !(rewardsObj instanceof List) || saved == null) {
            return;
        }
        boolean ambush;
        try {
            ambush = saved.copy().random(0, 99) < ((Integer) chanceObj).intValue();
        } catch (Throwable t) {
            return;
        }
        String header = DeadAdventurer.NAME;
        ArrayList<PowerTip> tips = new ArrayList<>();
        if (ambush) {
            tips.add(new PowerTip(header, Lang.ambushed()));
            showTips(tips);
            return;
        }
        List<?> rewards = (List<?>) rewardsObj;
        if (rewards.isEmpty()) {
            return;
        }
        String type = String.valueOf(rewards.get(0));
        if ("GOLD".equals(type)) {
            tips.add(new PowerTip(header, Lang.adventurerGold()));
        } else if ("NOTHING".equals(type)) {
            tips.add(new PowerTip(header, Lang.adventurerNothing()));
        } else if ("RELIC".equals(type)) {
            tips.add(new PowerTip(header, Lang.adventurerRelic()));
            tips.addAll(relicTips(NeowPredictor.screenlessRelic()));
        } else {
            return;
        }
        showTips(tips);
    }

    // ---------------------------------------------------------- 命运之轮
    // 「变化大转盘」不做预测：转盘结果原版会自己演出来，用户要求取消。


    // ---------------------------------------------------------- 诅咒之书

    /** 复刻 CursedTome.randomBook()：没拥有过的三本书里用 miscRng 抽一本。 */
    private static void cursedTome(AbstractEvent event, List<LargeDialogOptionButton> options) {
        if ("END".equals(String.valueOf(Reflect.get(event, "screen")))) {
            return;
        }
        if (!isHovered(options.get(0))) {
            return;
        }
        AbstractPlayer player = AbstractDungeon.player;
        Random rng = AbstractDungeon.miscRng;
        if (player == null || rng == null) {
            return;
        }
        List<AbstractRelic> books = new ArrayList<>();
        if (!player.hasRelic("Necronomicon")) {
            books.add(RelicLibrary.getRelic("Necronomicon"));
        }
        if (!player.hasRelic("Enchiridion")) {
            books.add(RelicLibrary.getRelic("Enchiridion"));
        }
        if (!player.hasRelic("Nilry's Codex")) {
            books.add(RelicLibrary.getRelic("Nilry's Codex"));
        }
        if (books.isEmpty()) {
            books.add(RelicLibrary.getRelic("Circlet"));
        }
        AbstractRelic book;
        try {
            book = books.get(Math.min(books.size() - 1, rng.copy().random(books.size() - 1)));
        } catch (Throwable t) {
            return;
        }
        ArrayList<PowerTip> tips = new ArrayList<>();
        int left = hpLeftInt(remainingDamage(event));
        if (left >= 0) {
            tips.add(new PowerTip(CursedTome.NAME, Lang.tomeBody(left, remainingReads(event))));
        }
        tips.addAll(relicTips(book));
        if (!tips.isEmpty()) {
            showTips(tips);
        }
    }

    /** 从当前页一路读到最后一页还要掉多少血（原版各页 1/2/3，最后一页 10，飞升 15 级起 15）。 */
    private static int remainingDamage(AbstractEvent event) {
        String screen = String.valueOf(Reflect.get(event, "screen"));
        Object finalObj = Reflect.get(event, "finalDmg");
        int last = finalObj instanceof Integer ? ((Integer) finalObj).intValue() : 10;
        if ("LAST_PAGE".equals(screen)) {
            return last;
        }
        if ("PAGE_3".equals(screen)) {
            return 3 + last;
        }
        if ("PAGE_2".equals(screen)) {
            return 2 + 3 + last;
        }
        return 1 + 2 + 3 + last;
    }

    /** 还要读几次才拿到书（每次读都掉血）：INTRO/PAGE_1 → 4，PAGE_2 → 3，PAGE_3 → 2，LAST_PAGE → 1。 */
    private static int remainingReads(AbstractEvent event) {
        String screen = String.valueOf(Reflect.get(event, "screen"));
        if ("LAST_PAGE".equals(screen)) {
            return 1;
        }
        if ("PAGE_3".equals(screen)) {
            return 2;
        }
        if ("PAGE_2".equals(screen)) {
            return 3;
        }
        return 4;
    }

    // ---------------------------------------------------------- 换脸商人

    /**
     * 复刻 FaceTrader.getRandomFace()：把没拥有过的五张面具按固定顺序收集，
     * 用 {@code new Random(miscRng.randomLong())} 洗一遍，取第一张。
     */
    private static void faceTrader(AbstractEvent event, List<LargeDialogOptionButton> options) {
        if (!"MAIN".equals(String.valueOf(Reflect.get(event, "screen")))) {
            return;
        }
        if (hoveredIndex(options, 2) != 1) {
            return;
        }
        AbstractPlayer player = AbstractDungeon.player;
        Random rng = AbstractDungeon.miscRng;
        if (player == null || rng == null) {
            return;
        }
        List<String> ids = new ArrayList<>();
        if (!player.hasRelic("CultistMask")) {
            ids.add("CultistMask");
        }
        if (!player.hasRelic("FaceOfCleric")) {
            ids.add("FaceOfCleric");
        }
        if (!player.hasRelic("GremlinMask")) {
            ids.add("GremlinMask");
        }
        if (!player.hasRelic("NlothsMask")) {
            ids.add("NlothsMask");
        }
        if (!player.hasRelic("SsserpentHead")) {
            ids.add("SsserpentHead");
        }
        if (ids.isEmpty()) {
            ids.add("Circlet");
        }
        long seed;
        try {
            seed = rng.copy().randomLong();
        } catch (Throwable t) {
            return;
        }
        Collections.shuffle(ids, new java.util.Random(seed));
        AbstractRelic mask = RelicLibrary.getRelic(ids.get(0));
        ArrayList<PowerTip> tips = relicTips(mask);
        if (!tips.isEmpty()) {
            showTips(tips);
        }
    }

    // ---------------------------------------------------------- 蓝衣女子

    /** 原版买 1/2/3 瓶时逐瓶调 {@code PotionHelper.getRandomPotion()}，这里在 potionRng 副本上照做。 */
    private static void womanInBlue(AbstractEvent event, List<LargeDialogOptionButton> options) {
        if (!"INTRO".equals(String.valueOf(Reflect.get(event, "screen")))) {
            return;
        }
        int index = hoveredIndex(options, 3);
        if (index < 0) {
            return;
        }
        List<AbstractPotion> potions = NeowPredictor.randomPotions(index + 1);
        ArrayList<PowerTip> tips = new ArrayList<>();
        for (AbstractPotion potion : potions) {
            if (potion != null) {
                tips.add(new PowerTip(potion.name, potion.description));
            }
        }
        if (!tips.isEmpty()) {
            showTips(tips);
        }
    }

    // ---------------------------------------------------------- 成瘾者

    /** 原版两个选项都是 {@code returnRandomScreenlessRelic(returnRandomRelicTier())}。 */
    private static void addict(AbstractEvent event, List<LargeDialogOptionButton> options) {
        Object screenNum = Reflect.get(event, "screenNum");
        if (!(screenNum instanceof Integer) || ((Integer) screenNum).intValue() != 0) {
            return;
        }
        if (hoveredIndex(options, 2) < 0) {
            return;
        }
        ArrayList<PowerTip> tips = relicTips(NeowPredictor.screenlessRelic());
        if (!tips.isEmpty()) {
            showTips(tips);
        }
    }

    // ---------------------------------------------------------------- 绘制

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

    /** 遗物的原版详情提示：图标 + 名字 + 效果。 */
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

    /** 用原版提示框显示（和原版遗物/药水详情同一套渲染）。 */
    private static void showTips(ArrayList<PowerTip> tips) {
        if (tips.isEmpty()) {
            return;
        }
        float y = (float) InputHelper.mY + TipHelper.calculateToAvoidOffscreen(tips, InputHelper.mY);
        TipHelper.queuePowerTips(470.0f * Settings.scale, y, tips);
    }

    /** 掉血之后还剩多少血，用在奖励文本 / 遗物详情前面提示。 */
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

    /** 掉这么多血之后还剩多少（玩家不存在时返回 -1）。 */
    private static int hpLeftInt(int damage) {
        AbstractPlayer player = AbstractDungeon.player;
        if (player == null) {
            return -1;
        }
        return Math.max(0, player.currentHealth - Math.max(0, damage));
    }

    private static void drawCardsAbove(SpriteBatch sb, List<AbstractCard> cards, LargeDialogOptionButton button) {
        float imageScale = Config.imageScale();
        float cardW = AbstractCard.IMG_WIDTH * imageScale;
        float cardH = AbstractCard.IMG_HEIGHT * imageScale;
        float gap = 12.0f * Settings.scale;
        int count = cards.size();
        float total = count * cardW + (count - 1) * gap;
        float left = button.hb.cX - total * 0.5f;
        float maxLeft = Settings.WIDTH - 16.0f * Settings.scale - total;
        if (left > maxLeft) {
            left = maxLeft;
        }
        if (left < 16.0f * Settings.scale) {
            left = 16.0f * Settings.scale;
        }
        float cy = button.hb.cY + button.hb.height * 0.5f + 20.0f * Settings.scale + cardH * 0.5f;
        float maxCy = Settings.HEIGHT - cardH * 0.5f - 12.0f * Settings.scale;
        if (cy > maxCy) {
            cy = maxCy;
        }
        sb.setColor(Color.WHITE);
        for (int i = 0; i < count; ++i) {
            AbstractCard card = cards.get(i);
            if (card == null) {
                continue;
            }
            card.current_x = left + cardW * 0.5f + i * (cardW + gap);
            card.current_y = cy;
            card.drawScale = imageScale;
            card.targetDrawScale = imageScale;
            card.render(sb);
        }
    }
}
