package wizardrobe;

import basemod.BaseMod;
import basemod.helpers.RelicType;
import basemod.interfaces.EditRelicsSubscriber;
import basemod.interfaces.EditStringsSubscriber;
import basemod.interfaces.OnStartBattleSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import wizardrobe.relics.KnowledgeBase;
import wizardrobe.relics.PastLifeMemory;
import wizardrobe.relics.WizardRobe;

@SpireInitializer
public class WizardRobeMod implements EditStringsSubscriber, EditRelicsSubscriber, OnStartBattleSubscriber {

    public static final String MOD_ID = "wizardrobe";   // 模组内部代号，也是遗物 ID 的前缀

    public WizardRobeMod() {
        // 把自己登记为「订阅者」，BaseMod 才会回调下面两个 receive... 方法
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        // ModTheSpire 加载模组时调用的固定入口方法
        new WizardRobeMod();
    }

    public static String makeID(String id) {
        // 拼出带前缀的完整 ID，例如 makeID("WizardRobe") → "wizardrobe:WizardRobe"
        return MOD_ID + ":" + id;
    }

    @Override
    public void receiveEditStrings() {
        // 按当前游戏语言，决定读哪一份文本文件
        String lang;
        switch (Settings.language) {
            case ZHS:   // 简体中文
            case ZHT:   // 繁体中文暂时也用简体那一份
                lang = "zhs";
                break;
            default:    // 其它语言一律读英文
                lang = "eng";
                break;
        }
        // 路径是相对 jar 根目录写的，所以实际文件是 wizardrobe/localization/zhs/RelicStrings.json
        BaseMod.loadCustomStringsFile(RelicStrings.class, "wizardrobe/localization/" + lang + "/RelicStrings.json");
    }

    @Override
    public void receiveEditRelics() {
        // 保留共享注册，联机房主能在「初始遗物」里给任何角色配置。
        BaseMod.addRelic(new WizardRobe(), RelicType.SHARED);
        BaseMod.addRelic(new KnowledgeBase(), RelicType.SHARED);
        BaseMod.addRelic(new PastLifeMemory(), RelicType.SHARED);
    }

    @Override
    public void receiveOnBattleStart(AbstractRoom room) {
        if (AbstractDungeon.player != null) {
            AbstractRelic relic = AbstractDungeon.player.getRelic(KnowledgeBase.ID);
            if (relic instanceof KnowledgeBase) {
                ((KnowledgeBase) relic).beginBattle(room);
            }
        }
    }
}
