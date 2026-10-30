package umapyoi.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import java.util.regex.Pattern;

/**
 * 修复「带 ! 的动态变量后面紧跟中文标点」时变量不被替换、卡面显示字面量的问题。
 *
 * <p>中文描述构建管线会把 {@code !key!} 包装成 {@code " " + word + "! "}，再用正则
 * {@code !(.+)!!} 取变量名。若原文写成 {@code !umapyoi:m2!。}（中间没有空格），
 * 拼出来的 token 是 {@code !umapyoi:m2!。!}，捕获到的键名变成 {@code umapyoi:m2!。}，
 * 匹配失败 → 不替换 → 之后原版逻辑会把 {@code !} 全部删掉，于是卡面上就出现
 * {@code umapyoi:m2} 这样的裸文本。
 *
 * <p>因此在构建描述之前，把「{@code !xxx!} 紧跟中文标点或汉字」的情况补一个空格，
 * 让动态变量成为独立 token。对 {@code {!M!|>0=+}} 这类大括号动态块无影响（块内
 * {@code !} 后面跟的是 {@code |} 等符号，不会命中）。
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
        __instance.rawDescription = GLUED_VARIABLE.matcher(raw).replaceAll("$1 ");
    }
}
