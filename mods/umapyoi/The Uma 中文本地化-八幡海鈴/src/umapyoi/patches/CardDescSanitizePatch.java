package umapyoi.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import java.util.regex.Pattern;

/**
 * 在每次构建卡面描述之前整理 rawDescription：
 *
 * <p>1. 去掉 {@code umapyoi:} / {@code stslib:} 这类命名空间前缀（见 {@link ZhKeywords}）。
 * 卡牌对象是在关键字注册之前构造的，那时关键字表里还没有这些键，中文描述管线会把
 * {@code umapyoi:心情}、{@code stslib:Soulbound} 当成普通文字，直接画到卡面上。
 * 改成裸中文名后，无论关键字是否已经注册，卡面都不会再出现原始 ID。
 *
 * <p>2. 修复「带 ! 的动态变量后面紧跟中文标点」时变量不被替换的问题。中文描述构建管线
 * 会把 {@code !key!} 包装成 {@code " " + word + "! "}，再用正则 {@code !(.+)!!} 取变量名。
 * 若原文写成 {@code !umapyoi:m2!。}（中间没有空格），拼出来的 token 是
 * {@code !umapyoi:m2!。!}，捕获到的键名变成 {@code umapyoi:m2!。}，匹配失败后原版
 * 逻辑会把 {@code !} 全部删掉，卡面上就出现 {@code umapyoi:m2} 这样的裸文本。
 */
@SpirePatch2(clz = AbstractCard.class, method = "initializeDescription", paramtypez = {})
public final class CardDescSanitizePatch {
    private static final Pattern GLUED_VARIABLE = Pattern.compile(
            "(![^\\s!]+!)(?=[。，、；：！？）》】」』]|\\p{IsHan})");

    private CardDescSanitizePatch() {
    }

    @SpirePrefixPatch
    public static void Prefix(AbstractCard __instance) {
        String raw = __instance.rawDescription;
        if (raw == null || raw.isEmpty()) {
            return;
        }
        String cleaned = ZhKeywords.clean(raw);
        if (cleaned != null && !cleaned.equals(raw)) {
            raw = cleaned;
        }
        __instance.rawDescription = GLUED_VARIABLE.matcher(raw).replaceAll("$1 ");
    }
}
