package shengzhufix.patches;

import basemod.ReflectionHacks;
import basicmod.BasicMod;
import basicmod.character.ShengZhuCustomPlayer;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;

/*
 * 父 mod 的 ShengZhuCustomPlayer.loadIdleFrames() 只在 idleFrames 为 null 或长度不为 20 时才重新加载，
 * 完全不检查缓存里的帧是否还有效。一旦数组里出现 null（某帧加载失败），
 * 后续战斗会一直复用这个坏数组。
 *
 * 这里在执行原方法前做一次有效性检查：只要发现 null 帧，先把剩下有效帧 dispose 掉，再把 idleFrames 置空，
 * 让原方法走正常的重新加载分支（既不留下坏数组，也不泄漏剩下的帧）。
 * 数组完整有效时不做任何事，不增加额外解码开销。
 */
@SpirePatch2(clz = ShengZhuCustomPlayer.class, method = "loadIdleFrames", paramtypez = {})
public class IdleFrameReloadPatch {

    @SpirePrefixPatch
    public static void Prefix(ShengZhuCustomPlayer __instance) {
        if (__instance == null) {
            return;
        }
        try {
            Texture[] frames = (Texture[]) ReflectionHacks.getPrivate(
                    __instance, ShengZhuCustomPlayer.class, "idleFrames");
            if (frames == null || frames.length != 20) {
                return;
            }
            boolean hasInvalid = false;
            for (int i = 0; i < frames.length; i++) {
                if (frames[i] == null) {
                    hasInvalid = true;
                    break;
                }
            }
            if (!hasInvalid) {
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
                    // 已被释放或释放失败都不影响后续流程
                }
                frames[i] = null;
            }
            ReflectionHacks.setPrivate(__instance, ShengZhuCustomPlayer.class, "idleFrames", null);
            BasicMod.logger.info("【圣主贴图泄漏修复】检测到无效 idle 帧缓存，已释放 " + disposed
                    + " 帧残留纹理，下次开启动画时重新加载。");
        } catch (Throwable t) {
            BasicMod.logger.warn("【圣主贴图泄漏修复】检查 idle 帧缓存时出现异常：" + t.getMessage());
        }
    }
}
