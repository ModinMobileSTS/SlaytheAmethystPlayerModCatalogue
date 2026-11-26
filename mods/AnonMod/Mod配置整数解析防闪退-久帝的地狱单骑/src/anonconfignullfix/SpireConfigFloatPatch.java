package anonconfignullfix;

import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 同 SpireConfigIntPatch：getFloat(key) 也是直接 Float.parseFloat(getString(key))，
 * key 缺失时同样会抛 NumberFormatException: null（只是消息变成 "null" 的浮点版本）。
 * 一并做成回退 0f，避免同类闪退。
 */
@SpirePatch2(clz = SpireConfig.class, method = "getFloat", paramtypez = {String.class})
public class SpireConfigFloatPatch {

    private static final Logger logger = LogManager.getLogger("ModConfigNullFix");

    @SpirePrefixPatch
    public static SpireReturn<Float> Prefix(SpireConfig __instance, Object[] __args) {
        String key = (String) __args[0];
        String raw = __instance.getString(key);
        if (raw == null) {
            logger.warn("[ConfigNullFix] 浮点配置项不存在: " + key + " ，按 0 处理");
            return SpireReturn.Return(0.0f);
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            logger.warn("[ConfigNullFix] 浮点配置项为空: " + key + " ，按 0 处理");
            return SpireReturn.Return(0.0f);
        }
        try {
            return SpireReturn.Return(Float.parseFloat(value));
        } catch (NumberFormatException e) {
            logger.warn("[ConfigNullFix] 配置项不是浮点数: " + key + " = " + raw + " ，按 0 处理");
            return SpireReturn.Return(0.0f);
        }
    }
}
