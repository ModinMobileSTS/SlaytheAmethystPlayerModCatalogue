package com.navsysfix;

import basemod.BaseMod;
import basemod.interfaces.StartGameSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;

/**
 * Entry point: loads the persisted settings, then lets the patches take over.
 */
@SpireInitializer
public class NavSysFixInit implements StartGameSubscriber {

    public NavSysFixInit() {
        Config.load();
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        new NavSysFixInit();
    }

    @Override
    public void receiveStartGame() {
        MarkMode.on = false;
        RoutePainter.invalidate();
    }
}
