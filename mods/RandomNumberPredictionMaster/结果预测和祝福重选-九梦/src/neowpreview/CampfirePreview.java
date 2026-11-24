package neowpreview;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.ui.campfire.AbstractCampfireOption;
import com.megacrit.cardcrawl.ui.campfire.DigOption;
import com.megacrit.cardcrawl.ui.campfire.RestOption;
import java.util.ArrayList;

/**
 * 篝火（休息处）里两处随机结果的预览：
 *
 * <ul>
 *   <li>铲子的「挖」：原版 {@code CampfireDigEffect} 用 {@code returnRandomRelicTier()} 抽稀有度，
 *       再取对应池子的第一件；这里在 relicRng 副本上算出同一件，悬停按钮就把遗物图标画出来。</li>
 *   <li>睡梦捕手的「休息」：原版 {@code CampfireSleepEffect} 结束时调 {@code AbstractDungeon.getRewardCards()}，
 *       这里在 cardRng 副本上算出同样的三张牌。</li>
 * </ul>
 *
 * <p>烟斗（移除卡牌）、石铃（固定提举）、帝王枕（固定多回血）以及禁用的咖啡滤壶／融合锤都没有随机结果，
 * 不需要预测。</p>
 */
public final class CampfirePreview {

    private static final String DREAM_CATCHER = "Dream Catcher";

    private CampfirePreview() {
    }

    /** 悬停篝火选项时调用。 */
    public static void render(SpriteBatch sb, AbstractCampfireOption option) {
        if (sb == null || option == null || AbstractDungeon.player == null) {
            return;
        }
        try {
            if (!option.hb.hovered) {
                return;
            }
            if (option instanceof DigOption) {
                dig(sb, option);
            } else if (option instanceof RestOption && AbstractDungeon.player.hasRelic(DREAM_CATCHER)) {
                dreamCatcher(sb, option);
            }
        } catch (Throwable ignored) {
            // 预测失败只放弃显示，不影响游戏本身
        }
    }

    /** 复刻 CampfireDigEffect：relicRng 抽稀有度 → 取该池第一件。 */
    private static void dig(SpriteBatch sb, AbstractCampfireOption option) {
        RelicUpgradePreview.Payload payload = PreviewCache.CARDS.get(option,
                "dig|" + RelicUpgradePreview.stateKey(), () -> {
                    RelicUpgradePreview.Payload p = new RelicUpgradePreview.Payload();
                    Random saved = AbstractDungeon.relicRng;
                    if (saved == null) {
                        return p;
                    }
                    AbstractRelic.RelicTier tier;
                    try {
                        AbstractDungeon.relicRng = saved.copy();
                        tier = AbstractDungeon.returnRandomRelicTier();
                    } catch (Throwable t) {
                        return p;
                    } finally {
                        AbstractDungeon.relicRng = saved;
                    }
                    AbstractRelic relic = NeowPredictor.relic(tier);
                    if (relic != null) {
                        p.relics.add(relic);
                    }
                    return p;
                });
        if (!payload.isEmpty()) {
            RelicUpgradePreview.drawCentered(sb, payload);
        }
    }

    /** 复刻 CampfireSleepEffect 里的睡梦捕手分支：三张卡牌奖励（cardRng）。 */
    private static void dreamCatcher(SpriteBatch sb, AbstractCampfireOption option) {
        RelicUpgradePreview.Payload payload = PreviewCache.CARDS.get(option,
                "dream|" + RelicUpgradePreview.stateKey(), () -> {
                    RelicUpgradePreview.Payload p = new RelicUpgradePreview.Payload();
                    Random saved = AbstractDungeon.cardRng;
                    if (saved == null) {
                        return p;
                    }
                    AbstractDungeon.cardRng = saved.copy();
                    try {
                        ArrayList<AbstractCard> cards = AbstractDungeon.getRewardCards();
                        if (cards != null) {
                            for (AbstractCard card : cards) {
                                if (card != null) {
                                    p.cards.add(card.makeCopy());
                                }
                            }
                        }
                    } catch (Throwable ignored) {
                    } finally {
                        AbstractDungeon.cardRng = saved;
                    }
                    return p;
                });
        if (!payload.isEmpty()) {
            RelicUpgradePreview.drawCentered(sb, payload);
        }
    }
}
