package simplemapdownfall;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.map.DungeonMap;
import simplemap.patch.ShrinkMap;

/**
 * 崩坠与父模组都改写了 DungeonMap.update 里的 Hitbox.move 调用：
 * 崩坠把它挪到 offsetY + 60*scale + 256*scale（这套坐标是给未收缩的竖直地图准备的），
 * 父模组则挪到收缩网格的 Boss 位。谁先应用取决于加载顺序，
 * 于是点击框与图标对不上，表现就是"Boss 图标看得见却点不到"。
 *
 * 这里不跟两个 raw patch 抢那一行改写，而是在 bossHb.update() 之前
 * 直接把点击框钉回收缩布局的 Boss 位置。DungeonMap.update 的调用顺序是
 * move(...) 然后 bossHb.update()，所以在 update() 前缀里纠正是顺序无关且当帧生效的。
 */
public final class BossLayoutState {

    private BossLayoutState() {
    }

    /** 当前是否由本补丁接管 Boss 图标与点击框的定位。 */
    public static boolean active() {
        return DownfallMapAdapter.active();
    }

    static boolean isBossHitbox(Hitbox hb) {
        if (hb == null || AbstractDungeon.dungeonMapScreen == null) {
            return false;
        }
        DungeonMap map = AbstractDungeon.dungeonMapScreen.map;
        return map != null && hb == map.bossHb;
    }

    @SpirePatch2(clz = Hitbox.class, method = "update", paramtypez = {})
    public static final class KeepBossHitboxAligned {
        private KeepBossHitboxAligned() {
        }

        @SpirePrefixPatch
        public static void Prefix(Hitbox __instance) {
            if (!isBossHitbox(__instance)) {
                return;
            }
            if (!active()) {
                return;
            }
            __instance.move(ShrinkMap.BOSS_X, ShrinkMap.BOSS_Y);
        }
    }
}
