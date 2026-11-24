package neowpreview;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.PotionHelper;
import com.megacrit.cardcrawl.neow.NeowEvent;
import com.megacrit.cardcrawl.neow.NeowReward;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.relics.NeowsLament;

import java.util.ArrayList;
import java.util.List;

/**
 * 用随机数的副本推算涅奥祝福的实际结果，全程不消耗真正的随机数。
 */
public final class NeowPredictor {

    private NeowPredictor() {
    }

    public static Preview predict(NeowReward reward) {
        Preview p = new Preview();
        if (reward == null || reward.type == null) {
            return p;
        }
        try {
            switch (reward.type) {
                case THREE_CARDS:
                    p.cards.addAll(rewardCards(false));
                    break;
                case THREE_RARE_CARDS:
                    p.cards.addAll(rewardCards(true));
                    break;
                case ONE_RANDOM_RARE_CARD: {
                    AbstractCard c = randomRareCard();
                    if (c != null) {
                        p.cards.add(c);
                    }
                    break;
                }
                case RANDOM_COLORLESS:
                    p.cards.addAll(colorlessCards(false));
                    break;
                case RANDOM_COLORLESS_2:
                    p.cards.addAll(colorlessCards(true));
                    break;
                case THREE_SMALL_POTIONS:
                    p.potions.addAll(potions());
                    break;
                case RANDOM_COMMON_RELIC: {
                    AbstractRelic r = relic(AbstractRelic.RelicTier.COMMON);
                    if (r != null) {
                        p.relic = r;
                    }
                    break;
                }
                case ONE_RARE_RELIC: {
                    AbstractRelic r = relic(AbstractRelic.RelicTier.RARE);
                    if (r != null) {
                        p.relic = r;
                    }
                    break;
                }
                case BOSS_RELIC: {
                    AbstractRelic r = relic(AbstractRelic.RelicTier.BOSS);
                    if (r != null) {
                        p.relic = r;
                    }
                    break;
                }
                case THREE_ENEMY_KILL:
                    p.relic = new NeowsLament();
                    break;
                case TRANSFORM_CARD:
                case TRANSFORM_TWO_CARDS:
                default:
                    break;
            }
        } catch (Throwable ignored) {
            // 预测失败时只放弃图片，不影响游戏本身
        }
        try {
            if (reward.drawback == NeowReward.NeowRewardDrawback.CURSE) {
                p.curse = curse(reward.type);
            }
        } catch (Throwable ignored) {
        }
        return p;
    }

    /** 复刻 NeowReward.getRewardCards，只消耗传入的随机数副本。 */
    private static List<AbstractCard> rewardCards(boolean rareOnly) {
        List<AbstractCard> out = new ArrayList<>();
        Random rng = copyOf(NeowEvent.rng);
        if (rng == null) {
            return out;
        }
        try {
            List<AbstractCard> raw = new ArrayList<>();
            for (int i = 0; i < 3; ++i) {
                AbstractCard.CardRarity rarity = rng.randomBoolean(0.33f)
                        ? AbstractCard.CardRarity.UNCOMMON : AbstractCard.CardRarity.COMMON;
                if (rareOnly) {
                    rarity = AbstractCard.CardRarity.RARE;
                }
                AbstractCard card = AbstractDungeon.getCard(rarity, rng);
                int guard = 0;
                while (card != null && raw.contains(card) && guard++ < 200) {
                    card = AbstractDungeon.getCard(rarity, rng);
                }
                if (card == null) {
                    break;
                }
                raw.add(card);
            }
            for (AbstractCard c : raw) {
                out.add(c.makeCopy());
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static AbstractCard randomRareCard() {
        Random rng = copyOf(NeowEvent.rng);
        if (rng == null) {
            return null;
        }
        try {
            AbstractCard c = AbstractDungeon.getCard(AbstractCard.CardRarity.RARE, rng);
            return c == null ? null : c.makeCopy();
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 复刻 NeowReward.getColorlessRewardCards。无色牌池取牌走的是 AbstractDungeon.cardRng，
     * 所以临时把 cardRng 换成副本，算完后还原。
     */
    private static List<AbstractCard> colorlessCards(boolean rareOnly) {
        List<AbstractCard> out = new ArrayList<>();
        Random neowRng = copyOf(NeowEvent.rng);
        Random saved = AbstractDungeon.cardRng;
        if (neowRng == null || saved == null) {
            return out;
        }
        AbstractDungeon.cardRng = saved.copy();
        try {
            List<AbstractCard> raw = new ArrayList<>();
            for (int i = 0; i < 3; ++i) {
                AbstractCard.CardRarity rarity = neowRng.randomBoolean(0.33f)
                        ? AbstractCard.CardRarity.UNCOMMON : AbstractCard.CardRarity.COMMON;
                if (rareOnly) {
                    rarity = AbstractCard.CardRarity.RARE;
                } else if (rarity == AbstractCard.CardRarity.COMMON) {
                    rarity = AbstractCard.CardRarity.UNCOMMON;
                }
                AbstractCard card = AbstractDungeon.getColorlessCardFromPool(rarity);
                int guard = 0;
                while (card != null && raw.contains(card) && guard++ < 200) {
                    card = AbstractDungeon.getColorlessCardFromPool(rarity);
                }
                if (card == null) {
                    break;
                }
                raw.add(card);
            }
            for (AbstractCard c : raw) {
                out.add(c.makeCopy());
            }
        } catch (Throwable ignored) {
        } finally {
            AbstractDungeon.cardRng = saved;
        }
        return out;
    }

    /**
     * 复刻 AbstractDungeon.returnRandomRelic：它会从遗物池里 remove(0)，
     * 所以先把五个池子临时换成副本，算完再把**原来的列表对象**放回去。
     * 注意不能对原列表做 clear()/addAll() —— 那样一旦中途出问题就会把遗物池清空，
     * 之后所有遗物奖励都会退化成 Circlet（看起来像“遗物池耗尽 / 未找到”）。
     * 另外 returnRandomRelicKey 本身不消耗随机数，所以这里也不用动 relicRng。
     */
    public static AbstractRelic relic(AbstractRelic.RelicTier tier) {
        ArrayList<String> common = AbstractDungeon.commonRelicPool;
        ArrayList<String> uncommon = AbstractDungeon.uncommonRelicPool;
        ArrayList<String> rare = AbstractDungeon.rareRelicPool;
        ArrayList<String> shop = AbstractDungeon.shopRelicPool;
        ArrayList<String> boss = AbstractDungeon.bossRelicPool;

        AbstractDungeon.commonRelicPool = copyPool(common);
        AbstractDungeon.uncommonRelicPool = copyPool(uncommon);
        AbstractDungeon.rareRelicPool = copyPool(rare);
        AbstractDungeon.shopRelicPool = copyPool(shop);
        AbstractDungeon.bossRelicPool = copyPool(boss);
        try {
            return AbstractDungeon.returnRandomRelic(tier);
        } catch (Throwable t) {
            return null;
        } finally {
            AbstractDungeon.commonRelicPool = common;
            AbstractDungeon.uncommonRelicPool = uncommon;
            AbstractDungeon.rareRelicPool = rare;
            AbstractDungeon.shopRelicPool = shop;
            AbstractDungeon.bossRelicPool = boss;
        }
    }

    /**
     * 原版「随机稀有度 → 无界面遗物」两步（事件里常见写法）：
     * 先用 {@code relicRng} 抽一次稀有度，再取对应池子的第一件。
     * 两步都在副本上做，不消耗真随机数、也不动真的遗物池。
     */
    public static AbstractRelic screenlessRelic() {
        Random saved = AbstractDungeon.relicRng;
        if (saved == null) {
            return null;
        }
        AbstractRelic.RelicTier tier;
        try {
            AbstractDungeon.relicRng = saved.copy();
            tier = AbstractDungeon.returnRandomRelicTier();
        } catch (Throwable t) {
            return null;
        } finally {
            AbstractDungeon.relicRng = saved;
        }
        return screenlessRelic(tier);
    }

    public static AbstractRelic screenlessRelic(AbstractRelic.RelicTier tier) {
        ArrayList<String> common = AbstractDungeon.commonRelicPool;
        ArrayList<String> uncommon = AbstractDungeon.uncommonRelicPool;
        ArrayList<String> rare = AbstractDungeon.rareRelicPool;
        ArrayList<String> shop = AbstractDungeon.shopRelicPool;
        ArrayList<String> boss = AbstractDungeon.bossRelicPool;
        AbstractDungeon.commonRelicPool = copyPool(common);
        AbstractDungeon.uncommonRelicPool = copyPool(uncommon);
        AbstractDungeon.rareRelicPool = copyPool(rare);
        AbstractDungeon.shopRelicPool = copyPool(shop);
        AbstractDungeon.bossRelicPool = copyPool(boss);
        try {
            return AbstractDungeon.returnRandomScreenlessRelic(tier);
        } catch (Throwable t) {
            return null;
        } finally {
            AbstractDungeon.commonRelicPool = common;
            AbstractDungeon.uncommonRelicPool = uncommon;
            AbstractDungeon.rareRelicPool = rare;
            AbstractDungeon.shopRelicPool = shop;
            AbstractDungeon.bossRelicPool = boss;
        }
    }

    /** 用 potionRng 的副本连续抽 count 瓶随机药水（和原版 PotionHelper.getRandomPotion() 同一路径）。 */
    public static List<AbstractPotion> randomPotions(int count) {
        List<AbstractPotion> out = new ArrayList<>();
        Random saved = AbstractDungeon.potionRng;
        if (saved == null || PotionHelper.potions == null || PotionHelper.potions.isEmpty() || count <= 0) {
            return out;
        }
        Random rng = saved.copy();
        try {
            for (int i = 0; i < count; ++i) {
                AbstractPotion potion = PotionHelper.getRandomPotion(rng);
                if (potion == null) {
                    break;
                }
                out.add(potion);
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static List<AbstractPotion> potions() {
        return randomPotions(3);
    }

    /**
     * 「获得好处但同时获得一张诅咒」这种代价的诅咒是哪张。
     *
     * <p>原版在 activate() 之后由 NeowReward.update() 调
     * {@code AbstractDungeon.getCardWithoutRng(CURSE)} → {@code CardLibrary.getCurse()}，
     * 后者用 {@code AbstractDungeon.cardRng} 抽一次。无色牌奖励会先用 cardRng 抽出三张，
     * 所以这里先在副本上把这一步模拟掉，再抽诅咒，才能和实际一致。</p>
     */
    private static AbstractCard curse(NeowReward.NeowRewardType type) {
        Random saved = AbstractDungeon.cardRng;
        if (saved == null) {
            return null;
        }
        AbstractDungeon.cardRng = saved.copy();
        try {
            if (type == NeowReward.NeowRewardType.RANDOM_COLORLESS
                    || type == NeowReward.NeowRewardType.RANDOM_COLORLESS_2) {
                simulateColorless(type == NeowReward.NeowRewardType.RANDOM_COLORLESS_2);
            }
            AbstractCard card = CardLibrary.getCurse();
            return card == null ? null : card.makeCopy();
        } catch (Throwable t) {
            return null;
        } finally {
            AbstractDungeon.cardRng = saved;
        }
    }

    /** 只消耗当前 cardRng（可能是副本）上的随机数，复刻一次无色牌三选一的取牌过程。 */
    private static void simulateColorless(boolean rareOnly) {
        Random neowRng = copyOf(NeowEvent.rng);
        if (neowRng == null) {
            return;
        }
        List<String> taken = new ArrayList<>();
        for (int i = 0; i < 3; ++i) {
            AbstractCard.CardRarity rarity = neowRng.randomBoolean(0.33f)
                    ? AbstractCard.CardRarity.UNCOMMON : AbstractCard.CardRarity.COMMON;
            if (rareOnly) {
                rarity = AbstractCard.CardRarity.RARE;
            } else if (rarity == AbstractCard.CardRarity.COMMON) {
                rarity = AbstractCard.CardRarity.UNCOMMON;
            }
            AbstractCard card = AbstractDungeon.getColorlessCardFromPool(rarity);
            int guard = 0;
            while (card != null && taken.contains(card.cardID) && guard++ < 200) {
                card = AbstractDungeon.getColorlessCardFromPool(rarity);
            }
            if (card == null) {
                break;
            }
            taken.add(card.cardID);
        }
    }

    private static Random copyOf(Random rng) {
        try {
            return rng == null ? null : rng.copy();
        } catch (Throwable t) {
            return null;
        }
    }

    private static ArrayList<String> copyPool(ArrayList<String> pool) {
        return pool == null ? new ArrayList<String>() : new ArrayList<>(pool);
    }
}
