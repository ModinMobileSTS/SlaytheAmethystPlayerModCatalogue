package simplemapdownfall;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.map.DungeonMap;
import simplemap.patch.ShrinkMap;

/**
 * 崩坠与父模组都用 @SpireRawPatch 改写 DungeonMap.renderBossIcon 里的 SpriteBatch.draw：
 * 崩坠按"未收缩"的坐标加 offsetY 与 60*scale，父模组则改画到收缩网格的 Boss 位。
 * 两者谁先应用取决于加载顺序，实测会留下画在屏幕外、或与点击框错开的 Boss 图标。
 *
 * 这里用一个前缀在崩坠反派模式下直接接管绘制，绕开被两边改写过的函数体，
 * 保证图标始终画在父模组收缩布局的 Boss 位置（右侧中部）。
 */
@SpirePatch2(
    cls = "com.megacrit.cardcrawl.map.DungeonMap",
    method = "renderBossIcon",
    paramtypez = { SpriteBatch.class }
)
public final class BossIconRenderPatch {
    private BossIconRenderPatch() {
    }

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(DungeonMap __instance, Object[] __args) {
        if (!BossLayoutState.active() || DungeonMap.boss == null || __args == null || __args.length == 0) {
            return SpireReturn.Continue();
        }
        if (!(__args[0] instanceof SpriteBatch)) {
            return SpireReturn.Continue();
        }
        SpriteBatch sb = (SpriteBatch) __args[0];
        Color nodeColor = (Color) ReflectionHacks.getPrivate(__instance, DungeonMap.class, "bossNodeColor");
        if (nodeColor == null) {
            return SpireReturn.Continue();
        }
        sb.setColor(new Color(1.0f, 1.0f, 1.0f, nodeColor.a));
        if (DungeonMap.bossOutline != null) {
            ShrinkMap.BossIconPosition.drawAltPosition(sb, DungeonMap.bossOutline);
        }
        sb.setColor(nodeColor);
        ShrinkMap.BossIconPosition.drawAltPosition(sb, DungeonMap.boss);
        if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MAP) {
            __instance.bossHb.render(sb);
            if (Settings.isControllerMode && __instance.bossHb.hovered) {
                __instance.renderReticle(sb, __instance.bossHb);
            }
        }
        return SpireReturn.Return(null);
    }
}
