package com.navsysfix;

import basemod.BaseMod;
import basemod.interfaces.StartGameSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;

/**
 * Entry point. The fixes themselves are ModTheSpire patches; this only resets
 * the per-run mark-mode state.
 */
@SpireInitializer
public class NavSysFixInit implements StartGameSubscriber {

    public NavSysFixInit() {
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        new NavSysFixInit();
    }

    @Override
    public void receiveStartGame() {
        MarkMode.on = false;
    }
}
