package fnhbotany;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import fearandhunger.cards.souls.shadowed.AdvancedBotanism;
import fearandhunger.powers.shadowed.BotanismPower;
import fearandhunger.util.CustomTags;
import java.util.ArrayList;

/**
 * 【高阶植物学】是能力牌，但它依赖的植物牌全部是一次性消耗牌，能力生效后往往已无植物可用。
 *
 * <p>本补丁给该能力追加一个回收效果：拥有 BotanismPower 时，每回合开始时从消耗牌堆中随机取
 * 1 张植物牌（带有 CustomTags.PLANT 的牌）或【熟知灌丛】放入抽牌堆；升级后最多 2 张；若消耗牌堆
 * 中没有这类牌则什么都不做。</p>
 *
 * <p>卡面文本通过给 initializeDescription 加前缀钩子来同步：每次重建描述前，先把 rawDescription
 * 归一化为「原版描述 + 本补丁说明」，因此构造、升级以及之后任何一次描述刷新都不会丢失新增文字，
 * 也不会重复叠加；另外补一个 upp 钩子，保证原模组没有 UPGRADE_DESCRIPTION 时升级后文本也会刷新。</p>
 */
public final class BotanyRecall {

    private static final String BOTANY_ID = "fnh:AdvancedBotanism";
    private static final String UNDERGROWTH_ID = "fnh:UndergrowthAwareness";
    /** 升级后每回合最多回收的张数。 */
    private static final int MAX_RETURN = 2;

    private BotanyRecall() {
    }

    /** 在重建卡面描述之前，把【高阶植物学】的描述统一写成「原版描述 + 新增效果」。 */
    @SpirePatch2(
        clz = AbstractCard.class,
        method = "initializeDescription",
        paramtypez = {}
    )
    public static class BotanyDescription {

        @SpirePrefixPatch
        public static void Prefix(AbstractCard __instance) {
            if (!(__instance instanceof AdvancedBotanism)) {
                return;
            }
            __instance.rawDescription = composeText(__instance);
        }
    }

    /**
     * 升级后立刻刷新描述。原模组只有在卡牌带 UPGRADE_DESCRIPTION 时才会自己刷新文本，
     * 【高阶植物学】没有该字段，因此这里主动重建一次，让升级描述变成「最多 2 张」。
     */
    @SpirePatch2(
        clz = AdvancedBotanism.class,
        method = "upp",
        paramtypez = {}
    )
    public static class BotanyUpgradeText {

        @SpirePostfixPatch
        public static void Postfix(AdvancedBotanism __instance) {
            __instance.rawDescription = composeText(__instance);
            __instance.initializeDescription();
        }
    }

    /** 回合开始时回收已消耗的植物牌；BotanismPower 自身没有覆盖 atStartOfTurn，所以会走到这里。 */
    @SpirePatch2(
        clz = AbstractPower.class,
        method = "atStartOfTurn",
        paramtypez = {}
    )
    public static class ReturnExhaustedPlants {

        @SpirePostfixPatch
        public static void Postfix(AbstractPower __instance) {
            if (!(__instance instanceof BotanismPower)) {
                return;
            }
            if (AbstractDungeon.player == null || __instance.owner != AbstractDungeon.player) {
                return;
            }
            if (AbstractDungeon.getCurrRoom() == null) {
                return;
            }
            int count = Math.max(1, Math.min(__instance.amount, MAX_RETURN));
            for (int i = 0; i < count; i++) {
                AbstractCard card = randomExhaustedPlant();
                if (card == null) {
                    return;
                }
                __instance.flash();
                AbstractDungeon.player.exhaustPile.moveToDeck(card, true);
            }
        }
    }

    private static AbstractCard randomExhaustedPlant() {
        if (AbstractDungeon.player == null) {
            return null;
        }
        ArrayList<AbstractCard> candidates = new ArrayList<AbstractCard>();
        for (AbstractCard card : AbstractDungeon.player.exhaustPile.group) {
            if (isPlant(card)) {
                candidates.add(card);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(AbstractDungeon.cardRandomRng.random(candidates.size() - 1));
    }

    private static boolean isPlant(AbstractCard card) {
        if (card == null) {
            return false;
        }
        return card.hasTag(CustomTags.PLANT) || UNDERGROWTH_ID.equals(card.cardID);
    }

    private static String composeText(AbstractCard card) {
        String raw = card.rawDescription == null ? "" : card.rawDescription;
        String base = raw.replace(extraText(false), "").replace(extraText(true), "");
        if (base.isEmpty()) {
            CardStrings strings = CardCrawlGame.languagePack.getCardStrings(BOTANY_ID);
            if (strings != null) {
                if (card.upgraded
                    && strings.UPGRADE_DESCRIPTION != null
                    && !strings.UPGRADE_DESCRIPTION.isEmpty()) {
                    base = strings.UPGRADE_DESCRIPTION;
                } else if (strings.DESCRIPTION != null) {
                    base = strings.DESCRIPTION;
                }
            }
        }
        return base + extraText(card.upgraded);
    }

    private static String extraText(boolean upgraded) {
        String name = undergrowthName();
        if (Settings.language == Settings.GameLanguage.ZHS) {
            return upgraded
                ? " NL 每回合开始时，最多将 " + MAX_RETURN + " 张已消耗的植物或【" + name + "】放入抽牌堆。"
                : " NL 每回合开始时，将 1 张已消耗的植物或【" + name + "】放入抽牌堆。";
        }
        return upgraded
            ? " NL At the start of your turn, put up to " + MAX_RETURN
                + " exhausted Plant or " + name + " into your draw pile."
            : " NL At the start of your turn, put 1 exhausted Plant or " + name
                + " into your draw pile.";
    }

    private static String undergrowthName() {
        CardStrings strings = CardCrawlGame.languagePack.getCardStrings(UNDERGROWTH_ID);
        if (strings != null && strings.NAME != null && !strings.NAME.isEmpty()) {
            return strings.NAME;
        }
        return "熟知灌丛";
    }
}
