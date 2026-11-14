package potionfix;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.localization.PowerStrings;
import java.lang.reflect.Field;

/**
 * 父模组把文本缓存在各个职业类的 private static 字段里，这里优先改写该字段真正引用的那份对象，
 * 保证“改文本”作用在药水/能力构造时实际读取的数据上；反射失败时退回语言包查询。
 */
final class LocalizedText {
    private LocalizedText() {
    }

    static void potionDescription(Class<?> potionClass, String potionId, int index, String text) {
        PotionStrings strings = potionStrings(potionClass, potionId);
        if (strings == null || strings.DESCRIPTIONS == null) {
            return;
        }
        if (index < 0 || index >= strings.DESCRIPTIONS.length) {
            return;
        }
        strings.DESCRIPTIONS[index] = text;
    }

    static void powerDescription(Class<?> powerClass, String powerId, int index, String text) {
        PowerStrings strings = powerStrings(powerClass, powerId);
        if (strings == null || strings.DESCRIPTIONS == null) {
            return;
        }
        if (index < 0 || index >= strings.DESCRIPTIONS.length) {
            return;
        }
        strings.DESCRIPTIONS[index] = text;
    }

    private static PotionStrings potionStrings(Class<?> potionClass, String potionId) {
        try {
            Field field = potionClass.getDeclaredField("potionStrings");
            field.setAccessible(true);
            Object value = field.get(null);
            if (value instanceof PotionStrings) {
                return (PotionStrings) value;
            }
        } catch (Throwable ignored) {
            // 字段改名或被混淆时退回语言表查询
        }
        try {
            return CardCrawlGame.languagePack.getPotionString(potionId);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static PowerStrings powerStrings(Class<?> powerClass, String powerId) {
        try {
            Field field = powerClass.getDeclaredField("powerStrings");
            field.setAccessible(true);
            Object value = field.get(null);
            if (value instanceof PowerStrings) {
                return (PowerStrings) value;
            }
        } catch (Throwable ignored) {
            // 字段改名或被混淆时退回语言表查询
        }
        try {
            return CardCrawlGame.languagePack.getPowerStrings(powerId);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
