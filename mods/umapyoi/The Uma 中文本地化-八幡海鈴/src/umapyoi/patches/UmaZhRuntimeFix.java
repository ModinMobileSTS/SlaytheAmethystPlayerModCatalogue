package umapyoi.patches;

import basemod.BaseMod;
import basemod.interfaces.PostInitializeSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.localization.CardStrings;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 载入结束后的收尾修正：
 *
 * <p>1. 卡名订正（见 {@link #RENAMES}），同时改 CardStrings 与卡牌对象，后续生成的
 * 副本也会用新名字。
 * <p>2. 若某张牌的描述是在关键字注册之前构建的（卡牌在 EditCards 阶段就被构造并生成
 * 了描述），描述里会残留 {@code umapyoi:xxx} 这类未解析的前缀。这里在全部关键字就绪
 * 后检测并重建这些卡牌的描述。
 */
@SpireInitializer
public final class UmaZhRuntimeFix implements PostInitializeSubscriber {
    /** 卡牌 ID → 中文卡名。 */
    private static final Map<String, String> RENAMES = new LinkedHashMap<>();

    private static Method initializeTitle;

    static {
        RENAMES.put("umapyoi:GDoubleZero", "G00 1st.F\u221e");
        RENAMES.put("umapyoi:RedAce", "赤色王牌");
        RENAMES.put("umapyoi:BlueRoseCloser", "蓝蔷薇追猎者");
    }

    public static void initialize() {
        BaseMod.subscribe(new UmaZhRuntimeFix());
    }

    @Override
    public void receivePostInitialize() {
        try {
            applyFixes();
        } catch (Throwable ignored) {
            // 任何意外都不应影响游戏启动
        }
    }

    private static void applyFixes() {
        for (Map.Entry<String, String> entry : RENAMES.entrySet()) {
            try {
                CardStrings strings = CardCrawlGame.languagePack.getCardStrings(entry.getKey());
                if (strings != null) {
                    strings.NAME = entry.getValue();
                }
            } catch (Throwable ignored) {
                // 取不到就只依赖下面的卡对象改名
            }
        }

        for (Object o : CardLibrary.getAllCards()) {
            if (!(o instanceof AbstractCard)) {
                continue;
            }
            AbstractCard card = (AbstractCard) o;
            String newName = RENAMES.get(card.cardID);
            if (newName != null) {
                card.name = newName;
                card.originalName = newName;
                refreshTitle(card);
            }
            if (hasUnresolvedKeyword(card)) {
                card.initializeDescription();
            }
        }
    }

    /** initializeTitle 是包私有的，只能反射调用；用于按新卡名重算标题字号。 */
    private static void refreshTitle(AbstractCard card) {
        try {
            if (initializeTitle == null) {
                initializeTitle = AbstractCard.class.getDeclaredMethod("initializeTitle");
                initializeTitle.setAccessible(true);
            }
            initializeTitle.invoke(card);
        } catch (Throwable ignored) {
            // 标题布局刷新失败不影响卡名本身
        }
    }

    /**
     * 描述里出现了「命名空间:名字」形式的词，但该词没有被记进卡牌关键字表，
     * 说明这段描述是在关键字注册之前构建的，需要重建。
     */
    private static boolean hasUnresolvedKeyword(AbstractCard card) {
        String raw = card.rawDescription;
        if (raw == null || raw.isEmpty()) {
            return false;
        }
        for (String token : raw.split(" ")) {
            String t = token.trim();
            int idx = t.indexOf(':');
            if (idx <= 0 || idx >= t.length() - 1) {
                continue;
            }
            if (!isNamespace(t.substring(0, idx))) {
                continue;
            }
            if (!card.keywords.contains(t.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNamespace(String s) {
        if (s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.';
            if (!ok) {
                return false;
            }
        }
        return true;
    }
}
