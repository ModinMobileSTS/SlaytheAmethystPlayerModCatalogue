package kimmocazhs;

/**
 * Runtime translation helpers for Slay the KimMoca.
 *
 * The parent mod registers its keywords under the prefix "kimmoca:" using
 * Korean names, and it appends a handful of hardcoded Korean fragments to
 * card descriptions, power descriptions and event options.  BaseMod renders a
 * keyword by looking up "kimmoca:&lt;name&gt;" in GameDictionary and then
 * stripping the prefix from the visible word, so the descriptions must use
 * Chinese names and matching Chinese keywords must be registered.
 */
public final class Zh {
    private Zh() {
    }

    /** Korean keyword name -&gt; Chinese keyword name, longest first. */
    private static final String[][] KEYWORD_NAMES = {
        {"혈당스파이크", "血糖峰值"},
        {"스트리머_마재_상점", "主播_马宰_商店"},
        {"스트리머_메탈킴_상점", "主播_金属金_商店"},
        {"스트리머_엔마_상점", "主播_恩玛_商店"},
        {"스트리머_연초봄_상점", "主播_延初春_商店"},
        {"스트리머_실피아_상점", "主播_希尔菲娅_商店"},
        {"스트리머_나옹이빵_상점", "主播_喵喵面包_商店"},
        {"스트리머_포베_상점", "主播_福贝_商店"},
        {"스트리머_무룩_상점", "主播_沉默_商店"},
        {"스트리머_나옹이빵", "主播_喵喵面包"},
        {"스트리머_메탈킴", "主播_金属金"},
        {"스트리머_연초봄", "主播_延初春"},
        {"스트리머_실피아", "主播_希尔菲娅"},
        {"스트리머_마재", "主播_马宰"},
        {"스트리머_엔마", "主播_恩玛"},
        {"스트리머_포베", "主播_福贝"},
        {"스트리머_무룩", "主播_沉默"},
        {"스트리머코인을", "主播币"},
        {"깜짝상자를", "惊喜盒"},
        {"조준상태", "瞄准状态"},
        {"공격대성공", "攻击大成功"},
        {"공격대실패", "攻击大失败"},
        {"수비대성공", "防御大成功"},
        {"수비대실패", "防御大失败"},
        {"운명점", "命运点"},
        {"증가량", "增长量"},
        {"카페인", "咖啡因"},
        {"치즈", "奶酪"},
        {"역류", "反流"},
        {"상급", "上级"},
        {"중급", "中级"},
        {"하급", "下级"},
        {"인성", "人性"},
        {"혈당", "血糖"},
        {"조준", "瞄准"},
        {"사거리", "射程"},
        {"주사위", "骰子"},
        {"나옹이빵", "喵喵面包"},
        {"레시피", "配方"},
        {"유통기한", "保质期"},
        {"암습", "伏击"},
        {"분신", "分身"},
        {"대본", "剧本"},
        {"덱", "卡组"},
        {"우클릭", "右键"},
        {"코스튬", "时装"},
        {"초코보", "陆行鸟"},
        {"막타", "终结击"}
    };

    /** Replace Korean keyword tokens in a localization JSON with Chinese ones. */
    public static String keywords(String text) {
        if (text == null) {
            return null;
        }
        String result = text;
        for (String[] pair : KEYWORD_NAMES) {
            result = result.replace(pair[0], pair[1]);
        }
        return result;
    }

    /** Translate hardcoded Korean fragments appended at runtime by the parent mod. */
    public static String fix(String text) {
        if (text == null) {
            return null;
        }
        String s = text;
        // Whole phrases first so that the single-word rules below cannot break them.
        s = s.replace("카드를 추가로 획득하거나 강화 할 수 있습니다.", "可以选择额外获得卡牌或强化卡牌。");
        s = s.replace("피해자 1명을 선택 해주세요.", "请选择 1 名受害者。");
        s = s.replace("열심히 만들었습니다.. 그것만 기대해주세요!", "用心制作.. 敬请期待！");
        s = s.replace("결승선 도착!", "到达终点！");
        s = s.replace("나옹이빵 데미지 : ", "喵喵面包伤害 : ");
        s = s.replace("체력 : #r", "生命 : #r");
        s = s.replace("만큼 후원합니다.", "赞助。");
        s = s.replace("카드 추가 획득", "额外获得卡牌");
        s = s.replace("수치 없음", "无数值");
        s = s.replace("장 추가 구매 가능", "张追加购买");
        s = s.replace("장 구매 가능", "张可购买");
        s = s.replace("중독 부여", "施加中毒");
        s = s.replace("의 피해", " 点伤害");
        s = s.replace("음성 전체 끄기", "关闭全部音效");
        s = s.replace("음성 일부만 끄기", "关闭部分音效");
        s = s.replace("Slay the KimMoca", "金莫卡");
        s = s.replace("미무스", "米穆斯");
        // Single words.
        s = s.replace("소멸", "消耗");
        s = s.replace("회복", "回复");
        s = s.replace("암습", "伏击");
        s = s.replace("부여", "施加");
        s = s.replace("감소", "减少");
        s = s.replace("골드", "金币");
        s = s.replace("획득", "获得");
        s = s.replace("이상", "以上");
        s = s.replace("미만", "以下");
        s = s.replace("구매", "购买");
        s = s.replace("가능", "可用");
        s = s.replace("장", "张");
        return s;
    }
}
