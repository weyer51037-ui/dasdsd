package dev.vortex.feature;

import dev.vortex.mixin.SimpleOptionAccessor;
import dev.vortex.module.Module;
import dev.vortex.module.ModuleManager;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Hold-to-zoom with smooth easing, scroll adjustment and optional sensitivity scaling. */
public final class ZoomFeature {
    public static KeyBinding key;

    private static double current = 1.0;
    private static double target = 1.0;
    private static double scrollMul = 1.0;
    private static boolean active;
    private static long lastNs;
    private static double savedSens = -1;

    private ZoomFeature() {}

    public static void register() {
        key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vortex.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.vortex"));
    }

    public static boolean isActive() {
        return active;
    }

    public static void tick(MinecraftClient mc) {
        Module m = ModuleManager.get("zoom");
        boolean on = m != null && m.isEnabled();
        boolean want = on && mc.currentScreen == null && mc.player != null && key.isPressed();
        if (!on) {
            while (key.wasPressed()) { /* drain */ }
        }
        if (want && !active) scrollMul = 1.0;
        active = want;

        if (want) {
            double level = m.getNum("Zoom Level") * scrollMul;
            target = Math.max(1.05, Math.min(60.0, level));
        } else {
            target = 1.0;
        }

        boolean wantSens = want && m.getBool("Reduce Sensitivity");
        SimpleOption<Double> sens = mc.options.getMouseSensitivity();
        if (wantSens) {
            if (savedSens < 0) savedSens = sens.getValue();
            double scaled = savedSens / Math.max(1.0, Math.sqrt(current));
            ((SimpleOptionAccessor<Double>) (Object) sens).vortex$setValue(scaled);
        } else if (savedSens >= 0) {
            ((SimpleOptionAccessor<Double>) (Object) sens).vortex$setValue(savedSens);
            savedSens = -1;
        }
    }

    public static void restore() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null && savedSens >= 0) {
            ((SimpleOptionAccessor<Double>) (Object) mc.options.getMouseSensitivity()).vortex$setValue(savedSens);
            savedSens = -1;
        }
    }

    /** Called from the FOV mixin. */
    public static double applyFov(double fov) {
        long now = System.nanoTime();
        double dt = lastNs == 0 ? 0.016 : Math.min(0.1, (now - lastNs) / 1e9);
        lastNs = now;

        Module m = ModuleManager.get("zoom");
        boolean smooth = m != null && m.getBool("Smooth");
        if (smooth) {
            current += (target - current) * (1.0 - Math.exp(-14.0 * dt));
        } else {
            current = target;
        }
        if (current < 1.002) return fov;
        double rad = Math.toRadians(fov);
        return Math.toDegrees(2.0 * Math.atan(Math.tan(rad / 2.0) / current));
    }

    /** Returns true when the scroll was used for zooming and must not reach the hotbar. */
    public static boolean onScroll(double vertical) {
        if (!active) return false;
        Module m = ModuleManager.get("zoom");
        if (m == null || !m.getBool("Scroll Zoom")) return false;
        scrollMul *= vertical > 0 ? 1.15 : 1.0 / 1.15;
        scrollMul = Math.max(0.2, Math.min(20.0, scrollMul));
        return true;
    }
}
