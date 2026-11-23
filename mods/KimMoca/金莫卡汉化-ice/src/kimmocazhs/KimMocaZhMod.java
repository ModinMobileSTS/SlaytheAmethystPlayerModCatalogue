package kimmocazhs;

import basemod.BaseMod;
import basemod.interfaces.EditKeywordsSubscriber;
import basemod.interfaces.EditStringsSubscriber;
import com.evacipated.cardcrawl.mod.stslib.Keyword;
import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.badlogic.gdx.Gdx;
import com.google.gson.Gson;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.localization.CharacterStrings;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.localization.RelicStrings;

import java.nio.charset.StandardCharsets;

/**
 * Loads Simplified Chinese localization for Slay the KimMoca.
 *
 * The parent mod loads its "eng/" files unconditionally; because these keys
 * are identical, loading our files afterwards overrides every string.  The
 * keyword JSON is registered separately because keywords are not part of the
 * LocalizedStrings maps.
 */
@SpireInitializer
public class KimMocaZhMod implements EditStringsSubscriber, EditKeywordsSubscriber {
    private static final String ROOT = "KimMocaZhResources/localization/zhs/";
    private static final String UTF8 = String.valueOf(StandardCharsets.UTF_8);

    private static final String CREDITS_KO = "NL NL 판을 키워버린 PM,개발 : #y미무스 NL NL 만렙 그래픽 : #y무존재감희삼 NL NL 작렬히 산화한 그래픽 : #y오른장인김오른 NL NL 테스터로 왔다가 사운드 깎고 그림도 그린  : #y빌런 NL NL 불꽃 피드백 테스터 : #y클버 NL NL (v0.9 합류) 테스터인가 QA 인가 : #y알터 NL NL (v0.9 합류) 슬더모 고인물이 되버린 : #yleef13 NL NL #bSpecial #bthanks NL NL 백호 메이드 : #y나방2호기입니다. NL NL 고장난 치즈 : #y르제흐프 ";
    private static final String CREDITS_ZH = "NL NL 把场子搞大的 PM、开发：#y米穆斯 NL NL 满级美术：#y无存在感希三 NL NL 燃烧到极致的美术：#y右手匠人金右恩 NL NL 以测试员身份加入，却连音效剪辑和绘图都包办的：#y反派 NL NL 火花反馈测试员：#y克勒伯 NL NL (v0.9 加入) 说是测试还是 QA：#y阿尔特 NL NL (v0.9 加入) 成了杀戮尖塔老玩家的：#yleef13 NL NL #b特别 #b感谢 NL NL 白虎女仆：#y我是飞蛾2号机。 NL NL 坏掉的奶酪：#y尔杰赫普 ";
    private static final String DESCRIPTION_KO = "열심히 만들었습니다.. 그것만 기억해주세요! NL NL credits에 다올리지 못했지만 카페에서 덧글로 도움 주신 분들 정말 감사합니다~!";
    private static final String DESCRIPTION_ZH = "用心制作.. 请只记住这一点！ NL NL 虽然没能全部写进鸣谢名单，但仍要衷心感谢在咖啡馆留言提供帮助的各位~!";

    public static void initialize() {
        translateMetadata();
        BaseMod.subscribe(new KimMocaZhMod());
    }

    /** Translate the mod-list metadata that ModTheSpire reads from the parent JAR manifest. */
    private static void translateMetadata() {
        if (Loader.MODINFOS == null) {
            return;
        }
        for (ModInfo info : Loader.MODINFOS) {
            if (info == null || !"KimMoca".equals(info.ID)) {
                continue;
            }
            if ("Slay the KimMoca".equals(info.Name)) {
                info.Name = "金莫卡";
            }
            if (info.Authors != null) {
                for (int i = 0; i < info.Authors.length; ++i) {
                    if ("미무스 NL".equals(info.Authors[i])) {
                        info.Authors[i] = "米穆斯 NL";
                    }
                }
            }
            if (CREDITS_KO.equals(info.Credits)) {
                info.Credits = CREDITS_ZH;
            }
            if (DESCRIPTION_KO.equals(info.Description)) {
                info.Description = DESCRIPTION_ZH;
            }
        }
    }

    private static String read(String file) {
        return Gdx.files.internal(ROOT + file).readString(UTF8);
    }

    @Override
    public void receiveEditStrings() {
        translateMetadata();
        BaseMod.loadCustomStrings(CardStrings.class, Zh.keywords(read("KimMocaMod-Card-Strings.json")));
        BaseMod.loadCustomStrings(RelicStrings.class, Zh.keywords(read("KimMocaMod-Relic-Strings.json")));
        BaseMod.loadCustomStrings(PowerStrings.class, Zh.keywords(read("KimMocaMod-Power-Strings.json")));
        BaseMod.loadCustomStrings(EventStrings.class, Zh.keywords(read("KimMocaMod-Event-Strings.json")));
        BaseMod.loadCustomStrings(PotionStrings.class, Zh.keywords(read("KimMocaMod-Potion-Strings.json")));
        BaseMod.loadCustomStrings(CharacterStrings.class, Zh.keywords(read("KimMocaMod-Character-Strings.json")));
        BaseMod.loadCustomStrings(OrbStrings.class, Zh.keywords(read("KimMocaMod-Orb-Strings.json")));
    }

    @Override
    public void receiveEditKeywords() {
        Keyword[] keywords = new Gson().fromJson(read("KimMocaMod-Keyword-Strings.json"), Keyword[].class);
        if (keywords == null) {
            return;
        }
        for (Keyword keyword : keywords) {
            if (keyword == null || keyword.NAMES == null || keyword.NAMES.length == 0) {
                continue;
            }
            BaseMod.addKeyword("kimmoca", keyword.PROPER_NAME, keyword.NAMES, keyword.DESCRIPTION);
        }
    }
}
