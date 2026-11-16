package amethyst.translation.kokoro;

import basemod.BaseMod;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.CharacterStrings;
import com.megacrit.cardcrawl.localization.RunModStrings;
import com.megacrit.cardcrawl.localization.StanceStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 在父模组 The Noh Dancer (Kokoro) 加载完自己的 zhs 字符串之后，
 * 用补丁内置的中文覆盖 JSON 重新加载仍为英文的条目。
 * BaseMod.loadJsonStrings 使用 putAll 合并，因此只覆盖本补丁提供的键，
 * 其余父模组翻译保持不变。
 */
@SpirePatch2(
    cls = "Kokoro.KokoroMod",
    method = "receiveEditStrings",
    paramtypez = {}
)
public final class LoadZhsOverridesPatch {
    public static final Logger logger = LogManager.getLogger(LoadZhsOverridesPatch.class.getName());

    private static final String ZHS_PREFIX = "KokoroTranslationResources/localization/zhs/";

    private LoadZhsOverridesPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(Kokoro.KokoroMod __instance) {
        if (!Settings.GameLanguage.ZHS.equals(Settings.language)) {
            return;
        }
        logger.info("Applying Noh Dancer Chinese translation overrides");
        loadSafe(RunModStrings.class, "Kokoro-Mod-Strings.json");
        loadSafe(UIStrings.class, "Kokoro-UI-Strings.json");
        loadSafe(CharacterStrings.class, "Kokoro-Character-Strings.json");
        loadSafe(StanceStrings.class, "Kokoro-Stances-Strings.json");
    }

    private static void loadSafe(Class<?> stringType, String fileName) {
        String path = ZHS_PREFIX + fileName;
        try {
            if (!com.badlogic.gdx.Gdx.files.internal(path).exists()) {
                logger.warn("Missing translation override resource: " + path);
                return;
            }
            BaseMod.loadCustomStringsFile(stringType, path);
        } catch (Exception e) {
            logger.error("Failed to load translation override: " + path, e);
        }
    }
}
