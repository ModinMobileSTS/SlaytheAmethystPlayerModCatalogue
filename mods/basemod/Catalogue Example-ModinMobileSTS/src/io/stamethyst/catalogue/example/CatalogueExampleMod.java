package io.stamethyst.catalogue.example;

import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;

/** A loadable, harmless catalogue fixture, not a gameplay fix. */
@SpireInitializer
public final class CatalogueExampleMod {
    private CatalogueExampleMod() {
    }

    public static void initialize() {
        System.out.println("[amethyst-catalogue-example] Example AI patch loaded; gameplay is unchanged.");
    }
}
