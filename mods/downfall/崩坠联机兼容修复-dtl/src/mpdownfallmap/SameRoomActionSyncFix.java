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
 * 联机模组判断“队友是不是和我待在同一个房间”看两样东西同时成立：
 *   1) 地图格子：幕、坐标、幕内计数（NetworkLocation 的 act / x / y / infinityCounter）；
 *   2) 阶段编号 roomActionCounter：同一个格子可能有多个阶段，比如“事件中途变成战斗”。
 * 两个都成立，才会画出队友的人物模型；交易界面（PreTradingScreen）也才把队友列进名单。
 * 而队友头顶信息栏只看角色状态、不查编号，所以表现为“名字有、人没有、TRADE 名单空”。
 *
 * 崩坠的心脏商店（HeartShopRoom）每次调用 showHeartMerchant 都会让阶段编号 +1
 * （联机模组给它专门写的 ActionCountPatches$HeartShopRoomPatch）。
 * 但“负责生成这间房”的客户端和“只加载房间数据”的客户端调用次数不一样，
 * 于是两台机器的编号对不齐，而且对不齐的格数还不固定——这正是
 * “谁先到谁就能看见对方、后到的看不见先到的”这种单向现象的原因。
 *
 * 修正范围（严格限制，避免出现“不在同一个地方也能交易”）：
 *   - 只对齐“地图格子完全相同”的玩家；坐标 / 幕 / 幕内计数一律不改，
 *     所以不同格子上的玩家永远不会被当成“同房间”；
 *   - 只在商店、篝火这类没有战斗阶段的房间生效，本地处于战斗时也不生效，
 *     避免把“事件阶段 / 已进战斗”这种真实差异抹平；
 *   - 只改联机模组缓存的展示层快照，不动任何一方真实的对局数据，两台机器各自执行、效果对称。
 */
public final class SameRoomActionSyncFix {
    private SameRoomActionSyncFix() {
    }

    private static void trySync(AbstractRoom room) {
        if (room == null || room.phase == AbstractRoom.RoomPhase.COMBAT) {
            return;
        }
        AbstractRoom current = AbstractDungeon.getCurrRoom();
        if (current == null) {
            return;
        }
        if (!(current instanceof ShopRoom) && !(current instanceof RestRoom)) {
            return;
        }
        MpSupport.alignSameRoomAction();
    }

    /** 每帧画房间之前对齐一次，联机模组紧接着就会按对齐后的数据画队友。 */
    @SpirePatch2(clz = AbstractRoom.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class BeforeRender {
        @SpirePrefixPatch
        public static void Prefix(AbstractRoom __instance) {
            trySync(__instance);
        }
    }

    /** 更新时也对齐一次，保证队友进房那一刻、打开交易界面前数据已经正确。 */
    @SpirePatch2(clz = AbstractRoom.class, method = "update", paramtypez = {})
    public static class OnUpdate {
        @SpirePrefixPatch
        public static void Prefix(AbstractRoom __instance) {
            trySync(__instance);
        }
    }
}
