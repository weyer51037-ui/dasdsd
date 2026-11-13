package dev.vortex.module;

import dev.vortex.config.ConfigManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();
    private static final Map<String, Module> BY_ID = new HashMap<>();
    private static final Map<Integer, Boolean> KEY_STATE = new HashMap<>();
    private static boolean started;
    private static long lastSave;

    private ModuleManager() {}

    public static void add(Module m) {
        if (BY_ID.put(m.id, m) != null) throw new IllegalStateException("Duplicate module id " + m.id);
        MODULES.add(m);
    }

    public static List<Module> all() { return Collections.unmodifiableList(MODULES); }

    public static Module get(String id) { return BY_ID.get(id); }

    public static boolean isEnabled(String id) {
        Module m = BY_ID.get(id);
        return m != null && m.isEnabled();
    }

    public static void tick(MinecraftClient mc) {
        if (!started) {
            started = true;
            for (Module m : MODULES) {
                if (m.isEnabled() && m.isToggleable()) m.runEnableHook();
            }
        }

        if (mc.currentScreen == null) {
            long window = mc.getWindow().getHandle();
            for (Module m : MODULES) {
                if (m.key < 0) continue;
                boolean down = InputUtil.isKeyPressed(window, m.key);
                boolean was = KEY_STATE.getOrDefault(m.key, false);
                KEY_STATE.put(m.key, down);
                if (down && !was) m.activate();
            }
        }

        if (mc.player != null && mc.world != null) {
            for (Module m : MODULES) m.tick(mc);
        }

        long now = System.currentTimeMillis();
        if (now - lastSave > 5000) {
            lastSave = now;
            if (ConfigDirty.consume()) ConfigManager.save();
        }
    }

    /** Called when the game closes so options we touched are put back before they get saved. */
    public static void shutdown() {
        for (Module m : MODULES) {
            if (m.isEnabled() && m.isToggleable()) m.runDisableHook();
        }
        dev.vortex.feature.OptionTweaks.restoreAll();
        VisualModules.restoreAll();
        dev.vortex.feature.ZoomFeature.restore();
    }
}
