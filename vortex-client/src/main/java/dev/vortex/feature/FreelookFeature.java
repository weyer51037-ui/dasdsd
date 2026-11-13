package dev.vortex.feature;

import dev.vortex.module.Module;
import dev.vortex.module.ModuleManager;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/** Look around with the camera without turning your character. */
public final class FreelookFeature {
    public static KeyBinding key;

    private static boolean active;
    private static boolean toggled;
    private static float yaw;
    private static float pitch;
    private static Perspective previous = Perspective.FIRST_PERSON;
    private static boolean changedPerspective;

    private FreelookFeature() {}

    public static void register() {
        key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vortex.freelook", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "category.vortex"));
    }

    public static boolean isActive() { return active; }
    public static float yaw() { return yaw; }
    public static float pitch() { return pitch; }

    public static void tick(MinecraftClient mc) {
        Module m = ModuleManager.get("freelook");
        boolean pressedEvent = false;
        while (key.wasPressed()) pressedEvent = true;

        boolean enabled = m != null && m.isEnabled() && mc.player != null && mc.currentScreen == null;
        boolean want;
        if (!enabled) {
            toggled = false;
            want = false;
        } else if (m.getChoice("Mode").equals("Toggle")) {
            if (pressedEvent) toggled = !toggled;
            want = toggled;
        } else {
            want = key.isPressed();
        }

        if (want && !active) {
            yaw = mc.player.getYaw();
            pitch = mc.player.getPitch();
            previous = mc.options.getPerspective();
            changedPerspective = false;
            if (m.getBool("Third Person") && previous.isFirstPerson()) {
                mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
                changedPerspective = true;
            }
            active = true;
        } else if (!want && active) {
            active = false;
            if (changedPerspective) mc.options.setPerspective(previous);
            changedPerspective = false;
        }
    }

    /** Called from the mouse mixin. Returns true when the movement was used for the camera. */
    public static boolean handle(double dx, double dy) {
        if (!active) return false;
        yaw += (float) dx * 0.15f;
        pitch = MathHelper.clamp(pitch + (float) dy * 0.15f, -90.0f, 90.0f);
        return true;
    }
}
