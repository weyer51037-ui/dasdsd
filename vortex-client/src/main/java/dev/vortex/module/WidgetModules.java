package dev.vortex.module;

import dev.vortex.hud.ExtraWidgets;
import dev.vortex.hud.HudWidgets;

/** Registers the overlays and graphs from ExtraWidgets. */
final class WidgetModules {
    private WidgetModules() {}

    private static Module add(String id, String name, String desc, Category cat) {
        Module m = new Module(id, name, desc, cat);
        ModuleManager.add(m);
        return m;
    }

    static void register() {
        add("crosshair", "Custom Crosshair", "Replaces the vanilla crosshair with one you design.", Category.VISUAL)
                .addChoice("Style", "Cross", "Cross", "Cross + Dot", "Dot", "T-Shape", "X", "Circle", "Square")
                .addChoice("Color", "White", ExtraWidgets.COLORS)
                .addNum("Size", 5, 1, 20, 1)
                .addNum("Thickness", 1, 1, 4, 1)
                .addNum("Gap", 2, 0, 12, 1)
                .addNum("Opacity", 100, 20, 100, 5)
                .addBool("Outline", true)
                .addBool("Dynamic", false)
                .addBool("Hide Vanilla", true)
                .draw(ExtraWidgets::crosshair);

        add("hit_marker", "Hit Marker", "A quick flash around the crosshair when you attack.", Category.VISUAL)
                .addChoice("Color", "White", ExtraWidgets.COLORS)
                .addNum("Size", 5, 2, 12, 1)
                .addNum("Duration", 250, 100, 800, 50)
                .draw(ExtraWidgets::hitMarker);

        add("cooldown_bar", "Attack Cooldown Bar", "A small bar under the crosshair showing weapon charge.", Category.VISUAL)
                .addBool("Hide When Full", true)
                .draw(ExtraWidgets::cooldownBar);

        add("watermark", "Watermark", "A small Vortex badge, with optional FPS.", Category.HUD)
                .addChoice("Position", "Top Right", HudWidgets.POSITIONS)
                .addBool("Show FPS", true)
                .draw(ExtraWidgets::watermark);

        add("time_bar", "Day / Night Bar", "A bar showing where the sun is in the day cycle.", Category.HUD)
                .addChoice("Position", "Top Right", HudWidgets.POSITIONS)
                .draw(ExtraWidgets::timeBar);

        add("speed_bar", "Speed Bar", "A bar that fills with your movement speed.", Category.HUD)
                .addChoice("Position", "Bottom Left", HudWidgets.POSITIONS)
                .addNum("Max Speed", 20, 5, 60, 5)
                .draw(ExtraWidgets::speedBar);

        add("ping_graph", "Ping Graph", "A graph of your latency over the last minute.", Category.HUD)
                .addChoice("Position", "Bottom Left", HudWidgets.POSITIONS)
                .draw(ExtraWidgets::pingGraph);

        add("memory_graph", "Memory Graph", "A graph of Java memory use over the last minute.", Category.HUD)
                .addChoice("Position", "Bottom Left", HudWidgets.POSITIONS)
                .draw(ExtraWidgets::memoryGraph);

        add("effect_warning", "Effect Expiry Warning", "Flashes a warning when a potion effect is about to run out.", Category.HUD)
                .addNum("Seconds", 10, 3, 60, 1)
                .draw(ExtraWidgets::effectWarning);

        add("color_overlay", "Color Overlay", "A tint over the whole screen.", Category.VISUAL)
                .addChoice("Color", "Orange", ExtraWidgets.COLORS)
                .addNum("Opacity", 15, 5, 60, 5)
                .draw(ExtraWidgets::colorOverlay);

        add("rule_of_thirds", "Rule of Thirds Grid", "Composition guides for screenshots and videos.", Category.VISUAL)
                .addBool("Center Lines", false)
                .draw(ExtraWidgets::ruleOfThirds);

        add("letterbox", "Cinematic Bars", "Black bars at the top and bottom of the screen.", Category.VISUAL)
                .addNum("Size", 12, 4, 25, 1)
                .draw(ExtraWidgets::letterbox);

        add("screen_border", "Screen Border", "A coloured frame around the screen.", Category.VISUAL)
                .addChoice("Color", "Theme", ExtraWidgets.COLORS)
                .addNum("Thickness", 2, 1, 8, 1)
                .draw(ExtraWidgets::screenBorder);
    }
}
