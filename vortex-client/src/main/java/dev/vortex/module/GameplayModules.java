package dev.vortex.module;

import dev.vortex.hud.HudWidgets;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

/** Movement, quality of life and utility modules. */
final class GameplayModules {
    private static Vec3d lastAlivePos = Vec3d.ZERO;
    private static String lastAliveDim = "";
    private static String deathText;
    private static boolean wasDead;

    private GameplayModules() {}

    private static Module add(String id, String name, String desc) {
        Module m = new Module(id, name, desc, Category.GAMEPLAY);
        ModuleManager.add(m);
        return m;
    }

    static void register() {
        add("toggle_sprint", "Toggle Sprint", "Keeps you sprinting without holding the key.")
                .onTick((mc, m) -> {
                    if (mc.currentScreen == null) mc.options.sprintKey.setPressed(true);
                })
                .onDisable(KeyBinding::updatePressedStates);

        add("auto_sneak", "Auto Sneak", "Holds sneak for you.")
                .onTick((mc, m) -> {
                    if (mc.currentScreen == null) mc.options.sneakKey.setPressed(true);
                })
                .onDisable(KeyBinding::updatePressedStates);

        add("auto_walk", "Auto Walk", "Walks forward on its own. Handy for long trips.")
                .onTick((mc, m) -> {
                    if (mc.currentScreen == null) mc.options.forwardKey.setPressed(true);
                })
                .onDisable(KeyBinding::updatePressedStates);

        add("quick_perspective", "Quick Perspective", "Bind a key to cycle first person, back and front camera.")
                .action(() -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    Perspective p = mc.options.getPerspective();
                    mc.options.setPerspective(p.next());
                });

        add("copy_coords", "Copy Coordinates", "Bind a key to copy your XYZ to the clipboard.")
                .action(() -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.player == null) return;
                    String s = mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ();
                    mc.keyboard.setClipboard(s);
                    mc.player.sendMessage(Text.literal("Copied " + s), true);
                });

        add("clear_chat", "Clear Chat", "Bind a key to wipe the chat window.")
                .action(() -> MinecraftClient.getInstance().inGameHud.getChatHud().clear(false));

        Module death = new Module("death_coords", "Death Coordinates",
                "Tells you where you died and keeps it on the HUD.", Category.GAMEPLAY)
                .label("Died at")
                .addBool("Chat Message", true)
                .value(m -> deathText)
                .onTick((mc, m) -> {
                    boolean dead = mc.currentScreen instanceof DeathScreen;
                    if (!dead) {
                        lastAlivePos = mc.player.getPos();
                        lastAliveDim = mc.world.getRegistryKey().getValue().getPath();
                    } else if (!wasDead) {
                        deathText = Math.round(lastAlivePos.x) + ", " + Math.round(lastAlivePos.y) + ", "
                                + Math.round(lastAlivePos.z) + " (" + lastAliveDim.replace('_', ' ') + ")";
                        if (m.getBool("Chat Message")) {
                            mc.player.sendMessage(Text.literal("You died at " + deathText), false);
                        }
                    }
                    wasDead = dead;
                });
        ModuleManager.add(death);

        add("low_durability", "Low Durability Warning", "Warns you when your tool or armor is about to break.")
                .addNum("Percent", 10, 1, 50, 1)
                .draw(HudWidgets::lowDurability);
    }
}
