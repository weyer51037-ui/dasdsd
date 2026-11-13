package dev.vortex.module;

import dev.vortex.feature.OptionTweaks;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.sound.SoundCategory;

/**
 * One module per vanilla option. While a module is on it holds that option at the chosen value;
 * turning it off (or closing the game) restores whatever you had set before.
 */
final class OptionModules {
    private OptionModules() {}

    private static <T> Module option(String id, String name, String desc,
                                     Function<GameOptions, SimpleOption<T>> getter, Function<Module, T> value) {
        OptionTweaks tweaks = new OptionTweaks();
        Module m = new Module(id, name, desc, Category.OPTIONS);
        m.onTick((mc, self) -> {
            if (mc.player.age % 10 == 0) tweaks.apply(getter.apply(mc.options), value.apply(self), true);
        });
        m.onEnable(() -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.options != null) tweaks.apply(getter.apply(mc.options), value.apply(m), true);
        });
        m.onDisable(() -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.options != null) tweaks.apply(getter.apply(mc.options), value.apply(m), false);
        });
        ModuleManager.add(m);
        return m;
    }

    private static Module percent(String id, String name, String desc,
                                  Function<GameOptions, SimpleOption<Double>> getter, double def) {
        return OptionModules.<Double>option(id, name, desc, getter, m -> m.getNum("Percent") / 100.0)
                .addNum("Percent", def, 0, 100, 5);
    }

    private static Module toggle(String id, String name, String desc,
                                 Function<GameOptions, SimpleOption<Boolean>> getter, boolean def) {
        return OptionModules.<Boolean>option(id, name, desc, getter, m -> m.getBool("Value"))
                .addBool("Value", def);
    }

    static void register() {
        // numbers
        OptionModules.<Integer>option("opt_fov", "FOV Override", "Holds your field of view at a fixed value.",
                o -> o.getFov(), m -> m.getInt("Value")).addNum("Value", 90, 30, 110, 1);
        OptionModules.<Integer>option("opt_max_fps", "FPS Cap Override", "Holds your frame rate cap. 260 means unlimited.",
                o -> o.getMaxFps(), m -> m.getInt("Value")).addNum("Value", 144, 10, 260, 10);
        OptionModules.<Integer>option("opt_view", "Render Distance Override", "Holds your render distance.",
                o -> o.getViewDistance(), m -> m.getInt("Value")).addNum("Value", 12, 2, 32, 1);
        OptionModules.<Integer>option("opt_sim", "Simulation Distance Override", "Holds your simulation distance.",
                o -> o.getSimulationDistance(), m -> m.getInt("Value")).addNum("Value", 8, 5, 32, 1);
        OptionModules.<Double>option("opt_sens", "Sensitivity Override", "Holds your mouse sensitivity. 100% is the vanilla default.",
                o -> o.getMouseSensitivity(), m -> m.getNum("Percent") / 200.0).addNum("Percent", 100, 0, 200, 5);
        OptionModules.<Double>option("opt_scroll", "Scroll Speed Override", "Holds your mouse wheel sensitivity.",
                o -> o.getMouseWheelSensitivity(), m -> m.getNum("Value")).addNum("Value", 1.0, 0.1, 5.0, 0.1);

        // chat look
        percent("opt_chat_opacity", "Chat Opacity", "How see-through the chat text is.", o -> o.getChatOpacity(), 100);
        percent("opt_chat_scale", "Chat Text Size", "Size of chat text.", o -> o.getChatScale(), 100);
        percent("opt_chat_spacing", "Chat Line Spacing", "Space between chat lines.", o -> o.getChatLineSpacing(), 0);
        percent("opt_chat_width", "Chat Width", "How wide the chat window is.", o -> o.getChatWidth(), 100);
        percent("opt_chat_height", "Chat Height (Open)", "Chat window height while typing.", o -> o.getChatHeightFocused(), 100);
        percent("opt_chat_height_idle", "Chat Height (Closed)", "Chat window height while not typing.", o -> o.getChatHeightUnfocused(), 44);
        percent("opt_chat_bg", "Text Background Opacity", "Darkness of the box behind chat text.", o -> o.getTextBackgroundOpacity(), 50);

        // switches
        toggle("opt_vsync", "VSync Override", "Forces VSync on or off.", o -> o.getEnableVsync(), false);
        toggle("opt_auto_jump", "Auto Jump Override", "Forces auto jump on or off.", o -> o.getAutoJump(), false);
        toggle("opt_subtitles", "Subtitles Override", "Forces subtitles on or off.", o -> o.getShowSubtitles(), true);
        toggle("opt_invert_y", "Invert Mouse Override", "Forces inverted Y on or off.", o -> o.getInvertYMouse(), false);
        toggle("opt_discrete_scroll", "Discrete Scrolling Override", "Forces discrete scrolling on or off.", o -> o.getDiscreteMouseScroll(), false);
        toggle("opt_chat_colors", "Chat Colors Override", "Forces chat colours on or off.", o -> o.getChatColors(), true);
        toggle("opt_chat_links", "Chat Links Override", "Forces clickable chat links on or off.", o -> o.getChatLinks(), true);

        // choices
        OptionModules.<AttackIndicator>option("opt_attack_indicator", "Attack Indicator Override", "Where the attack cooldown shows.",
                o -> o.getAttackIndicator(), m -> AttackIndicator.valueOf(m.getChoice("Value").toUpperCase(Locale.ROOT)))
                .addChoice("Value", "Crosshair", "Off", "Crosshair", "Hotbar");
        OptionModules.<CloudRenderMode>option("opt_clouds", "Clouds Override", "Holds the cloud setting.",
                o -> o.getCloudRenderMode(), m -> CloudRenderMode.valueOf(m.getChoice("Value").toUpperCase(Locale.ROOT)))
                .addChoice("Value", "Off", "Off", "Fast", "Fancy");
        OptionModules.<GraphicsMode>option("opt_graphics", "Graphics Override", "Holds the graphics mode.",
                o -> o.getGraphicsMode(), m -> GraphicsMode.valueOf(m.getChoice("Value").toUpperCase(Locale.ROOT)))
                .addChoice("Value", "Fast", "Fast", "Fancy", "Fabulous");
        OptionModules.<ParticlesMode>option("opt_particles", "Particles Override", "Holds the particle setting.",
                o -> o.getParticles(), m -> ParticlesMode.valueOf(m.getChoice("Value").toUpperCase(Locale.ROOT)))
                .addChoice("Value", "Decreased", "All", "Decreased", "Minimal");

        // sound mixer, one module per category
        for (SoundCategory c : SoundCategory.values()) {
            String label = HudModules.pretty(c.getName());
            percent("vol_" + c.getName(), label + " Volume", "Holds the " + label.toLowerCase(Locale.ROOT) + " volume.",
                    o -> o.getSoundVolumeOption(c), 50);
        }
    }
}
