package neowpreview;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.PotionHelper;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.neow.NeowEvent;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

import java.util.ArrayList;

/**
 * 选择祝福前的存档快照：金币、生命、卡组、遗物、药水，以及所有可能被祝福消耗的随机数。
 *
 * <p>同一局里撤回就用内存中这份快照；读档（SL）之后靠 {@link #encode()} / {@link #decode(String)}
 * 把同样的内容写到磁盘上（见 {@link RestorePoint}），这样重开游戏也还能撤回。</p>
 */
public class Snapshot {

    /** 当前这局的种子，用来防止把上一局的撤回点套到这局上。 */
    public Long seed;

    /** 这一组是“两选项的小型祝福”还是完整的四个（撤回时要还原成同一组）。 */
    public boolean mini;

    private int gold;
    private int hp;
    private int maxHp;

    private final ArrayList<AbstractCard> deck = new ArrayList<>();
    private final ArrayList<AbstractRelic> relics = new ArrayList<>();
    private final ArrayList<AbstractPotion> potions = new ArrayList<>();
    private final ArrayList<AbstractRelic> roomRelics = new ArrayList<>();

    private Random neowRng;
    private Random cardRng;
    private Random relicRng;
    private Random potionRng;

    private ArrayList<String> commonPool;
    private ArrayList<String> uncommonPool;
    private ArrayList<String> rarePool;
    private ArrayList<String> shopPool;
    private ArrayList<String> bossPool;

    public Snapshot() {
        this.seed = Settings.seed;
        AbstractPlayer p = AbstractDungeon.player;
        if (p != null) {
            gold = p.gold;
            hp = p.currentHealth;
            maxHp = p.maxHealth;
            if (p.masterDeck != null && p.masterDeck.group != null) {
                for (AbstractCard c : p.masterDeck.group) {
                    deck.add(c.makeStatEquivalentCopy());
                }
            }
            if (p.relics != null) {
                relics.addAll(p.relics);
            }
            if (p.potions != null) {
                potions.addAll(p.potions);
            }
        }
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        if (room != null && room.relics != null) {
            roomRelics.addAll(room.relics);
        }
        neowRng = copyOf(NeowEvent.rng);
        cardRng = copyOf(AbstractDungeon.cardRng);
        relicRng = copyOf(AbstractDungeon.relicRng);
        potionRng = copyOf(AbstractDungeon.potionRng);
        commonPool = copyPool(AbstractDungeon.commonRelicPool);
        uncommonPool = copyPool(AbstractDungeon.uncommonRelicPool);
        rarePool = copyPool(AbstractDungeon.rareRelicPool);
        shopPool = copyPool(AbstractDungeon.shopRelicPool);
        bossPool = copyPool(AbstractDungeon.bossRelicPool);
    }

    /** 从磁盘读回来的快照：不包含随机数（读档后原版本就会按种子重播随机数）。 */
    private Snapshot(Long seed) {
        this.seed = seed;
    }

    public void restore() {
        AbstractPlayer p = AbstractDungeon.player;
        if (p != null) {
            p.gold = gold;
            p.displayGold = gold;
            p.maxHealth = maxHp;
            p.currentHealth = hp;
            try {
                p.healthBarUpdatedEvent();
            } catch (Throwable ignored) {
            }
            if (p.masterDeck != null && p.masterDeck.group != null) {
                p.masterDeck.group.clear();
                for (AbstractCard c : deck) {
                    p.masterDeck.group.add(c.makeStatEquivalentCopy());
                }
            }
            if (p.relics != null) {
                p.relics.clear();
                p.relics.addAll(relics);
                try {
                    p.reorganizeRelics();
                } catch (Throwable ignored) {
                }
            }
            if (p.potions != null) {
                p.potions.clear();
                p.potions.addAll(potions);
                try {
                    p.adjustPotionPositions();
                } catch (Throwable ignored) {
                }
            }
        }
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        if (room != null && room.relics != null) {
            room.relics.clear();
            room.relics.addAll(roomRelics);
        }
        if (neowRng != null) {
            NeowEvent.rng = neowRng.copy();
        }
        if (cardRng != null) {
            AbstractDungeon.cardRng = cardRng.copy();
        }
        if (relicRng != null) {
            AbstractDungeon.relicRng = relicRng.copy();
        }
        if (potionRng != null) {
            AbstractDungeon.potionRng = potionRng.copy();
        }
        restorePool(AbstractDungeon.commonRelicPool, commonPool);
        restorePool(AbstractDungeon.uncommonRelicPool, uncommonPool);
        restorePool(AbstractDungeon.rareRelicPool, rarePool);
        restorePool(AbstractDungeon.shopRelicPool, shopPool);
        restorePool(AbstractDungeon.bossRelicPool, bossPool);
    }

    // ------------------------------------------------------------------ 序列化

    /**
     * 压成一行文本（不含换行，避免配置文件转义问题）。
     * 版式：{@code v1;种子;金币,生命,上限;D|卡id,升级数,misc;R|遗物id,计数;P|药水id;L五个池子}
     */
    public String encode() {
        StringBuilder sb = new StringBuilder("v1");
        sb.append(';').append(seed == null ? "" : String.valueOf(seed));
        sb.append(';').append(gold).append(',').append(hp).append(',').append(maxHp);
        sb.append(";D");
        for (AbstractCard c : deck) {
            sb.append('|').append(c.cardID).append(',').append(c.timesUpgraded).append(',').append(c.misc);
        }
        sb.append(";R");
        for (AbstractRelic r : relics) {
            sb.append('|').append(r.relicId).append(',').append(relicCounter(r));
        }
        sb.append(";P");
        for (AbstractPotion p : potions) {
            sb.append('|').append(p.ID);
        }
        sb.append(";L").append(join(commonPool)).append('|').append(join(uncommonPool))
                .append('|').append(join(rarePool)).append('|').append(join(shopPool))
                .append('|').append(join(bossPool));
        sb.append(";M").append(mini ? "1" : "0");
        return sb.toString();
    }

    public static Snapshot decode(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        String[] parts = data.split(";", -1);
        if (parts.length < 3 || !"v1".equals(parts[0])) {
            return null;
        }
        Long seed = null;
        try {
            if (!parts[1].isEmpty()) {
                seed = Long.valueOf(parts[1]);
            }
        } catch (Throwable ignored) {
        }
        Snapshot sn = new Snapshot(seed);
        String[] stats = parts[2].split(",", -1);
        sn.gold = parseInt(stats, 0);
        sn.hp = parseInt(stats, 1);
        sn.maxHp = parseInt(stats, 2);
        for (int i = 3; i < parts.length; i++) {
            String seg = parts[i];
            if (seg.isEmpty()) {
                continue;
            }
            char tag = seg.charAt(0);
            String body = seg.substring(1);
            try {
                if (tag == 'D') {
                    sn.decodeDeck(body);
                } else if (tag == 'R') {
                    sn.decodeRelics(body);
                } else if (tag == 'P') {
                    sn.decodePotions(body);
                } else if (tag == 'L') {
                    sn.decodePools(body);
                } else if (tag == 'M') {
                    sn.mini = "1".equals(body);
                }
            } catch (Throwable ignored) {
            }
        }
        return sn;
    }

    private void decodeDeck(String body) {
        for (String item : splitEntries(body)) {
            String[] f = item.split(",", -1);
            if (f.length < 1 || f[0].isEmpty()) {
                continue;
            }
            int upgrades = f.length > 1 ? parseInt(f, 1) : 0;
            int misc = f.length > 2 ? parseInt(f, 2) : 0;
            AbstractCard card = CardLibrary.getCopy(f[0], upgrades, misc);
            if (card != null) {
                deck.add(card);
            }
        }
    }

    private void decodeRelics(String body) {
        for (String item : splitEntries(body)) {
            String[] f = item.split(",", -1);
            if (f.length < 1 || f[0].isEmpty()) {
                continue;
            }
            AbstractRelic relic = RelicLibrary.getRelic(f[0]);
            if (relic == null) {
                continue;
            }
            AbstractRelic copy = relic.makeCopy();
            int counter = f.length > 1 ? parseInt(f, 1) : -1;
            Reflect.set(copy, "counter", Integer.valueOf(counter));
            relics.add(copy);
        }
    }

    private void decodePotions(String body) {
        for (String item : splitEntries(body)) {
            if (item.isEmpty()) {
                continue;
            }
            AbstractPotion potion = PotionHelper.getPotion(item);
            if (potion != null) {
                potions.add(potion);
            }
        }
    }

    private void decodePools(String body) {
        String[] f = body.split("\\|", -1);
        if (f.length < 5) {
            return;
        }
        commonPool = parsePool(f[0]);
        uncommonPool = parsePool(f[1]);
        rarePool = parsePool(f[2]);
        shopPool = parsePool(f[3]);
        bossPool = parsePool(f[4]);
    }

    private static String[] splitEntries(String body) {
        if (body.isEmpty()) {
            return new String[0];
        }
        return body.substring(1).split("\\|", -1);
    }

    private static ArrayList<String> parsePool(String csv) {
        ArrayList<String> out = new ArrayList<>();
        if (csv == null || csv.isEmpty()) {
            return out;
        }
        for (String s : csv.split(",", -1)) {
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }

    private static String join(ArrayList<String> pool) {
        if (pool == null || pool.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pool.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(pool.get(i));
        }
        return sb.toString();
    }

    private static int parseInt(String[] values, int index) {
        try {
            return index < values.length ? Integer.parseInt(values[index].trim()) : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    private static int relicCounter(AbstractRelic relic) {
        Object c = Reflect.get(relic, "counter");
        return c instanceof Integer ? (Integer) c : -1;
    }

    private static Random copyOf(Random rng) {
        try {
            return rng == null ? null : rng.copy();
        } catch (Throwable t) {
            return null;
        }
    }

    private static ArrayList<String> copyPool(ArrayList<String> pool) {
        return pool == null ? null : new ArrayList<>(pool);
    }

    private static void restorePool(ArrayList<String> field, ArrayList<String> snapshot) {
        if (field == null || snapshot == null || snapshot.isEmpty()) {
            return;
        }
        field.clear();
        field.addAll(snapshot);
    }
}
