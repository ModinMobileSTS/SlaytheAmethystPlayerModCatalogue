package tkfix;

import basemod.BaseMod;
import basemod.interfaces.PostInitializeSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;

/**
 * 补丁入口：游戏加载完所有 mod 之后跑一次自检。
 */
@SpireInitializer
public class TkFixInit implements PostInitializeSubscriber {

    public static void initialize() {
        BaseMod.subscribe(new TkFixInit());
    }

    @Override
    public void receivePostInitialize() {
        SelfTest.run();
    }
}
