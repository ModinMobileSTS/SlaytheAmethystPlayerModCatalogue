package patches;

import com.megacrit.cardcrawl.core.Settings;

public final class EventTextTranslation {
    private EventTextTranslation() {
    }

    public static String translate(String text) {
        if (Settings.language != Settings.GameLanguage.ZHS || text == null) return text;
        if (text.equals("강화할 카드를 선택하십시오")) return "选择要升级的牌。";
        if (text.equals("강화 가능한 카드가 없습니다.")) return "没有可升级的牌。";
        if (text.equals("소멸할 카드를 선택하세요.")) return "选择要消化的牌。";
        return text
                .replace("#p텐코 #p시부키", "#p天狐 #p希布基")
                .replace("#r하나코 #r나나", "#r花子 #r娜娜")
                .replace("#g유즈하 #g리코", "#g柚叶 #g莉可")
                .replace("#b아오쿠모 #b린", "#b青云 #b凛")
                .replace("게스트", "嘉宾")
                .replace("Tenko Shibuki", "天狐希布基")
                .replace("Hanako Nana", "花子娜娜")
                .replace("Yuzuha Riko", "柚叶莉可")
                .replace("Aokumo Rin", "青云凛")
                .replace("Guest", "嘉宾");
    }

    public static String quizOption(String text) {
        if (Settings.language != Settings.GameLanguage.ZHS || text == null) return text;
        String[] options = code.Event.Event_Quiz.OPTIONS;
        if (options != null && options.length > 11) {
            if (text.equals(options[9] + "공격 카드")) return options[9] + "攻击牌";
            if (text.equals(options[10] + "스킬 카드")) return options[10] + "技能牌";
            if (text.equals(options[11] + "파워 카드")) return options[11] + "能力牌";
        }
        return text;
    }
}
