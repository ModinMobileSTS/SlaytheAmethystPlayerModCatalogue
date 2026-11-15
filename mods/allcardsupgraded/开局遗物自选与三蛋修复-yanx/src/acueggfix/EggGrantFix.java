package acueggfix;

import basemod.BaseMod;
import basemod.interfaces.PostDungeonInitializeSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.relics.FrozenEgg2;
import com.megacrit.cardcrawl.relics.MoltenEgg2;
import com.megacrit.cardcrawl.relics.ToxicEgg2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

/**
 * 接管 All Cards Upgraded 的开局发蛋逻辑，修掉父 mod 的几处问题：
 * 1. 父 mod 的 eggsGrantedThisRun 在检查前就被重置，标记永远为 false，等于没有去重；
 * 2. 父 mod 只判断 actNum == 1，读档或 act 1 内重新初始化时会再次 instantObtain，出现重复遗物；
 * 3. 发放前没有 hasRelic 检查，已持有三蛋时仍会再发一份；
 * 4. 只从 uncommonRelicPool 移除三蛋，其它池子里残留。
 */
@SpireInitializer
public class EggGrantFix implements PostDungeonInitializeSubscriber {

    private static final Logger logger = LogManager.getLogger("AllCardsUpgradedFix");

    private static final String MOLTEN_EGG_ID = "Molten Egg 2";
    private static final String TOXIC_EGG_ID = "Toxic Egg 2";
    private static final String FROZEN_EGG_ID = "Frozen Egg 2";

    public EggGrantFix() {
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        new EggGrantFix();
    }

    @Override
    public void receivePostDungeonInitialize() {
        AbstractPlayer player = AbstractDungeon.player;
        if (player == null) {
            return;
        }
        // 只在开局（第 0 层）发放。换层、读档、act 1 内重新初始化都会因为 floorNum > 0 被挡掉，
        // 这正是父 mod 重复发蛋的来源。
        if (AbstractDungeon.floorNum > 0) {
            return;
        }
        grantEggs(player);
        removeEggsFromRelicPools();
        upgradeStartingDeck(player);
    }

    private static void grantEggs(AbstractPlayer player) {
        AbstractRelic[] eggs = new AbstractRelic[]{
                new MoltenEgg2(),
                new ToxicEgg2(),
                new FrozenEgg2()
        };
        for (AbstractRelic egg : eggs) {
            if (player.hasRelic(egg.relicId)) {
                // 已经持有（父 mod 之前发过 / 其它 mod 给过），不再发第二份
                continue;
            }
            egg.instantObtain();
            logger.info("Granted starting relic: " + egg.relicId);
        }
    }

    private static void removeEggsFromRelicPools() {
        removeAll(AbstractDungeon.commonRelicPool, MOLTEN_EGG_ID);
        removeAll(AbstractDungeon.commonRelicPool, TOXIC_EGG_ID);
        removeAll(AbstractDungeon.commonRelicPool, FROZEN_EGG_ID);
        removeAll(AbstractDungeon.uncommonRelicPool, MOLTEN_EGG_ID);
        removeAll(AbstractDungeon.uncommonRelicPool, TOXIC_EGG_ID);
        removeAll(AbstractDungeon.uncommonRelicPool, FROZEN_EGG_ID);
        removeAll(AbstractDungeon.rareRelicPool, MOLTEN_EGG_ID);
        removeAll(AbstractDungeon.rareRelicPool, TOXIC_EGG_ID);
        removeAll(AbstractDungeon.rareRelicPool, FROZEN_EGG_ID);
        removeAll(AbstractDungeon.shopRelicPool, MOLTEN_EGG_ID);
        removeAll(AbstractDungeon.shopRelicPool, TOXIC_EGG_ID);
        removeAll(AbstractDungeon.shopRelicPool, FROZEN_EGG_ID);
        removeAll(AbstractDungeon.bossRelicPool, MOLTEN_EGG_ID);
        removeAll(AbstractDungeon.bossRelicPool, TOXIC_EGG_ID);
        removeAll(AbstractDungeon.bossRelicPool, FROZEN_EGG_ID);
    }

    private static void removeAll(ArrayList<String> pool, String relicId) {
        if (pool == null) {
            return;
        }
        for (int i = pool.size() - 1; i >= 0; i--) {
            if (relicId.equals(pool.get(i))) {
                pool.remove(i);
            }
        }
    }

    private static void upgradeStartingDeck(AbstractPlayer player) {
        if (player.masterDeck == null) {
            return;
        }
        int upgraded = 0;
        for (AbstractCard card : player.masterDeck.group) {
            if (card == null || !card.canUpgrade()) {
                continue;
            }
            card.upgrade();
            upgraded++;
        }
        logger.info("Upgraded " + upgraded + " cards in starting deck");
    }
}
