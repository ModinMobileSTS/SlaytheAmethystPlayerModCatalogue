package frierenaudiofix;

import com.badlogic.gdx.audio.Music;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.audio.TempMusic;

/**
 * 修复 FrierenMod 特殊 BGM：
 * 死亡/放弃本局时 FrierenMod 会调用 playTempBgmInstantly("Frieren_DeathStinger")，
 * 一旦它自己的 key 映射没生效，原版 TempMusic.getSong 会兜底成
 * "audio/music/Frieren_DeathStinger"（无扩展名）并直接崩溃。
 *
 * 这里在 getSong 最前面接管以 "Frieren" 开头的自定义键，指向
 * FrierenModResources/audio/sound/*.mp3；找不到时返回 Continue，
 * 由后续（其它 Mod 注入 / 原版兜底）继续处理，最终由
 * MainMusicNewMusicPatch 保证不再抛异常。
 */
@SpirePatch2(clz = TempMusic.class, method = "getSong", paramtypez = {String.class})
public final class TempMusicGetSongPatch {

    private TempMusicGetSongPatch() {
    }

    @SpirePrefixPatch
    public static SpireReturn<Music> Prefix(Object[] __args) {
        String key = (__args != null && __args.length > 0 && __args[0] instanceof String)
                ? (String) __args[0]
                : null;

        Music music = MusicPathFix.resolveFrierenKey(key);
        if (music == null) {
            return SpireReturn.Continue();
        }
        return SpireReturn.Return(music);
    }
}
