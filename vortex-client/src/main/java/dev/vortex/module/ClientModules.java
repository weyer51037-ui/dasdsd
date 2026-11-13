package dev.vortex.module;

import dev.vortex.hud.HudWidgets;
import dev.vortex.ui.Theme;

/** Always-on modules that hold global settings for the menu and the HUD. */
final class ClientModules {
    private ClientModules() {}

    static void register() {
        ModuleManager.add(new Module("hud_style", "HUD Style",
                "Layout and look of the HUD text list.", Category.CLIENT)
                .permanent()
                .addChoice("Position", "Top Left", HudWidgets.POSITIONS)
                .addNum("Scale", 1.0, 0.5, 2.0, 0.1)
                .addNum("Margin", 4, 0, 24, 1)
                .addBool("Background", true)
                .addBool("Shadow", true)
                .addBool("Accent Bar", true));

        ModuleManager.add(new Module("menu_style", "Menu Style",
                "Theme and background of the Vortex menu.", Category.CLIENT)
                .permanent()
                .addChoice("Theme", "Vortex", Theme.NAMES)
                .addChoice("Background", "Dim", "Dim", "Clear", "Vanilla")
                .addBool("Animations", true)
                .addBool("Title Screen Button", true)
                .addBool("Pause Screen Button", true));
    }
}
