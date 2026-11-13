package dev.vortex.module;

/** Registers every module. Order here is the order in the menu and in the HUD list. */
public final class Modules {
    private static boolean done;

    private Modules() {}

    public static void registerAll() {
        if (done) return;
        done = true;
        ClientModules.register();
        HudModules.register();
        InfoModules.register();
        VisualModules.register();
        WidgetModules.register();
        GameplayModules.register();
        ToolModules.register();
        OptionModules.register();
        PerformanceModules.register();
    }
}
