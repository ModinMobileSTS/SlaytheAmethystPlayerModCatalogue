package downfallmp;

import com.evacipated.cardcrawl.modthespire.lib.LineFinder;
import com.evacipated.cardcrawl.modthespire.lib.Matcher;
import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertLocator;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.map.MapRoomNode;
import javassist.CtBehavior;
import spireTogether.SpireTogetherMod;

@SpirePatch2(clz = MapRoomNode.class, method = "<ctor>", paramtypez = {int.class, int.class},
    requiredModId = "downfall", optional = true)
public final class MapBaseJitterCapture {
    private MapBaseJitterCapture() {}

    @SpirePatch2(clz = MapRoomNode.class, method = SpirePatch.CLASS,
        requiredModId = "downfall", optional = true)
    public static class Fields {
        public static SpireField<Float> baseJitterY = new SpireField<>(() -> null);
    }

    // 构造函数内已写入原版随机偏移，但崩坠和联机的构造后钩子都还没执行。
    @SpireInsertPatch(locator = BeforeHitbox.class)
    public static void capture(MapRoomNode __instance) {
        if (SpireTogetherMod.isConnected) {
            Fields.baseJitterY.set(__instance, __instance.offsetY);
        }
    }

    public static class BeforeHitbox extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctor) throws Exception {
            return LineFinder.findInOrder(ctor, new Matcher.NewExprMatcher(Hitbox.class));
        }
    }
}
