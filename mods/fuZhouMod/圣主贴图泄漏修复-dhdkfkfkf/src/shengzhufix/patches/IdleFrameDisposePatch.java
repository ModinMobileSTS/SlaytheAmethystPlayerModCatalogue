package shengzhufix.patches;

import basemod.ReflectionHacks;
import basicmod.BasicMod;
import basicmod.character.ShengZhuCustomPlayer;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;

/*
 * 父 mod 的 ShengZhuCustomPlayer.disableIdleAnimation() 只做 this.idleFrames = null，
 * 从不 dispose 20 帧 idle 纹理（每帧 517x572 RGBA，约 1.2MB，合计约 23.7MB）。
 * 每次“关闭动画 -> 再开启动画”（例如鼠符咒被老爹封印后又被返还）都会重新 new 20 张纹理，
 * 旧的仍留在显存里，战斗中反复循环会持续吃内存，最终表现为卡死/被系统回收。
 *
 * 这里在方法体执行前先把待丢弃的帧纹理释放掉，随后原方法照常把 idleFrames 置空、
 * 把 img 换成静态石像贴图，行为与改动前完全一致，不涉及任何玩法数值。
 */
@SpirePatch2(clz = ShengZhuCustomPlayer.class, method = "disableIdleAnimation", paramtypez = {})
public class IdleFrameDisposePatch {

    @SpirePrefixPatch
    public static void Prefix(ShengZhuCustomPlayer __instance) {
        if (__instance == null) {
            return;
        }
        try {
            Texture[] frames = (Texture[]) ReflectionHacks.getPrivate(
                    __instance, ShengZhuCustomPlayer.class, "idleFrames");
            if (frames == null || frames.length == 0) {
                return;
            }
            int disposed = 0;
            for (int i = 0; i < frames.length; i++) {
                Texture frame = frames[i];
                if (frame == null) {
                    continue;
                }
                try {
                    frame.dispose();
                    disposed++;
                } catch (Throwable ignored) {
                    // 已经被释放或释放失败都不影响后续流程
                }
                frames[i] = null;
            }
            if (disposed > 0) {
                BasicMod.logger.info("【圣主贴图泄漏修复】已释放 " + disposed + " 帧旧 idle 纹理。");
            }
        } catch (Throwable t) {
            BasicMod.logger.warn("【圣主贴图泄漏修复】释放 idle 纹理时出现异常：" + t.getMessage());
        }
    }
}
