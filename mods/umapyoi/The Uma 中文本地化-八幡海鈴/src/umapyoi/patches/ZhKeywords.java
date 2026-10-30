package umapyoi.patches;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 把中文卡牌/遗物文本里的「命名空间:名字」关键字写法（例如 {@code umapyoi:心情}、
 * {@code stslib:Soulbound}）换成裸的中文关键字名（{@code 心情}、{@code 灵魂绑定}）。
 *
 * <p>为什么必须换掉：中文描述管线用 {@code GameDictionary.keywords.containsKey(word.toLowerCase())}
 * 判断一个词是不是关键字，而关键字表的键是「注册时书写的原样大小写 + modid 前缀」。
 * 卡牌对象是在关键字注册之前构造的，那时 {@code umapyoi:心情}、{@code stslib:Soulbound}
 * 都还不是关键字，于是被当成普通文字原样画到卡面上（卡牌总数里那些「监督」「腿部骨折」
 * 「三条七」「传承永存」就是这么露出原始 ID 的）。
 *
 * <p>换成裸中文名之后，只要再把这些裸名登记进关键字表（见 {@code UmaZhRuntimeFix}），
 * 就能照常拿到金色高亮与悬浮说明；即使登记失败，文本也只会是普通的「心情」，
 * 不会再把原始 ID 暴露到卡面上。
 *
 * <p>注意：中文译文里关键字常常紧贴前面的汉字，例如 {@code 你的回合开始时，umapyoi:限制}，
 * 所以命名空间要从冒号往回取「由命名空间字符组成的连续串」，而不是取整段冒号前的文字。
 */
public final class ZhKeywords {
    /** 本补丁认得并负责改写的命名空间。 */
    private static final Set<String> NAMESPACES = new HashSet<>(Arrays.asList("umapyoi", "stslib"));

    /** StSLib 三个关键字的官方中文名。 */
    private static final Map<String, String> DISPLAY = new HashMap<>();

    /** 允许出现在关键字名后面的标点，改写时原样保留。 */
    private static final String PUNCT = "。，、；：！？）》】」』”’,.;:!?)]}";

    static {
        DISPLAY.put("stslib:soulbound", "灵魂绑定");
        DISPLAY.put("stslib:purge", "抹除");
        DISPLAY.put("stslib:fleeting", "即逝");
    }

    private ZhKeywords() {
    }

    /** 把整段描述里的命名空间前缀去掉；遇到 null 原样返回。 */
    public static String clean(String text) {
        if (text == null || text.isEmpty() || text.indexOf(':') < 0) {
            return text;
        }
        String[] parts = text.split(" ", -1);
        StringBuilder sb = new StringBuilder(text.length() + 8);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(cleanToken(parts[i]));
        }
        return sb.toString();
    }

    private static String cleanToken(String token) {
        if (token.isEmpty()) {
            return token;
        }
        char first = token.charAt(0);
        // 动态变量 !xxx!、动态块 {!X!|...}、[E] 之类的能量图标都不动
        if (first == '!' || first == '{' || first == '[') {
            return token;
        }
        // 形如 "#yumapyoi:心情" 的颜色码 + 关键字，先拆掉颜色码再处理
        if (first == '#' && token.length() > 2) {
            String inner = cleanToken(token.substring(2));
            if (!inner.equals(token.substring(2))) {
                return "#" + token.charAt(1) + inner;
            }
            return token;
        }
        int colon = token.indexOf(':');
        if (colon <= 0 || colon >= token.length() - 1) {
            return token;
        }
        int start = colon;
        while (start > 0 && isNamespaceChar(token.charAt(start - 1))) {
            start--;
        }
        if (start == colon) {
            return token;
        }
        String namespace = token.substring(start, colon);
        if (!NAMESPACES.contains(namespace.toLowerCase(Locale.ROOT))) {
            return token;
        }
        String head = token.substring(0, start);
        String rest = token.substring(colon + 1);
        int end = rest.length();
        while (end > 0 && PUNCT.indexOf(rest.charAt(end - 1)) >= 0) {
            end--;
        }
        if (end == 0) {
            return token;
        }
        String name = rest.substring(0, end);
        String tail = rest.substring(end);
        String display = displayName(namespace, name) + tail;
        // 关键字紧贴前面的汉字时补一个空格，让它成为独立的词，才能拿到金色高亮
        return head.isEmpty() ? display : head + " " + display;
    }

    private static boolean isNamespaceChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.';
    }

    private static String displayName(String namespace, String name) {
        String mapped = DISPLAY.get((namespace + ":" + name).toLowerCase(Locale.ROOT));
        return mapped != null ? mapped : name;
    }
}
