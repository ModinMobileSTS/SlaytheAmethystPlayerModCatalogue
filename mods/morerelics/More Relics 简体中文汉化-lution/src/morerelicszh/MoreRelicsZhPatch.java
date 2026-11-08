package morerelicszh;

import basemod.BaseMod;
import basemod.interfaces.EditKeywordsSubscriber;
import basemod.interfaces.EditStringsSubscriber;
import basemod.interfaces.SetUnlocksSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.localization.UIStrings;

/**
 * 为 More Relics 提供简体中文文本。
 *
 * More Relics 自带 loadLocalization("eng") + loadLocalization(当前语言)，
 * 但它没有 localization/zhs 目录，因此在简体中文环境下会退回英文（并在日志里留下异常）。
 * 这里在本模组自己的 jar 里补充 zhs 文本，用同一个 "morerelics:" 前缀覆盖它的英文条目。
 *
 * 文本必须在 More Relics 构造遗物实例之前写入语言包（遗物的名字与描述在构造时被缓存），
 * 所以在 EditStrings、EditKeywords、SetUnlocks 三个钩子里各写一次：
 * 这三个钩子都早于 EditCards/EditRelics，无论模组之间的订阅顺序如何，最终生效的都是中文。
 */
@SpireInitializer
public class MoreRelicsZhPatch implements EditStringsSubscriber, EditKeywordsSubscriber, SetUnlocksSubscriber {

    /** 本模组自己的资源前缀，避免与其它模组的资源路径冲突。 */
    private static final String ZH_ROOT = "morerelics_zh/localization/zhs/";

    public static void initialize() {
        BaseMod.subscribe(new MoreRelicsZhPatch());
    }

    @Override
    public void receiveEditStrings() {
        loadChinese();
    }

    @Override
    public void receiveEditKeywords() {
        loadChinese();
    }

    @Override
    public void receiveSetUnlocks() {
        loadChinese();
    }

    private static void loadChinese() {
        if (Settings.language != Settings.GameLanguage.ZHS) {
            return;
        }
        BaseMod.loadCustomStringsFile(RelicStrings.class, ZH_ROOT + "RelicStrings.json");
        BaseMod.loadCustomStringsFile(PowerStrings.class, ZH_ROOT + "PowerStrings.json");
        BaseMod.loadCustomStringsFile(UIStrings.class, ZH_ROOT + "UIStrings.json");
    }
}
