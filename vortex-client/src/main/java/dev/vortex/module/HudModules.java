package dev.vortex.module;

import dev.vortex.feature.CombatStats;
import dev.vortex.hud.ClickTracker;
import dev.vortex.hud.FrameStats;
import dev.vortex.hud.HudWidgets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;

/** Everything that shows information on screen. */
final class HudModules {
    private static final long SESSION_START = System.currentTimeMillis();
    private static final String[] MOON = {
        "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent",
        "New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous"};

    private HudModules() {}

    static MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }

    /** Registers a one-line HUD module. */
    static Module line(String id, String name, String desc, String label, boolean def,
                               Function<Module, String> fn) {
        Module m = new Module(id, name, desc, Category.HUD).label(label).value(fn).defaultEnabled(def);
        ModuleManager.add(m);
        return m;
    }

    static Module counter(String id, String name, String label, Item... items) {
        return line(id, name, "Shows how many " + label.toLowerCase(Locale.ROOT) + " are in your inventory.",
                label, false, m -> {
                    int c = 0;
                    for (Item i : items) c += mc().player.getInventory().count(i);
                    if (c == 0 && m.getBool("Hide When Zero")) return null;
                    return String.valueOf(c);
                }).addBool("Hide When Zero", true);
    }

    static String fmt(double v, int decimals) {
        return String.format(Locale.ROOT, "%." + decimals + "f", v);
    }

    static String pretty(String s) {
        StringBuilder sb = new StringBuilder();
        for (String part : s.split("[_ ]")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    static String clock(long seconds) {
        long h = seconds / 3600, mi = (seconds % 3600) / 60, s = seconds % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, mi, s) : String.format("%d:%02d", mi, s);
    }

    static void register() {
        // ---- performance numbers --------------------------------------------------------
        line("fps", "FPS", "Frames per second.", "FPS", true, m -> {
            int f = FrameStats.fps();
            return m.getBool("Show 1% Low") ? f + " (low " + FrameStats.low() + ")" : String.valueOf(f);
        }).addBool("Show 1% Low", false);

        line("frame_time", "Frame Time", "How long the last frame took.", "Frame", false,
                m -> fmt(FrameStats.frameMs(), 1) + " ms");

        ModuleManager.add(new Module("frame_graph", "Frame Graph",
                "A small graph of recent frame times. Spikes show stutters.", Category.HUD)
                .addChoice("Position", "Bottom Left", HudWidgets.POSITIONS)
                .draw(HudWidgets::frameGraph));

        line("memory", "Memory", "Java memory in use.", "RAM", false, m -> {
            Runtime r = Runtime.getRuntime();
            long used = (r.totalMemory() - r.freeMemory()) >> 20;
            return used + " / " + (r.maxMemory() >> 20) + " MB";
        });

        line("entities", "Entity Count", "Entities currently loaded around you.", "Entities", false, m -> {
            int n = 0;
            for (Entity ignored : mc().world.getEntities()) n++;
            return String.valueOf(n);
        });

        line("ping", "Ping", "Your latency to the server.", "Ping", false, m -> {
            ClientPlayNetworkHandler nh = mc().getNetworkHandler();
            if (nh == null || mc().isInSingleplayer()) return null;
            PlayerListEntry e = nh.getPlayerListEntry(mc().player.getUuid());
            return e == null ? null : e.getLatency() + " ms";
        });

        line("server", "Server Address", "The server you are connected to.", "Server", false, m -> {
            if (mc().isInSingleplayer()) return "Singleplayer";
            return mc().getCurrentServerEntry() == null ? null : mc().getCurrentServerEntry().address;
        });

        // ---- input ----------------------------------------------------------------------
        line("cps", "CPS", "Clicks per second.", "CPS", true,
                m -> ClickTracker.left() + " | " + ClickTracker.right());

        ModuleManager.add(new Module("keystrokes", "Keystrokes",
                "WASD, space and mouse buttons with a live CPS counter.", Category.HUD)
                .addChoice("Position", "Bottom Right", HudWidgets.POSITIONS)
                .addBool("Show Mouse", true)
                .addBool("Show CPS", true)
                .addBool("Show Space", true)
                .draw(HudWidgets::keystrokes));

        // ---- position -------------------------------------------------------------------
        line("coords", "Coordinates", "Your XYZ position, with Nether conversion.", "XYZ", true, m -> {
            ClientPlayerEntity p = mc().player;
            int d = m.getInt("Decimals");
            String s = fmt(p.getX(), d) + ", " + fmt(p.getY(), d) + ", " + fmt(p.getZ(), d);
            if (m.getBool("Nether Conversion")) {
                String dim = mc().world.getRegistryKey().getValue().getPath();
                if (dim.equals("overworld")) {
                    s += "  (Nether " + fmt(p.getX() / 8, d) + ", " + fmt(p.getZ() / 8, d) + ")";
                } else if (dim.equals("the_nether")) {
                    s += "  (Overworld " + fmt(p.getX() * 8, d) + ", " + fmt(p.getZ() * 8, d) + ")";
                }
            }
            return s;
        }).addNum("Decimals", 1, 0, 3, 1).addBool("Nether Conversion", true);

        line("chunk", "Chunk", "Chunk coordinates and your position inside the chunk.", "Chunk", false, m -> {
            ClientPlayerEntity p = mc().player;
            return (p.getBlockX() >> 4) + ", " + (p.getBlockZ() >> 4)
                    + "  (" + (p.getBlockX() & 15) + ", " + (p.getBlockZ() & 15) + ")";
        });

        line("direction", "Direction", "The way you are facing.", "Facing", true, m -> {
            Direction d = mc().player.getHorizontalFacing();
            String s;
            switch (d) {
                case NORTH: s = "North (-Z)"; break;
                case SOUTH: s = "South (+Z)"; break;
                case EAST: s = "East (+X)"; break;
                case WEST: s = "West (-X)"; break;
                default: s = pretty(d.asString());
            }
            return s;
        });

        line("rotation", "Yaw / Pitch", "Exact view angles.", "Yaw/Pitch", false,
                m -> fmt(MathHelper.wrapDegrees(mc().player.getYaw()), 1) + " / " + fmt(mc().player.getPitch(), 1));

        line("speed", "Speed", "Movement speed in blocks per second.", "Speed", false, m -> {
            ClientPlayerEntity p = mc().player;
            double dx = p.getX() - p.prevX;
            double dz = p.getZ() - p.prevZ;
            double dy = m.getBool("Include Vertical") ? p.getY() - p.prevY : 0;
            return fmt(Math.sqrt(dx * dx + dy * dy + dz * dz) * 20, 2) + " b/s";
        }).addBool("Include Vertical", false);

        line("fall_distance", "Fall Distance", "How far you have fallen. Hidden while on the ground.", "Fall", false,
                m -> mc().player.fallDistance > 1 ? fmt(mc().player.fallDistance, 1) + " blocks" : null);

        line("sprint_state", "Movement State", "Shows when you are sprinting or sneaking.", "State", false, m -> {
            ClientPlayerEntity p = mc().player;
            if (p.isSneaking()) return "Sneaking";
            if (p.isSprinting()) return "Sprinting";
            return null;
        });

        // ---- world ----------------------------------------------------------------------
        line("biome", "Biome", "The biome you are standing in.", "Biome", false, m ->
                mc().world.getBiome(mc().player.getBlockPos()).getKey()
                        .map(k -> pretty(k.getValue().getPath())).orElse("Unknown"));

        line("dimension", "Dimension", "Which dimension you are in.", "Dimension", false,
                m -> pretty(mc().world.getRegistryKey().getValue().getPath()));

        line("light", "Light Level", "Block and sky light at your feet.", "Light", false, m ->
                mc().world.getLightLevel(LightType.BLOCK, mc().player.getBlockPos()) + " block / "
                        + mc().world.getLightLevel(LightType.SKY, mc().player.getBlockPos()) + " sky");

        line("weather", "Weather", "Current weather.", "Weather", false, m -> {
            if (mc().world.isThundering()) return "Thunderstorm";
            if (mc().world.isRaining()) return "Rain";
            return m.getBool("Hide When Clear") ? null : "Clear";
        }).addBool("Hide When Clear", true);

        line("clock", "Real Clock", "Your computer's time.", "Time", false, m ->
                LocalTime.now().format(DateTimeFormatter.ofPattern(m.getBool("24 Hour") ? "HH:mm" : "h:mm a", Locale.ENGLISH)))
                .addBool("24 Hour", true);

        line("game_time", "Game Time", "Minecraft's time of day.", "Game", false, m -> {
            long t = mc().world.getTimeOfDay() % 24000;
            long hours = (t / 1000 + 6) % 24;
            long minutes = (t % 1000) * 60 / 1000;
            return String.format("%02d:%02d", hours, minutes);
        });

        line("day", "Day Counter", "How many in-game days have passed.", "Day", false,
                m -> String.valueOf(mc().world.getTimeOfDay() / 24000));

        line("moon", "Moon Phase", "Current moon phase.", "Moon", false,
                m -> MOON[Math.floorMod(mc().world.getMoonPhase(), 8)]);

        line("session", "Session Timer", "How long this play session has lasted.", "Session", false,
                m -> clock((System.currentTimeMillis() - SESSION_START) / 1000));

        line("gamemode", "Game Mode", "Your current game mode.", "Mode", false, m -> {
            if (mc().interactionManager == null) return null;
            return pretty(mc().interactionManager.getCurrentGameMode().getName());
        });

        // ---- you ------------------------------------------------------------------------
        line("health", "Health", "Your health in hit points.", "HP", false, m -> {
            ClientPlayerEntity p = mc().player;
            String s = fmt(p.getHealth(), 1) + " / " + fmt(p.getMaxHealth(), 0);
            if (p.getAbsorptionAmount() > 0) s += " +" + fmt(p.getAbsorptionAmount(), 0);
            return s;
        });

        line("hunger", "Hunger", "Food and saturation.", "Food", false, m -> {
            ClientPlayerEntity p = mc().player;
            return p.getHungerManager().getFoodLevel() + " / 20  (sat " + fmt(p.getHungerManager().getSaturationLevel(), 1) + ")";
        });

        line("xp", "XP Level", "Your experience level.", "XP", false,
                m -> mc().player.experienceLevel + " (" + Math.round(mc().player.experienceProgress * 100) + "%)");

        line("held_item", "Held Item", "Name, durability or count of the item in your hand.", "Hand", false, m -> {
            ItemStack s = mc().player.getMainHandStack();
            if (s.isEmpty()) return null;
            String n = s.getName().getString();
            if (s.isDamageable()) return n + "  " + (s.getMaxDamage() - s.getDamage()) + "/" + s.getMaxDamage();
            return s.getCount() > 1 ? n + " x" + s.getCount() : n;
        });

        ModuleManager.add(new Module("armor", "Armor Status",
                "Shows your armor and held items with their durability.", Category.HUD)
                .addChoice("Position", "Middle Right", HudWidgets.POSITIONS)
                .addBool("Durability Numbers", true)
                .addBool("Show Held Items", false)
                .draw(HudWidgets::armor));

        ModuleManager.add(new Module("effects", "Potion Effects",
                "Active effects with their remaining time.", Category.HUD)
                .addChoice("Position", "Middle Left", HudWidgets.POSITIONS)
                .draw(HudWidgets::effects));

        counter("arrows", "Arrow Counter", "Arrows", Items.ARROW);
        counter("totems", "Totem Counter", "Totems", Items.TOTEM_OF_UNDYING);
        counter("gapples", "Golden Apple Counter", "Gapples", Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE);
        counter("pearls", "Ender Pearl Counter", "Pearls", Items.ENDER_PEARL);
        counter("fireworks", "Firework Counter", "Fireworks", Items.FIREWORK_ROCKET);

        // ---- combat info ----------------------------------------------------------------
        line("combo", "Combo Counter", "Hits in a row without taking damage.", "Combo", false,
                m -> CombatStats.combo() > 0 ? String.valueOf(CombatStats.combo()) : null);

        line("reach", "Reach Display", "Distance of your last hit.", "Reach", false, m -> {
            double r = CombatStats.lastReach();
            return r < 0 ? null : fmt(r, 2) + " blocks";
        });

        line("target_entity", "Target Info", "Name and health of what you are looking at.", "Target", false, m -> {
            if (!(mc().crosshairTarget instanceof EntityHitResult hit)) return null;
            Entity e = hit.getEntity();
            String s = e.getName().getString();
            if (e instanceof LivingEntity le) s += "  " + fmt(le.getHealth(), 1) + "/" + fmt(le.getMaxHealth(), 0);
            return s;
        });

        line("target_block", "Target Block", "The block you are looking at.", "Block", false, m -> {
            if (mc().crosshairTarget == null || mc().crosshairTarget.getType() != HitResult.Type.BLOCK) return null;
            BlockHitResult hit = (BlockHitResult) mc().crosshairTarget;
            return mc().world.getBlockState(hit.getBlockPos()).getBlock().getName().getString();
        });

        line("tnt_timer", "TNT Timer", "Countdown of the nearest primed TNT.", "TNT", false, m -> {
            float best = Float.MAX_VALUE;
            int n = 0;
            for (Entity e : mc().world.getEntities()) {
                if (e instanceof TntEntity tnt) {
                    n++;
                    best = Math.min(best, tnt.getFuse() / 20f);
                }
            }
            if (n == 0) return null;
            return fmt(best, 1) + "s" + (n > 1 ? "  (" + n + " primed)" : "");
        });
    }
}
