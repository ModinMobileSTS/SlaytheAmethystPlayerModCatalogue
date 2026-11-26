package anonconfignullfix;

import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * SpireConfig.getInt(key) 的原实现是 Integer.parseInt(getString(key))。
 * 当 .properties 配置文件里没有这个 key 时 getString 返回 null，
 * Integer.parseInt(null) 直接抛出 NumberFormatException: null，
 * 这一异常没有任何兜底，最终把游戏顶到崩溃页。
 *
 * 这里在解析前先做判定：缺失、空白、非数字的值一律回退为 0，
 * 并把有问题的 key 写进日志，方便定位是哪个 Mod 的哪个配置项。
 */
@SpirePatch2(clz = SpireConfig.class, method = "getInt", paramtypez = {String.class})
public class SpireConfigIntPatch {

    private static final Logger logger = LogManager.getLogger("ModConfigNullFix");

    @SpirePrefixPatch
    public static SpireReturn<Integer> Prefix(SpireConfig __instance, Object[] __args) {
        String key = (String) __args[0];
        String raw = __instance.getString(key);
        if (raw == null) {
            logger.warn("[ConfigNullFix] 配置项不存在: " + key + " ，按 0 处理");
            return SpireReturn.Return(0);
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            logger.warn("[ConfigNullFix] 配置项为空: " + key + " ，按 0 处理");
            return SpireReturn.Return(0);
        }
        try {
            return SpireReturn.Return(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            logger.warn("[ConfigNullFix] 配置项不是整数: " + key + " = " + raw + " ，按 0 处理");
            return SpireReturn.Return(0);
        }
    }
}
