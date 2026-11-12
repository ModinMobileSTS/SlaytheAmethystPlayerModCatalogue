package shengzhufix.patches;

import basemod.BaseMod;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;
import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * 修复圣主受击时的卡死。
 *
 * 根因在 GuFu（古夫）mod 的 luShi.luShi.onAttackAssit()：
 * <pre>
 *   if (!(orb instanceof AbstractMinion)) continue;   // 下标 j 不推进，orb 也不变
 * </pre>
 * 该循环只有在充能球槽里是“清醒随从”或遇到空槽时才会退出。
 * 而圣主的面具系统同样占用充能球槽，面具既不是随从也不是空槽，
 * 于是玩家一受到伤害，主线程就永久卡在这个循环里（现场：连续 20 多秒栈完全相同）。
 *
 * 本补丁不改 GuFu、也不改圣主：在 BaseMod 派发“玩家受伤”钩子前后做一次临时处理——
 * 派发前按原循环的判定顺序推演，若发现这次派发一定会死循环（或下标越界），
 * 就把那个“毒”住循环的球暂时替换成空槽，派发结束后立即原样还原。
 * 推演结果正常时完全不动任何东西，正常随从配置下 GuFu 的挡伤逻辑一字不改。
 */
@SpirePatch2(clz = BaseMod.class, method = "publishOnPlayerDamaged", paramtypez = {int.class, DamageInfo.class})
public class GuFuAssistHangPatch {

    private static final String MINION_CLASS = "luShi.cards.Abstract.AbstractMinion";
    /** 推演结果：原方法会正常结束。 */
    private static final int OK = -1;
    /** 推演结果：下标会走到列表外（原方法抛越界）。 */
    private static final int OFF_END = -2;

    private static boolean resolved;
    private static Class<?> minionClass;
    private static Field sleepingField;

    /** 被临时改动的球列表，以及还原所需的信息。 */
    private static ArrayList<AbstractOrb> touchedList;
    private static int touchedIndex = -1;
    private static AbstractOrb savedOrb;
    private static boolean appendedSlot;

    @SpirePrefixPatch
    public static void Prefix() {
        try {
            restore();
            AbstractPlayer player = AbstractDungeon.player;
            if (player == null) {
                return;
            }
            ArrayList<AbstractOrb> orbs = player.orbs;
            if (orbs == null || orbs.isEmpty()) {
                return;
            }
            int blocking = blockingIndex(orbs);
            if (blocking >= 0) {
                touchedList = orbs;
                touchedIndex = blocking;
                savedOrb = orbs.get(blocking);
                orbs.set(blocking, new EmptyOrbSlot());
            } else if (blocking == OFF_END) {
                touchedList = orbs;
                appendedSlot = true;
                orbs.add(new EmptyOrbSlot());
            }
        } catch (Throwable ignored) {
            // 任何意外都不干预游戏
        }
    }

    @SpirePostfixPatch
    public static void Postfix() {
        try {
            restore();
        } catch (Throwable ignored) {
        }
    }

    private static void restore() {
        if (touchedList == null) {
            return;
        }
        try {
            if (appendedSlot) {
                int last = touchedList.size() - 1;
                if (last >= 0 && touchedList.get(last) instanceof EmptyOrbSlot) {
                    touchedList.remove(last);
                }
            } else if (touchedIndex >= 0 && touchedIndex < touchedList.size() && savedOrb != null) {
                touchedList.set(touchedIndex, savedOrb);
            }
        } catch (Throwable ignored) {
        } finally {
            touchedList = null;
            touchedIndex = -1;
            savedOrb = null;
            appendedSlot = false;
        }
    }

    /** 按 luShi.luShi.onAttackAssit() 的判定顺序推演：返回毒住循环的下标 / OFF_END / OK。 */
    private static int blockingIndex(ArrayList<AbstractOrb> orbs) throws Exception {
        resolve();
        if (minionClass == null || sleepingField == null) {
            return OK;
        }
        int size = orbs.size();
        int j = 0;
        // 下标每次最多前进一格，size + 1 步内必然得出结论
        for (int step = 0; step <= size; step++) {
            if (j < 0 || j >= size) {
                return OFF_END;
            }
            Object orb = orbs.get(j);
            if (orb == null) {
                return j;
            }
            if (orb instanceof EmptyOrbSlot) {
                return OK;
            }
            if (!minionClass.isInstance(orb)) {
                return j;
            }
            if (!sleepingField.getBoolean(orb)) {
                return OK;
            }
            j++;
        }
        return OFF_END;
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            minionClass = Class.forName(MINION_CLASS);
            sleepingField = minionClass.getDeclaredField("isSleeping");
            sleepingField.setAccessible(true);
        } catch (Throwable t) {
            minionClass = null;
            sleepingField = null;
        }
    }
}
