package patches;

import basemod.BaseMod;
import basemod.ReflectionHacks;
import code.cards.HanakoNana.Na_01_Love;
import com.megacrit.cardcrawl.cards.AbstractCard;
import java.lang.reflect.Method;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;

@SpirePatch2(cls = "code.cards.HanakoNana.Na_01_Love", method = "Na_Love_ImgLoad", paramtypez = {})
public final class LoveCardTitlePatch {
    @SpirePostfixPatch
    public static void Postfix(Na_01_Love __instance) {
        if (Settings.language != Settings.GameLanguage.ZHS || __instance.name == null) return;
        String title = __instance.name
                .replace("[마]지막[탄]환", "魔弹")
                .replace("공포탄", "空包弹")
                .replace("2발째!", "第2发！")
                .replace("3발째!", "第3发！")
                .replace("4발째!", "第4发！")
                .replace("5발째!", "第5发！");
        if (!title.equals(__instance.name)) {
            __instance.name = title;
            try {
                Method initialize = ReflectionHacks.getCachedMethod(AbstractCard.class, "initializeTitle");
                if (initialize != null) initialize.invoke(__instance);
            } catch (ReflectiveOperationException e) {
                BaseMod.logger.warn("Stellive Chinese: could not update Love card title layout", e);
            }
        }
    }
}
