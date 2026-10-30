package umapyoi.patches;

import basemod.BaseMod;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.evacipated.cardcrawl.modthespire.lib.ByRef;
import com.evacipated.cardcrawl.modthespire.lib.LineFinder;
import com.evacipated.cardcrawl.modthespire.lib.Matcher;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertLocator;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.cards.AbstractCard;
import javassist.CannotCompileException;
import javassist.CtBehavior;

/**
 * 中文卡面描述里去掉「modid:」关键字前缀。
 *
 * <p>BaseMod 自带的 {@code CNUniqueKeywords} 在中文描述里调用
 * {@code BaseMod.keywordIsUnique(word)} 时使用原始大小写，而关键字表里的键一律是
 * 作者书写的形式（例如 StSLib 的 {@code stslib:soulbound} / {@code stslib:purge} /
 * {@code stslib:fleeting}）。因此 {@code stslib:Soulbound} 这类首字母大写的写法匹配失败，
 * 前缀不会被去掉，卡面上就会直接显示 {@code stslib:Soulbound} 这样的原始 ID。
 *
 * <p>本补丁插在同一个位置（关键字分支内的第一次 {@code GlyphLayout.setText}），
 * 用小写比较 + 已知命名空间双重判断来去前缀，并且保留作者书写的大小写用于显示。
 */
@SpirePatch2(clz = AbstractCard.class, method = "initializeDescriptionCN", paramtypez = {})
public final class CnKeywordPrefixFix {
    /** 诊断用：实际去掉了多少次前缀。 */
    public static int hits;

    private CnKeywordPrefixFix() {
    }

    @SpireInsertPatch(locator = Locator.class, localvars = {"word"})
    public static void Insert(AbstractCard __instance, @ByRef String[] word) {
        String w = word[0];
        if (w == null) {
            return;
        }
        int idx = w.indexOf(':');
        if (idx <= 0 || idx >= w.length() - 1) {
            return;
        }
        if (!isNamespace(w.substring(0, idx))) {
            return;
        }
        String lower = w.toLowerCase();
        if (BaseMod.keywordIsUnique(lower) || lower.startsWith("umapyoi:") || lower.startsWith("stslib:")) {
            word[0] = w.substring(idx + 1);
            hits++;
        }
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

    public static final class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
            Matcher.MethodCallMatcher matcher = new Matcher.MethodCallMatcher(GlyphLayout.class, "setText");
            return LineFinder.findInOrder(ctMethodToPatch, matcher);
        }
    }
}
