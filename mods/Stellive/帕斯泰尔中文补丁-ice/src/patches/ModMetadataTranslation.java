package patches;

import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;

final class ModMetadataTranslation {
    private ModMetadataTranslation() {
    }

    static void apply() {
        if (Loader.MODINFOS == null) return;
        for (ModInfo info : Loader.MODINFOS) {
            if (info == null || !"Stellive".equals(info.ID)) continue;
            if ("Slay the Cliche 제작팀 입니다.".equals(info.Description)) {
                info.Description = "我们是 Slay the Cliche 制作团队。";
            }
            if (info.Authors != null) {
                for (int i = 0; i < info.Authors.length; i++) {
                    if ("Slay the Cliche 제작팀".equals(info.Authors[i])) {
                        info.Authors[i] = "Slay the Cliche 制作团队";
                    }
                }
            }
            if (info.Credits != null) {
                String[] lines = info.Credits.split("\n", -1);
                for (int i = 0; i < lines.length; i++) {
                    String original = lines[i].trim();
                    String translated = creditLine(original);
                    if (!original.equals(translated)) {
                        int start = lines[i].indexOf(original);
                        lines[i] = lines[i].substring(0, start) + translated + lines[i].substring(start + original.length());
                    }
                }
                info.Credits = String.join("\n", lines);
            }
        }
    }

    private static String creditLine(String text) {
        switch (text) {
            case "PM": return "项目管理";
            case "개발": return "开发";
            case "기획": return "策划";
            case "일러스트": return "插画";
            case "Special Thanks": return "特别鸣谢";
            default: return contributor(text);
        }
    }

    static String contributor(String text) {
        switch (text) {
            case "미무스": return "Mimuseu";
            case "나코": return "Nako";
            case "사피조": return "Sapijo";
            case "녜잉어": return "Nyeingeo";
            case "휴로": return "Hyuro";
            case "설탕가루": return "糖粉";
            case "극악무도한 도넛":
            case "극악무도한도넛": return "凶恶甜甜圈";
            case "딴뚱": return "Ttanttung";
            case "박무지": return "Park Muji";
            case "산호초초": return "珊瑚礁礁";
            case "최쌩쥐": return "Choi Ssaengjwi";
            case "수환": return "Suhwan";
            case "미야옹옹": return "Miyaongong";
            case "꼬모": return "Kkomo";
            case "뭉개구름이": return "Munggaegureumi";
            default: return text;
        }
    }
}
