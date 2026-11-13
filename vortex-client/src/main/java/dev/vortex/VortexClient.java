package dev.vortex;

import dev.vortex.config.ConfigManager;
import dev.vortex.feature.CombatStats;
import dev.vortex.feature.FreelookFeature;
import dev.vortex.feature.ZoomFeature;
import dev.vortex.hud.HudRenderer;
import dev.vortex.module.Module;
import dev.vortex.module.ModuleManager;
import dev.vortex.module.Modules;
import dev.vortex.ui.ModMenuScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VortexClient implements ClientModInitializer {
    public static final String MOD_ID = "vortex";
    public static final Logger LOGGER = LoggerFactory.getLogger("Vortex");

    public static KeyBinding menuKey;

    @Override
    public void onInitializeClient() {
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vortex.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.vortex"));
        ZoomFeature.register();
        FreelookFeature.register();

        Modules.registerAll();
        ConfigManager.load();

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (menuKey.wasPressed()) {
                if (mc.currentScreen == null) mc.setScreen(new ModMenuScreen(null));
            }
            ZoomFeature.tick(mc);
            FreelookFeature.tick(mc);
            CombatStats.tick(mc);
            ModuleManager.tick(mc);
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> HudRenderer.render(ctx));

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient) CombatStats.onAttack(entity);
            return ActionResult.PASS;
        });

        // "Vortex" button on the title screen and the pause menu
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            Module style = ModuleManager.get("menu_style");
            boolean title = screen instanceof TitleScreen && style.getBool("Title Screen Button");
            boolean pause = screen instanceof GameMenuScreen && style.getBool("Pause Screen Button");
            if (!title && !pause) return;
            Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("Vortex"),
                            b -> client.setScreen(new ModMenuScreen(screen)))
                    .dimensions(6, 6, 64, 20)
                    .build());
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> {
            ModuleManager.shutdown();
            ConfigManager.save();
        });

        LOGGER.info("Vortex Client loaded with {} modules", ModuleManager.all().size());
    }
}
