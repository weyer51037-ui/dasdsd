package dev.vortex.module;

import dev.vortex.feature.OptionTweaks;
import dev.vortex.hud.FrameStats;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;

/** Modules that make the game run smoother. */
final class PerformanceModules {
    private static final OptionTweaks OPTIMIZER = new OptionTweaks();
    private static final OptionTweaks ADAPTIVE = new OptionTweaks();
    private static final OptionTweaks BACKGROUND = new OptionTweaks();

    private static int adaptiveTicks;
    private static int adaptiveDistance = -1;

    private PerformanceModules() {}

    private static Module add(String id, String name, String desc) {
        Module m = new Module(id, name, desc, Category.PERFORMANCE);
        ModuleManager.add(m);
        return m;
    }

    private static void applyOptimizer(MinecraftClient mc, Module m, boolean on) {
        GameOptions o = mc.options;
        OPTIMIZER.apply(o.getGraphicsMode(), GraphicsMode.FAST, on && m.getBool("Fast Graphics"));
        OPTIMIZER.apply(o.getCloudRenderMode(), CloudRenderMode.OFF, on && m.getBool("Clouds Off"));
        OPTIMIZER.apply(o.getParticles(), ParticlesMode.MINIMAL, on && m.getBool("Minimal Particles"));
        OPTIMIZER.apply(o.getBiomeBlendRadius(), 0, on && m.getBool("No Biome Blend"));
        OPTIMIZER.apply(o.getEntityShadows(), false, on && m.getBool("No Entity Shadows"));
        OPTIMIZER.apply(o.getEntityDistanceScaling(), m.getNum("Entity Distance"), on && m.getBool("Lower Entity Distance"));
        OPTIMIZER.apply(o.getViewDistance(), m.getInt("Render Distance"), on && m.getBool("Cap Render Distance"));
        OPTIMIZER.apply(o.getSimulationDistance(), m.getInt("Simulation Distance"), on && m.getBool("Cap Simulation Distance"));
    }

    static void register() {
        add("optimizer", "Optimizer",
                "Applies performance-friendly video settings and puts your own settings back when you turn it off.")
                .addBool("Fast Graphics", true)
                .addBool("Clouds Off", true)
                .addBool("Minimal Particles", false)
                .addBool("No Biome Blend", true)
                .addBool("No Entity Shadows", true)
                .addBool("Lower Entity Distance", true)
                .addNum("Entity Distance", 0.75, 0.5, 2.0, 0.25)
                .addBool("Cap Render Distance", false)
                .addNum("Render Distance", 12, 2, 32, 1)
                .addBool("Cap Simulation Distance", false)
                .addNum("Simulation Distance", 8, 5, 32, 1)
                .onTick((mc, m) -> {
                    if (mc.player.age % 20 == 0) applyOptimizer(mc, m, true);
                })
                .onEnable(() -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    Module m = ModuleManager.get("optimizer");
                    if (mc.options != null && m != null) applyOptimizer(mc, m, true);
                })
                .onDisable(() -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    Module m = ModuleManager.get("optimizer");
                    if (mc.options != null && m != null) applyOptimizer(mc, m, false);
                });

        add("entity_culling", "Entity Culling", "Skips drawing entities that are far away.")
                .addNum("Max Distance", 48, 8, 128, 4)
                .addBool("Keep Players", true);

        add("particle_limiter", "Particle Limiter", "Drops a share of the particles to save frames.")
                .addNum("Keep Percent", 40, 0, 100, 5);

        add("adaptive_distance", "Adaptive Render Distance",
                "Lowers render distance when your FPS drops below the target and raises it again when there is room.")
                .addNum("Target FPS", 60, 20, 240, 5)
                .addNum("Min Distance", 6, 2, 32, 1)
                .addNum("Max Distance", 16, 2, 32, 1)
                .onTick((mc, m) -> {
                    if (++adaptiveTicks < 80) return;
                    adaptiveTicks = 0;
                    int min = m.getInt("Min Distance");
                    int max = Math.max(min, m.getInt("Max Distance"));
                    if (adaptiveDistance < 0) adaptiveDistance = mc.options.getViewDistance().getValue();
                    int fps = FrameStats.fps();
                    int target = m.getInt("Target FPS");
                    if (fps > 0 && fps < target * 0.9 && adaptiveDistance > min) adaptiveDistance--;
                    else if (fps > target * 1.5 && adaptiveDistance < max) adaptiveDistance++;
                    adaptiveDistance = Math.max(min, Math.min(max, adaptiveDistance));
                    ADAPTIVE.apply(mc.options.getViewDistance(), adaptiveDistance, true);
                })
                .onDisable(() -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    adaptiveDistance = -1;
                    adaptiveTicks = 0;
                    if (mc.options != null) ADAPTIVE.apply(mc.options.getViewDistance(), 0, false);
                });

        add("background_fps", "Background FPS Limit", "Drops the frame rate while the game window is not in focus.")
                .addNum("Limit", 20, 10, 60, 5)
                .onTick((mc, m) -> BACKGROUND.apply(mc.options.getMaxFps(), m.getInt("Limit"), !mc.isWindowFocused()))
                .onDisable(() -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.options != null) BACKGROUND.apply(mc.options.getMaxFps(), 0, false);
                });
    }
}
