package patches;

import code.cards.AbstractEasyCard;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;

@SpirePatch2(cls = "code.cards.AbstractEasyCard", method = "getTopText", paramtypez = {})
public final class CardTagTranslationPatch {
    @SpirePostfixPatch
    public static String Postfix(String __result, AbstractEasyCard __instance) {
        if (Settings.language != Settings.GameLanguage.ZHS || __result == null) return __result;
        switch (__result) {
            case "신성": return "神圣";
            case "권능": return "权能";
            case "히키코모리": return "家里蹲";
            case "권법": return "拳法";
            case "사랑이": return "爱爱";
            case "오타쿠": return "宅力";
            case "긁": return "刮伤";
            case "용사": return "勇者";
            case "용사 / 매도": return "勇者 / 辱骂";
            case "뇌청초": return "青脑草";
            case "먹보": return "吃货";
            case "매도": return "辱骂";
            case "소울푸드": return "灵魂美食";
            case "메이드": return "女仆";
            case "별명": return "昵称";
            case "효과음": return "音效";
            case "봉누도": return "Bongnudo";
            default: return __result;
        }
    }
}
