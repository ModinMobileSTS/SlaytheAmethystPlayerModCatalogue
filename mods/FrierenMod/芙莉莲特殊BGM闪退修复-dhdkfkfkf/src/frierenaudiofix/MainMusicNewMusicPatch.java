package frierenaudiofix;

import com.badlogic.gdx.audio.Music;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.audio.MainMusic;

/**
 * 最后一道拦截。
 *
 * 原版 TempMusic.getSong 与 MainMusic.getSong 的所有分支最终都调用
 * MainMusic.newMusic(path)，而 libGDX 的 OpenALAudio.newMusic 只按扩展名挑解码器，
 * 遇到没有扩展名的路径就抛 GdxRuntimeException（"Unknown file extension for music: ..."）。
 *
 * 这里只处理"扩展名无法识别"的路径（正常音乐一律放行、不改行为）：
 * 先尝试补全 .ogg/.mp3/.wav，仍不可用就替换成原版一定存在的 ogg，
 * 因此任何 Mod 传入的坏音乐路径都不会再让游戏闪退。
 */
@SpirePatch2(clz = MainMusic.class, method = "newMusic", paramtypez = {String.class})
public final class MainMusicNewMusicPatch {

    private MainMusicNewMusicPatch() {
    }

    @SpirePrefixPatch
    public static SpireReturn<Music> Prefix(Object[] __args) {
        String path = (__args != null && __args.length > 0 && __args[0] instanceof String)
                ? (String) __args[0]
                : null;

        if (MusicPathFix.hasKnownExtension(path)) {
            // 正常路径，保持原版行为
            return SpireReturn.Continue();
        }
        Music repaired = MusicPathFix.repairPath(path);
        if (repaired == null) {
            // 连原版兜底音乐都建不起来（例如音频设备未就绪），交回原版处理
            return SpireReturn.Continue();
        }
        return SpireReturn.Return(repaired);
    }
}
