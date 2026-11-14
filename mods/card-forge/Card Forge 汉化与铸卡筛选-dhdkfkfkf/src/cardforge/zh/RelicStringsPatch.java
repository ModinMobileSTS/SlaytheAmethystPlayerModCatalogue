package cardforge.zh;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.localization.RelicStrings;

/**
 * 遗物名称与风味文本来自 RelicStrings 表，而 AbstractRelic 在构造时就把 NAME / FLAVOR /
 * DESCRIPTIONS 读进 final 字段，所以只能在查表这一步替换，遗物本体不必改动。
 *
 * <p>顺带把原 mod 英文名 "Hammer of  Thor" 中多出来的空格规范化；若查不到条目（例如玩家删换了
 * 本地化文件）则补一份字符串，避免 AbstractRelic 构造时空指针。</p>
 */
@SpirePatch2(
        cls = "com.megacrit.cardcrawl.localization.LocalizedStrings",
        method = "getRelicStrings",
        paramtypez = {String.class}
)
public final class RelicStringsPatch {
    private RelicStringsPatch() {
    }

    @SpirePostfixPatch
    public static RelicStrings Postfix(RelicStrings __result, Object[] __args) {
        if (__args == null || __args.length < 1 || !(__args[0] instanceof String)) {
            return __result;
        }
        String relicName = (String) __args[0];

        String name = Texts.name(relicName);
        if (name == null) {
            return __result;
        }
        String flavor = Texts.flavor(relicName);
        String description = Texts.description(relicName);

        if (__result == null) {
            RelicStrings created = new RelicStrings();
            created.NAME = name;
            created.FLAVOR = flavor;
            created.DESCRIPTIONS = new String[]{description};
            return created;
        }

        __result.NAME = name;
        __result.FLAVOR = flavor;
        __result.DESCRIPTIONS = new String[]{description};
        return __result;
    }
}
