package dev.vortex.module;

import dev.vortex.VortexClient;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * One feature of the client. Build modules with the fluent methods and register them
 * with {@link ModuleManager#add(Module)}.
 */
public final class Module {
    public interface Tick { void tick(MinecraftClient mc, Module self); }

    public interface HudDraw { void draw(DrawContext ctx, MinecraftClient mc, int sw, int sh, Module self); }

    public final String id;
    public final String name;
    public final String description;
    public final Category category;
    public final List<Setting> settings = new ArrayList<>();

    /** GLFW key code, or -1 when unbound. */
    public int key = -1;

    private boolean enabled;
    private boolean toggleable = true;
    private boolean defaultEnabled;
    private Tick tickHook;
    private Runnable enableHook;
    private Runnable disableHook;
    private Runnable action;
    private Function<Module, String> valueFn;
    private String label = "";
    private HudDraw draw;
    private boolean failed;

    public Module(String id, String name, String description, Category category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
    }

    // ---- builder -------------------------------------------------------------------------

    public Module defaultEnabled(boolean v) { this.defaultEnabled = v; this.enabled = v; return this; }
    public Module permanent() { this.toggleable = false; this.enabled = true; this.defaultEnabled = true; return this; }
    public Module addBool(String n, boolean def) { settings.add(Setting.bool(n, def)); return this; }
    public Module addNum(String n, double def, double min, double max, double step) { settings.add(Setting.number(n, def, min, max, step)); return this; }
    public Module addChoice(String n, String def, String... choices) { settings.add(Setting.choice(n, def, choices)); return this; }
    public Module onTick(Tick t) { this.tickHook = t; return this; }
    public Module onEnable(Runnable r) { this.enableHook = r; return this; }
    public Module onDisable(Runnable r) { this.disableHook = r; return this; }
    public Module action(Runnable r) { this.action = r; this.toggleable = false; this.enabled = false; return this; }
    public Module label(String l) { this.label = l; return this; }
    public Module value(Function<Module, String> f) { this.valueFn = f; return this; }
    public Module draw(HudDraw d) { this.draw = d; return this; }

    // ---- state ---------------------------------------------------------------------------

    public boolean isEnabled() { return enabled && !failed; }
    public boolean isToggleable() { return toggleable; }
    public boolean isAction() { return action != null; }
    public boolean defaultsEnabled() { return defaultEnabled; }
    public String hudLabel() { return label; }
    public boolean hasValue() { return valueFn != null; }
    public boolean hasDraw() { return draw != null; }
    public boolean hasTick() { return tickHook != null; }

    /** Sets the state without running hooks. Used while loading the config. */
    public void setEnabledSilently(boolean v) {
        if (toggleable) enabled = v;
    }

    public void setEnabled(boolean v) {
        if (!toggleable || enabled == v) return;
        enabled = v;
        failed = false;
        ConfigDirty.mark();
        try {
            Runnable r = v ? enableHook : disableHook;
            if (r != null) r.run();
        } catch (Throwable t) {
            fail("toggle", t);
        }
    }

    public void toggle() { setEnabled(!enabled); }

    /** Runs the action for action modules, toggles everything else. */
    public void activate() {
        if (action != null) {
            try {
                action.run();
            } catch (Throwable t) {
                VortexClient.LOGGER.error("Module {} action failed", id, t);
            }
        } else {
            toggle();
        }
    }

    public void runEnableHook() {
        try {
            if (enableHook != null) enableHook.run();
        } catch (Throwable t) {
            fail("enable", t);
        }
    }

    public void runDisableHook() {
        try {
            if (disableHook != null) disableHook.run();
        } catch (Throwable t) {
            VortexClient.LOGGER.error("Module {} disable hook failed", id, t);
        }
    }

    public void tick(MinecraftClient mc) {
        if (tickHook == null || !isEnabled()) return;
        try {
            tickHook.tick(mc, this);
        } catch (Throwable t) {
            fail("tick", t);
        }
    }

    public void drawHud(DrawContext ctx, MinecraftClient mc, int sw, int sh) {
        if (draw == null || !isEnabled()) return;
        try {
            draw.draw(ctx, mc, sw, sh, this);
        } catch (Throwable t) {
            fail("draw", t);
        }
    }

    /** Current text shown in the HUD list, or null to hide the line. */
    public String currentValue() {
        if (valueFn == null || !isEnabled()) return null;
        try {
            return valueFn.apply(this);
        } catch (Throwable t) {
            fail("value", t);
            return null;
        }
    }

    private void fail(String where, Throwable t) {
        failed = true;
        VortexClient.LOGGER.error("Module {} crashed in {} and was switched off", id, where, t);
    }

    // ---- settings ------------------------------------------------------------------------

    public Setting setting(String n) {
        for (Setting s : settings) {
            if (s.name.equals(n)) return s;
        }
        throw new IllegalArgumentException("Module " + id + " has no setting '" + n + "'");
    }

    public boolean getBool(String n) { return setting(n).getBool(); }
    public double getNum(String n) { return setting(n).getNum(); }
    public int getInt(String n) { return setting(n).getInt(); }
    public String getChoice(String n) { return setting(n).getChoice(); }

    public void resetSettings() {
        for (Setting s : settings) s.reset();
    }
}
