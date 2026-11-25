package neowpreview;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.CampfireUI;
import com.megacrit.cardcrawl.ui.campfire.AbstractCampfireOption;
import com.megacrit.cardcrawl.ui.campfire.DigOption;
import com.megacrit.cardcrawl.ui.campfire.RestOption;
import java.util.ArrayList;
import java.util.List;

/**
 * 篝火（休息处）里两处随机结果的预览：
 *
 * <ul>
 *   <li>铲子的「挖」：用 relicRng 副本复刻 {@code CampfireDigEffect}（抽稀有度 → 取该池第一件），
 *       悬停按钮时用原版遗物提示框显示会挖到的那件遗物的名称与效果。</li>
 *   <li>睡梦捕手的「休息」：用 cardRng 副本复刻 {@code CampfireSleepEffect} 结尾的
 *       {@code AbstractDungeon.getRewardCards()}，把附加的三张牌画在屏幕上（画在所有篝火按钮之后，不会被遮挡）。</li>
 * </ul>
 *
 * <p>烟斗（移除卡牌）、石铃（固定提举）、帝王枕（固定多回血）以及禁用的咖啡滤壶／融合锤都没有随机结果，
 * 不需要预测。</p>
 */
public final class CampfirePreview {

    private static final String DREAM_CATCHER = "Dream Catcher";

    private CampfirePreview() {
    }

    /** 在 {@code CampfireUI.render} 之后调用：所有按钮都画完了，这里画的东西不会被压住。 */
    public static void render(SpriteBatch sb, CampfireUI ui) {
        if (sb == null || ui == null || CampfireUI.hidden || AbstractDungeon.player == null) {
            return;
        }
        Object buttonsObj = Reflect.get(ui, "buttons");
        if (!(buttonsObj instanceof List)) {
            return;
        }
        for (Object element : (List<?>) buttonsObj) {
            if (!(element instanceof AbstractCampfireOption)) {
                continue;
            }
            AbstractCampfireOption option = (AbstractCampfireOption) element;
            if (!option.hb.hovered || !option.usable) {
                continue;
            }
            try {
                if (option instanceof DigOption) {
                    dig(option);
                    return;
                }
                if (option instanceof RestOption && AbstractDungeon.player.hasRelic(DREAM_CATCHER)) {
                    dreamCatcher(sb, option);
                    return;
                }
            } catch (Throwable ignored) {
                // 预测失败只放弃显示，不影响游戏本身
            }
        }
    }

    /** 复刻 CampfireDigEffect：relicRng 抽稀有度 → 取该池第一件，然后用原版遗物提示框显示详情。 */
    private static void dig(AbstractCampfireOption option) {
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
        if (payload.relics.isEmpty()) {
            return;
        }
        AbstractRelic relic = payload.relics.get(0);
        ArrayList<PowerTip> tips = new ArrayList<>();
        if (relic.tips != null && !relic.tips.isEmpty()) {
            tips.addAll(relic.tips);
        } else {
            tips.add(new PowerTip(relic.name, relic.description));
        }
        TipHelper.queuePowerTips(InputHelper.mX + 50.0f * Settings.scale,
                InputHelper.mY + 50.0f * Settings.scale, tips);
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
            RelicUpgradePreview.drawCentered(sb, payload.cards);
        }
    }
}
