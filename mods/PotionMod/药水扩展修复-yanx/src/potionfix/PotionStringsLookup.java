package potionfix;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PotionStrings;
import potionmod.potions.GhostPotion;

import java.lang.reflect.Field;

/**
 * GhostPotion 只在自己的构造函数里使用 private static 的 potionStrings，
 * 这里优先取该字段实际引用的对象，保证修复作用在构造时真正读取的那份文本上。
 */
final class PotionStringsLookup {
    static final String GHOST_POTION_ID = "PotionMod:GhostPotion";

    private PotionStringsLookup() {
    }

    static PotionStrings ghostStrings() {
        try {
            Field field = GhostPotion.class.getDeclaredField("potionStrings");
            field.setAccessible(true);
            PotionStrings strings = (PotionStrings) field.get(null);
            if (strings != null) {
                return strings;
            }
        } catch (Throwable ignored) {
            // 字段改名或被混淆时退回语言表查询
        }
        return CardCrawlGame.languagePack.getPotionString(GHOST_POTION_ID);
    }
}
