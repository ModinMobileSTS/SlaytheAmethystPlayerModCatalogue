package wizardrobe.relics;   // 包名：对应 jar 里的 wizardrobe/relics/ 目录

import basemod.abstracts.CustomRelic;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import gremlin.powers.WizPower;          // 崩坠里「法力」这个状态
import wizardrobe.WizardRobeMod;

/**
 * 巫师长袍（遗物本体）——遗物的效果全部在这个文件里。
 *
 * 效果：每回合开始时获得 3 层崩坠原生「法力」(Gremlin:Wiz)。
 * 它和地精法师用的是同一个状态，所以法师杖、巫师帽、法力词条都联动：
 * 攒到 3 层后，下一张攻击牌会消耗全部法力，额外造成 7 点伤害（带法师杖是 14 点）。
 */
public class WizardRobe extends CustomRelic {

    // 遗物唯一 ID："wizardrobe:WizardRobe"，必须和 RelicStrings.json 里的键名一致
    public static final String ID = WizardRobeMod.makeID("WizardRobe");

    /** 每回合给予的法力层数（想改数值，改这里的 3） */
    public static final int MANA_PER_TURN = 3;

    // 图标直接借用崩坠自带的图片（崩坠是必需依赖，所以能读到它的资源）
    private static final String IMG = "downfallResources/images/relics/CloakOfManyFaces.png";
    // 悬停/选中时显示的金色描边图
    private static final String OUTLINE = "downfallResources/images/relics/Outline/CloakOfManyFaces.png";

    public WizardRobe() {
        // 参数依次是：ID、图标、描边图、品阶（RARE=稀有）、拾取音效（MAGICAL=魔法音）
        super(ID, new Texture(IMG), new Texture(OUTLINE), RelicTier.RARE, LandingSound.MAGICAL);
    }

    @Override
    public String getUpdatedDescription() {
        // 返回文本文件里的描述（DESCRIPTIONS[0] = RelicStrings.json 里 DESCRIPTIONS 的第 1 条）
        return this.DESCRIPTIONS[0];
    }

    /**
     * 每回合开始时执行。游戏在玩家回合开始阶段会依次调用所有遗物的这个方法，
     * 第一回合也会调用。
     */
    @Override
    public void atTurnStart() {
        AbstractPlayer p = AbstractDungeon.player;   // 拿到当前玩家
        if (p == null) {
            return;                                  // 保护性判断，避免空指针崩溃
        }
        this.flash();                                // 遗物闪一下，给玩家视觉提示
        // 给玩家施加 N 层「法力」（N = MANA_PER_TURN），放进动作队列排队执行
        this.addToBot(new ApplyPowerAction(p, p, new WizPower(p, MANA_PER_TURN), MANA_PER_TURN));
    }

    @Override
    public boolean canSpawn() {
        return false;   // 不进随机遗物池：战斗奖励、商店、宝箱都不会刷出它
    }

    @Override
    public AbstractRelic makeCopy() {
        return new WizardRobe();   // 游戏需要复制遗物时（例如商店展示）调用
    }
}