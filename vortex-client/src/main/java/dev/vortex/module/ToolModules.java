package dev.vortex.module;

import static dev.vortex.module.HudModules.fmt;
import static dev.vortex.module.HudModules.line;
import static dev.vortex.module.HudModules.mc;

import dev.vortex.feature.OptionTweaks;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/** Timers, waypoints, snap-look, quick commands and screen actions. */
final class ToolModules {
    private static final OptionTweaks MUTE = new OptionTweaks();

    // stopwatch
    private static long swStart;
    private static long swElapsed;
    private static boolean swRunning;

    // countdown
    private static long cdEnd;

    // break reminder
    private static long lastBreak = System.currentTimeMillis();

    // waypoint
    private static boolean hasWaypoint;
    private static double wx, wy, wz;
    private static String wDim = "";

    private ToolModules() {}

    private static Module add(String id, String name, String desc) {
        Module m = new Module(id, name, desc, Category.TOOLS);
        ModuleManager.add(m);
        return m;
    }

    private static void say(String text, boolean overlay) {
        ClientPlayerEntity p = mc().player;
        if (p != null) p.sendMessage(Text.literal(text), overlay);
    }

    private static String stamp(long ms) {
        long s = ms / 1000;
        return String.format(Locale.ROOT, "%d:%02d.%d", s / 60, s % 60, (ms % 1000) / 100);
    }

    static void register() {
        registerTimers();
        registerWaypoint();
        registerSnapLook();
        registerQuickCommands();
        registerScreenActions();
        registerMisc();
    }

    // ---- timers --------------------------------------------------------------------------

    private static void registerTimers() {
        line("stopwatch", "Stopwatch", "Shows the stopwatch on the HUD. Control it with the start/stop and reset keys.", "Stopwatch", false,
                m -> stamp(swElapsed + (swRunning ? System.currentTimeMillis() - swStart : 0)));

        add("stopwatch_toggle", "Stopwatch Start / Stop", "Bind a key to start or pause the stopwatch.").action(() -> {
            if (swRunning) {
                swElapsed += System.currentTimeMillis() - swStart;
                swRunning = false;
            } else {
                swStart = System.currentTimeMillis();
                swRunning = true;
            }
        });

        add("stopwatch_reset", "Stopwatch Reset", "Bind a key to zero the stopwatch.").action(() -> {
            swElapsed = 0;
            swRunning = false;
        });

        line("countdown", "Countdown Timer", "A countdown that tells you in chat when it ends.", "Timer", false, m -> {
            if (cdEnd == 0) return null;
            return stamp(Math.max(0, cdEnd - System.currentTimeMillis()));
        }).addNum("Minutes", 5, 1, 60, 1)
                .onTick((mc, self) -> {
                    if (cdEnd != 0 && System.currentTimeMillis() >= cdEnd) {
                        cdEnd = 0;
                        say("Countdown finished!", false);
                    }
                });

        add("countdown_start", "Countdown Start", "Bind a key to start the countdown timer.").action(() -> {
            Module cd = ModuleManager.get("countdown");
            cdEnd = System.currentTimeMillis() + (long) (cd.getNum("Minutes") * 60_000);
            say("Countdown started: " + cd.getInt("Minutes") + " min", true);
        });

        add("countdown_cancel", "Countdown Cancel", "Bind a key to stop the countdown timer.").action(() -> cdEnd = 0);

        add("break_reminder", "Break Reminder", "Reminds you in chat to take a break.")
                .addNum("Minutes", 60, 10, 180, 5)
                .onEnable(() -> lastBreak = System.currentTimeMillis())
                .onTick((mc, m) -> {
                    if (System.currentTimeMillis() - lastBreak >= (long) (m.getNum("Minutes") * 60_000)) {
                        lastBreak = System.currentTimeMillis();
                        say("You've been playing for " + m.getInt("Minutes") + " minutes. Time for a break!", false);
                    }
                });
    }

    // ---- waypoint ------------------------------------------------------------------------

    private static void registerWaypoint() {
        add("waypoint_set", "Set Waypoint", "Bind a key to save your current position as the waypoint.").action(() -> {
            ClientPlayerEntity p = mc().player;
            if (p == null) return;
            wx = p.getX();
            wy = p.getY();
            wz = p.getZ();
            wDim = mc().world.getRegistryKey().getValue().toString();
            hasWaypoint = true;
            say("Waypoint set at " + p.getBlockX() + " " + p.getBlockY() + " " + p.getBlockZ(), true);
        });

        add("waypoint_clear", "Clear Waypoint", "Bind a key to remove the waypoint.").action(() -> hasWaypoint = false);

        line("waypoint", "Waypoint", "Distance and direction to your saved waypoint.", "Waypoint", false, m -> {
            if (!hasWaypoint) return null;
            ClientPlayerEntity p = mc().player;
            if (!mc().world.getRegistryKey().getValue().toString().equals(wDim)) return "other dimension";
            double dx = wx - p.getX(), dz = wz - p.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            float want = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float rel = MathHelper.wrapDegrees(want - p.getYaw());
            String[] arrows = {"\u2191", "\u2197", "\u2192", "\u2198", "\u2193", "\u2199", "\u2190", "\u2196"};
            int i = Math.floorMod(Math.round(rel / 45f), 8);
            return arrows[i] + " " + fmt(dist, 0) + " blocks";
        });
    }

    // ---- snap look -----------------------------------------------------------------------

    private static void look(float yaw) {
        ClientPlayerEntity p = mc().player;
        if (p != null) p.setYaw(yaw);
    }

    private static void registerSnapLook() {
        add("face_north", "Face North", "Bind a key to turn to face north.").action(() -> look(180f));
        add("face_south", "Face South", "Bind a key to turn to face south.").action(() -> look(0f));
        add("face_east", "Face East", "Bind a key to turn to face east.").action(() -> look(-90f));
        add("face_west", "Face West", "Bind a key to turn to face west.").action(() -> look(90f));

        add("snap_yaw", "Snap to Nearest Direction", "Bind a key to round your view to the nearest compass direction.")
                .action(() -> {
                    ClientPlayerEntity p = mc().player;
                    if (p != null) p.setYaw(Math.round(p.getYaw() / 90f) * 90f);
                });

        add("turn_around", "Turn Around", "Bind a key to turn 180 degrees.").action(() -> {
            ClientPlayerEntity p = mc().player;
            if (p != null) p.setYaw(p.getYaw() + 180f);
        });

        add("look_level", "Level Pitch", "Bind a key to look straight ahead.").action(() -> {
            ClientPlayerEntity p = mc().player;
            if (p != null) p.setPitch(0f);
        });

        add("look_up", "Look Up", "Bind a key to look straight up.").action(() -> {
            ClientPlayerEntity p = mc().player;
            if (p != null) p.setPitch(-90f);
        });

        add("look_down", "Look Down", "Bind a key to look straight down.").action(() -> {
            ClientPlayerEntity p = mc().player;
            if (p != null) p.setPitch(90f);
        });
    }

    // ---- quick commands and chat ---------------------------------------------------------

    private static void registerQuickCommands() {
        String[] commands = {"home", "spawn", "sethome", "back", "tpa", "tpaccept", "tpdeny", "warp", "kit", "sit", "rules", "help"};
        String[] chats = {"gg", "gl hf", "thanks", "brb", "back", "afk", "wp", "ty", "hello", "o/"};
        for (int i = 1; i <= 4; i++) {
            Module cmd = add("quick_command_" + i, "Quick Command " + i, "Bind a key to run a server command.")
                    .addChoice("Command", commands[i - 1], commands);
            cmd.action(() -> {
                var nh = mc().getNetworkHandler();
                if (nh != null) nh.sendChatCommand(cmd.getChoice("Command"));
            });

            Module chat = add("quick_chat_" + i, "Quick Chat " + i, "Bind a key to send a short chat message.")
                    .addChoice("Message", chats[i - 1], chats);
            chat.action(() -> {
                var nh = mc().getNetworkHandler();
                if (nh != null) nh.sendChatMessage(chat.getChoice("Message"));
            });
        }
    }

    // ---- screen and clipboard actions ----------------------------------------------------

    private static void registerScreenActions() {
        add("toggle_hud", "Toggle HUD", "Bind a key to hide or show the whole HUD (like F1).")
                .action(() -> mc().options.hudHidden = !mc().options.hudHidden);

        add("toggle_fullscreen", "Toggle Fullscreen", "Bind a key to switch fullscreen on or off.")
                .action(() -> mc().getWindow().toggleFullscreen());

        add("toggle_debug", "Toggle Debug Screen", "Bind a key to open or close the F3 screen.")
                .action(() -> mc().options.debugEnabled = !mc().options.debugEnabled);

        add("reload_chunks", "Reload Chunks", "Bind a key to redraw the world (like F3 + A).")
                .action(() -> {
                    MinecraftClient mc = mc();
                    if (mc.worldRenderer != null) mc.worldRenderer.reload();
                });

        add("reload_resources", "Reload Resource Packs", "Bind a key to reload resource packs (like F3 + T).")
                .action(() -> mc().reloadResources());

        add("copy_rotation", "Copy Rotation", "Bind a key to copy your yaw and pitch.").action(() -> {
            ClientPlayerEntity p = mc().player;
            if (p == null) return;
            String s = fmt(MathHelper.wrapDegrees(p.getYaw()), 1) + " " + fmt(p.getPitch(), 1);
            mc().keyboard.setClipboard(s);
            say("Copied " + s, true);
        });

        add("copy_nether_coords", "Copy Nether Coordinates", "Bind a key to copy your position converted to the other dimension.")
                .action(() -> {
                    ClientPlayerEntity p = mc().player;
                    if (p == null) return;
                    boolean nether = mc().world.getRegistryKey().getValue().getPath().equals("the_nether");
                    double f = nether ? 8.0 : 1.0 / 8.0;
                    String s = Math.round(p.getX() * f) + " " + p.getBlockY() + " " + Math.round(p.getZ() * f);
                    mc().keyboard.setClipboard(s);
                    say("Copied " + s + (nether ? " (overworld)" : " (nether)"), true);
                });

        add("copy_held_item", "Copy Held Item Name", "Bind a key to copy the name of the item in your hand.")
                .action(() -> {
                    ClientPlayerEntity p = mc().player;
                    if (p == null || p.getMainHandStack().isEmpty()) return;
                    String s = p.getMainHandStack().getName().getString();
                    mc().keyboard.setClipboard(s);
                    say("Copied " + s, true);
                });
    }

    private static void registerMisc() {
        add("mute_unfocused", "Mute When Unfocused", "Silences the game while its window is in the background.")
                .onTick((mc, m) -> MUTE.apply(mc.options.getSoundVolumeOption(SoundCategory.MASTER), 0.0, !mc.isWindowFocused()))
                .onDisable(() -> {
                    MinecraftClient mc = mc();
                    if (mc.options != null) MUTE.apply(mc.options.getSoundVolumeOption(SoundCategory.MASTER), 0.0, false);
                });
    }
}
