package heartfx;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.powers.AbstractPower;

/**
 * 「我夺取心脏」演出的公共常量与判定。
 */
public final class HeartFx {

    /** 突进时的破空音效（蕾忍mod 已注册的 audio/Flash.mp3）。 */
    public static final String SFX_DASH = "Flash";

    public static final String POWER_LEXKELA = "LexKela";
    public static final String POWER_AMOXILIN = "AmoXilin";
    public static final String POWER_SCARE = "ScarePower";
    public static final String POWER_DIMDEADTREE = "DimDeadTreePower";
    public static final String POWER_ARTIFACT = "Artifact";

    /** 我夺取心脏 的忍术消耗。 */
    public static final int NINJUTSU_COST = 10;

    /** 前冲距离占「蕾忍与目标之间距离」的比例。 */
    public static final float DASH_RATIO = 0.40f;

    private HeartFx() {
    }

    /**
     * 与蕾忍mod 的 actions.NinjutsuAction 判定保持一致。
     * 只有忍术真的会发动（也就是目标真的会被斩杀）时，才播放演出，避免空放动画。
     */
    public static boolean willTrigger(AbstractPlayer p, int cost) {
        if (p == null) {
            return false;
        }
        AbstractPower scare = p.getPower(POWER_SCARE);
        if (scare != null && scare.amount > 0) {
            return true;
        }
        if (p.getPower(POWER_DIMDEADTREE) != null) {
            return true;
        }
        AbstractPower amoxilin = p.getPower(POWER_AMOXILIN);
        if (amoxilin != null) {
            cost -= amoxilin.amount;
            if (cost < 0) {
                cost = 0;
            }
        }
        if (cost == 0) {
            return true;
        }
        AbstractPower lex = p.getPower(POWER_LEXKELA);
        return lex != null && lex.amount >= cost;
    }

    /**
     * 前冲距离：约为蕾忍与目标之间水平距离的 40%，过近/过远时夹在安全范围内。
     */
    public static float dashDistance(AbstractPlayer p, AbstractCreature target) {
        float min = 90.0f * Settings.scale;
        float max = 430.0f * Settings.scale;
        float gap = (target != null) ? (target.drawX - p.drawX) : 0.0f;
        float dist = DASH_RATIO * gap;
        if (dist < min) {
            dist = min;
        }
        if (dist > max) {
            dist = max;
        }
        return dist;
    }
}
