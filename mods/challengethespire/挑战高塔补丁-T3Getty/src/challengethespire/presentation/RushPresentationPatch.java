package challengethespire.presentation;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.map.MapRoomNode;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Swaps the Boss Rush encounter rooms for boss-typed rooms when the challenge map is generated.
 *
 * <p>The parent mod builds its map with {@code MonsterRoomBossRush}, which does not extend
 * {@code MonsterRoomBoss}. The vanilla game decides boss music, background variations and
 * ambiance/sound cues with {@code instanceof MonsterRoomBoss}, so those never happened in a
 * Boss Rush fight. Replacing the room object keeps all encounter/reward logic intact while the
 * vanilla boss presentation works unchanged. Elite Rush already uses a MonsterRoomElite subclass
 * and needs no substitution.</p>
 */
public final class RushPresentationPatch {
    private static final String GENERATE_MAP_HOOK =
            "challengeTheSpire.patches.com.megacrit.cardcrawl.dungeons.AbstractDungeon.GenerateMapHook";
    private static final String BOSS_RUSH_ROOM = "challengeTheSpire.MonsterRoomBossRush";

    private static Field encounterIdField;

    private RushPresentationPatch() {
    }

    @SpirePatch2(cls = GENERATE_MAP_HOOK, method = "generateMonsterRooms",
            paramtypez = {List.class, Class.class, Random.class})
    public static class UseBossRoomType {
        @SpirePostfixPatch
        public static ArrayList after(ArrayList __result, Object[] __args) {
            Object roomClass = __args != null && __args.length > 1 ? __args[1] : null;
            if (__result == null || !(roomClass instanceof Class)
                    || !BOSS_RUSH_ROOM.equals(((Class<?>) roomClass).getName())) {
                return __result;
            }
            for (Object rowObj : __result) {
                if (!(rowObj instanceof List)) {
                    continue;
                }
                for (Object nodeObj : (List<?>) rowObj) {
                    if (!(nodeObj instanceof MapRoomNode)) {
                        continue;
                    }
                    MapRoomNode node = (MapRoomNode) nodeObj;
                    if (node.room == null || !BOSS_RUSH_ROOM.equals(node.room.getClass().getName())) {
                        continue;
                    }
                    String encounterID = readEncounterId(node.room);
                    if (encounterID != null) {
                        node.room = new RushBossRoom(encounterID);
                    }
                }
            }
            return __result;
        }
    }

    private static String readEncounterId(Object room) {
        try {
            if (encounterIdField == null) {
                encounterIdField = Class.forName(BOSS_RUSH_ROOM).getDeclaredField("encounterID");
                encounterIdField.setAccessible(true);
            }
            return (String) encounterIdField.get(room);
        } catch (Exception e) {
            return null;
        }
    }
}
