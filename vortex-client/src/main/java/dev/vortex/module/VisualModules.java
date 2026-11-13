package dev.vortex.module;

import dev.vortex.feature.OptionTweaks;
import dev.vortex.feature.ZoomFeature;
import dev.vortex.hud.HudWidgets;
import dev.vortex.mixin.SimpleOptionAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

/** Camera, lighting and overlay modules. */
final class VisualModules {
    private static final OptionTweaks TWEAKS = new OptionTweaks();
    private static double savedGamma = -1;

    private VisualModules() {}

    private static Module add(String id, String name, String desc) {
        Module m = new Module(id, name, desc, Category.VISUAL);
        ModuleManager.add(m);
        return m;
    }

    @SuppressWarnings("unchecked")
    private static void setGamma(MinecraftClient mc, double v) {
        if (mc.options == null) return;
        SimpleOption<Double> g = mc.options.getGamma();
        if (savedGamma < 0) savedGamma = g.getValue();
        if (g.getValue() != v) ((SimpleOptionAccessor<Double>) (Object) g).vortex$setValue(v);
    }

    @SuppressWarnings("unchecked")
    private static void restoreGamma() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null || savedGamma < 0) return;
        ((SimpleOptionAccessor<Double>) (Object) mc.options.getGamma()).vortex$setValue(savedGamma);
        savedGamma = -1;
    }

    static void register() {
        add("zoom", "Zoom", "Hold the zoom key (C by default) to zoom in. Scroll while zooming to adjust.")
                .addNum("Zoom Level", 4.0, 1.5, 30.0, 0.5)
                .addBool("Smooth", true)
                .addBool("Scroll Zoom", true)
                .addBool("Reduce Sensitivity", true)
                .onDisable(ZoomFeature::restore);

        add("freelook", "Freelook", "Hold the freelook key (Left Alt) to look around without turning your character.")
                .addChoice("Mode", "Hold", "Hold", "Toggle")
                .addBool("Third Person", true);

        add("fullbright", "Fullbright", "Maximum brightness everywhere, including caves.")
                .onEnable(() -> setGamma(MinecraftClient.getInstance(), 16.0))
                .onTick((mc, m) -> setGamma(mc, 16.0))
                .onDisable(VisualModules::restoreGamma);

        add("no_hurt_cam", "No Hurt Camera", "Stops the screen from shaking when you take damage.");
        add("no_fire", "No Fire Overlay", "Hides the fire on your screen while burning.");
        add("no_water_overlay", "No Water Overlay", "Hides the blue tint when your head is underwater.");
        add("no_vignette", "No Vignette", "Removes the dark edges around the screen.");

        add("no_menu_blur", "No Menu Blur", "Turns off the blurred background behind menus and inventories.")
                .defaultEnabled(true);

        add("no_fov_effects", "No FOV Effects", "Keeps your field of view steady while sprinting or with speed effects.")
                .onTick((mc, m) -> TWEAKS.apply(mc.options.getFovEffectScale(), 0.0, true))
                .onDisable(() -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.options != null) TWEAKS.apply(mc.options.getFovEffectScale(), 0.0, false);
                });

        add("no_distortion", "No Distortion", "Removes the nausea and portal wobble effects.")
                .onTick((mc, m) -> TWEAKS.apply(mc.options.getDistortionEffectScale(), 0.0, true))
                .onDisable(() -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.options != null) TWEAKS.apply(mc.options.getDistortionEffectScale(), 0.0, false);
                });

        add("no_bobbing", "No View Bobbing", "Turns off the camera sway while walking.")
                .onTick((mc, m) -> TWEAKS.apply(mc.options.getBobView(), false, true))
                .onDisable(() -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.options != null) TWEAKS.apply(mc.options.getBobView(), false, false);
                });

        add("hitboxes", "Hitboxes", "Shows entity hitboxes (same as F3 + B).")
                .onTick((mc, m) -> mc.getEntityRenderDispatcher().setRenderHitboxes(true))
                .onDisable(() -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.getEntityRenderDispatcher() != null) mc.getEntityRenderDispatcher().setRenderHitboxes(false);
                });

        add("cinematic_camera", "Cinematic Camera", "Smooth, slow camera movement for recording.")
                .onTick((mc, m) -> mc.options.smoothCameraEnabled = true)
                .onDisable(() -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.options != null) mc.options.smoothCameraEnabled = false;
                });

        add("low_health_warning", "Low Health Warning", "Pulses red around the screen edges when your health is low.")
                .addNum("Hearts", 4, 1, 10, 1)
                .addNum("Intensity", 1.0, 0.2, 2.0, 0.1)
                .draw(HudWidgets::lowHealth);
    }

    /** Called when the game closes. */
    static void restoreAll() {
        TWEAKS.restore();
        restoreGamma();
    }
}
