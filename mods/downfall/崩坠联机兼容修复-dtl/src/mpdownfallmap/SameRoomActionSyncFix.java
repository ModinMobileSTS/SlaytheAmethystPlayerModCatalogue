package mpdownfallmap;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;

import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.RestRoom;
import com.megacrit.cardcrawl.rooms.ShopRoom;

/**
 * 修复联机时“明明在同一个房间，却看不到队友、也没法交易”的问题（崩坠的心脏商店最明显）。
 *
 * 联机模组判断“队友是不是和我待在同一个房间”看两组数据：
 *   1) 地图位置：幕、坐标、幕内计数（NetworkLocation 的 act / x / y / infinityCounter）；
 *   2) 房间阶段编号 roomActionCounter：同一个房间可能有多个阶段，比如“事件转战斗”。
 * 两个条件都成立，才会画出队友的人物模型；交易界面（PreTradingScreen）也才把队友列进名单。
 *
 * 崩坠的心脏商店（HeartShopRoom）是特殊房间：崩坠给它单独写了阶段编号处理，
 * 但那段处理只在“负责生成这间房”的客户端真正跑过，另一台只是加载房间数据，
 * 于是两边编号差 1：队友的信息栏还在（不查编号），人物却不画（查编号），
 * TRADE 名单也空了（查编号）—— 表现就是“看不到他、也没法交易”。
 *
 * 修正范围（严格限制，避免出现“不在一个地方也能交易”）：
 *   - 只对齐“地图位置完全相同”的玩家，坐标 / 幕 / 幕内计数一律不改，
 *     所以不同地图格子上的人永远不会变成“同房间”；
 *   - 只对齐相差 1 的阶段编号（真实原因就是差 1 格），差更多的直接跳过；
 *   - 只在商店、篝火这类没有战斗阶段的房间生效，且本地处于战斗时不生效，
 *     避免把“事件阶段 / 已进战斗”这种真实的阶段差异抹平。
 * 改的是联机模组缓存的展示层快照，不动任何一方真实的对局数据；两台机器各自执行，效果对称。
 */
@SpirePatch2(clz = AbstractRoom.class, method = "render", paramtypez = {SpriteBatch.class})
public final class SameRoomActionSyncFix {
    private SameRoomActionSyncFix() {
    }

    @SpirePrefixPatch
    public static void Prefix(AbstractRoom __instance) {
        if (__instance == null) {
            return;
        }
        if (__instance.phase == AbstractRoom.RoomPhase.COMBAT) {
            return;
        }
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        if (room == null) {
            return;
        }
        // 只处理没有“事件转战斗”这类阶段变化的房间，阶段编号才有唯一含义。
        if (!(room instanceof ShopRoom) && !(room instanceof RestRoom)) {
            return;
        }
        MpSupport.alignSameRoomAction();
    }
}