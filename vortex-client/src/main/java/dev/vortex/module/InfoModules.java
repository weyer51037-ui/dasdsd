package dev.vortex.module;

import static dev.vortex.module.HudModules.clock;
import static dev.vortex.module.HudModules.counter;
import static dev.vortex.module.HudModules.fmt;
import static dev.vortex.module.HudModules.line;
import static dev.vortex.module.HudModules.mc;
import static dev.vortex.module.HudModules.pretty;

import dev.vortex.hud.FrameStats;
import java.lang.management.ManagementFactory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;

/** A second batch of HUD information lines and item counters. */
final class InfoModules {
    private static double trip;
    private static double maxSpeed;
    private static final String[] CARDINALS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    private InfoModules() {}

    private static int countNear(Class<? extends Entity> type, double radius) {
        ClientPlayerEntity p = mc().player;
        int n = 0;
        for (Entity e : mc().world.getEntities()) {
            if (type.isInstance(e) && e.squaredDistanceTo(p) <= radius * radius) n++;
        }
        return n;
    }

    static void register() {
        registerPlayerLines();
        registerWorldLines();
        registerSystemLines();
        registerCounters();
    }

    private static void registerPlayerLines() {
        line("air", "Air Supply", "Remaining breath underwater. Hidden when full.", "Air", false, m -> {
            ClientPlayerEntity p = mc().player;
            return p.getAir() >= p.getMaxAir() ? null : fmt(Math.max(0, p.getAir()) / 20.0, 1) + "s";
        });

        line("armor_points", "Armor Points", "Total armor protection you are wearing.", "Armor", false,
                m -> String.valueOf(mc().player.getArmor()));

        line("attack_cooldown", "Attack Cooldown", "How charged your next attack is.", "Attack", false,
                m -> Math.round(mc().player.getAttackCooldownProgress(0f) * 100) + "%");

        line("selected_slot", "Selected Slot", "Which hotbar slot is active.", "Slot", false,
                m -> String.valueOf(mc().player.getInventory().selectedSlot + 1));

        line("free_slots", "Free Slots", "Empty inventory slots.", "Slots", false, m -> {
            int free = 0;
            for (ItemStack s : mc().player.getInventory().main) {
                if (s.isEmpty()) free++;
            }
            return free + " / 36";
        });

        line("offhand", "Offhand Item", "What is in your offhand.", "Offhand", false, m -> {
            ItemStack s = mc().player.getOffHandStack();
            if (s.isEmpty()) return null;
            return s.getCount() > 1 ? s.getName().getString() + " x" + s.getCount() : s.getName().getString();
        });

        line("held_count", "Held Item Total", "How many of the item in your hand you carry in total.", "Total", false, m -> {
            ItemStack s = mc().player.getMainHandStack();
            return s.isEmpty() ? null : String.valueOf(mc().player.getInventory().count(s.getItem()));
        });

        line("total_xp", "Total XP Points", "Your total experience points.", "XP pts", false,
                m -> String.valueOf(mc().player.totalExperience));

        line("standing_on", "Standing On", "The block under your feet.", "On", false,
                m -> mc().world.getBlockState(mc().player.getBlockPos().down()).getBlock().getName().getString());

        line("vehicle", "Vehicle", "What you are riding.", "Riding", false,
                m -> mc().player.getVehicle() == null ? null : mc().player.getVehicle().getName().getString());

        line("status_flags", "Fire / Freeze", "Shows when you are burning or freezing.", "Status", false, m -> {
            if (mc().player.isOnFire()) return "Burning";
            if (mc().player.getFrozenTicks() > 0) return "Freezing";
            return null;
        });

        line("fall_damage", "Fall Damage", "Damage you would take if you landed now.", "Fall dmg", false, m -> {
            double d = Math.ceil(mc().player.fallDistance - 3);
            return d <= 0 ? null : fmt(d / 2.0, 1) + " hearts";
        });

        line("y_level", "Y Level", "Just your height.", "Y", false, m -> String.valueOf(mc().player.getBlockY()));

        line("sea_level", "Height Above Sea", "How far above or below sea level you are.", "Sea", false,
                m -> fmt(mc().player.getY() - mc().world.getSeaLevel(), 0));

        line("trip_meter", "Trip Meter", "Distance walked this session. Reset it with the Reset Trip Meter key.", "Trip", false,
                m -> fmt(trip, 0) + " blocks")
                .onTick((mc, self) -> {
                    ClientPlayerEntity p = mc.player;
                    double dx = p.getX() - p.prevX, dz = p.getZ() - p.prevZ;
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d < 20) trip += d;
                    maxSpeed = Math.max(maxSpeed, Math.min(d * 20, 100));
                });

        line("max_speed", "Top Speed", "Fastest speed you reached this session.", "Top", false,
                m -> fmt(maxSpeed, 1) + " b/s");

        ModuleManager.add(new Module("reset_trip", "Reset Trip Meter", "Bind a key to zero the trip meter and top speed.",
                Category.HUD).action(() -> {
            trip = 0;
            maxSpeed = 0;
        }));

        line("compass", "Compass", "A compass heading with degrees.", "", false, m -> {
            float heading = (MathHelper.wrapDegrees(mc().player.getYaw()) + 180f + 360f) % 360f;
            int i = Math.round(heading / 45f) % 8;
            return CARDINALS[Math.floorMod(i - 2, 8)] + "  " + CARDINALS[Math.floorMod(i - 1, 8)] + "  ["
                    + CARDINALS[i] + "]  " + CARDINALS[(i + 1) % 8] + "  " + CARDINALS[(i + 2) % 8]
                    + "   " + Math.round(heading) + "\u00B0";
        });

        line("target_pos", "Target Position", "Coordinates of the block you are looking at.", "Looking at", false, m -> {
            HitResult r = mc().crosshairTarget;
            if (r == null || r.getType() != HitResult.Type.BLOCK) return null;
            BlockHitResult b = (BlockHitResult) r;
            return b.getBlockPos().getX() + ", " + b.getBlockPos().getY() + ", " + b.getBlockPos().getZ();
        });

        line("target_distance", "Target Distance", "How far away what you are looking at is.", "Distance", false, m -> {
            HitResult r = mc().crosshairTarget;
            if (r == null || r.getType() == HitResult.Type.MISS) return null;
            return fmt(r.getPos().distanceTo(mc().player.getEyePos()), 1) + " blocks";
        });
    }

    private static void registerWorldLines() {
        line("hostile_count", "Hostile Mobs Nearby", "Hostile mobs within the radius.", "Hostiles", false, m -> {
            int n = countNear(HostileEntity.class, m.getNum("Radius"));
            return n == 0 && m.getBool("Hide When Zero") ? null : String.valueOf(n);
        }).addNum("Radius", 24, 8, 64, 4).addBool("Hide When Zero", true);

        line("passive_count", "Passive Mobs Nearby", "Animals and villagers within the radius.", "Passives", false, m -> {
            int n = countNear(PassiveEntity.class, m.getNum("Radius"));
            return n == 0 && m.getBool("Hide When Zero") ? null : String.valueOf(n);
        }).addNum("Radius", 24, 8, 64, 4).addBool("Hide When Zero", true);

        line("item_count", "Dropped Items Nearby", "Item entities on the ground nearby.", "Items", false, m -> {
            int n = countNear(ItemEntity.class, m.getNum("Radius"));
            return n == 0 && m.getBool("Hide When Zero") ? null : String.valueOf(n);
        }).addNum("Radius", 16, 4, 64, 4).addBool("Hide When Zero", true);

        line("orb_count", "XP Orbs Nearby", "Experience orbs nearby.", "Orbs", false, m -> {
            int n = countNear(ExperienceOrbEntity.class, m.getNum("Radius"));
            return n == 0 && m.getBool("Hide When Zero") ? null : String.valueOf(n);
        }).addNum("Radius", 16, 4, 64, 4).addBool("Hide When Zero", true);

        line("players_online", "Players Online", "Players in the server list.", "Players", false, m -> {
            ClientPlayNetworkHandler nh = mc().getNetworkHandler();
            return nh == null ? null : String.valueOf(nh.getPlayerList().size());
        });

        line("world_border", "World Border", "Distance to the world border when you are close to it.", "Border", false, m -> {
            double d = mc().world.getWorldBorder().getDistanceInsideBorder(mc().player);
            return d > m.getNum("Show Within") ? null : fmt(Math.max(0, d), 0) + " blocks";
        }).addNum("Show Within", 200, 20, 2000, 20);

        line("difficulty", "Difficulty", "The world difficulty.", "Difficulty", false,
                m -> pretty(mc().world.getDifficulty().getName()));

        line("world_age", "World Age", "How long the world has existed, in days.", "World age", false,
                m -> fmt(mc().world.getTime() / 24000.0, 1) + " days");

        line("day_cycle", "Day / Night Timer", "Real time until the next sunset or sunrise.", "Cycle", false, m -> {
            long t = mc().world.getTimeOfDay() % 24000;
            return t < 12000 ? "Night in " + clock((12000 - t) / 20) : "Day in " + clock((24000 - t) / 20);
        });
    }

    private static void registerSystemLines() {
        line("render_distance", "Render Distance", "Your current render distance.", "Render", false,
                m -> mc().options.getViewDistance().getValue() + " chunks");

        line("sim_distance", "Simulation Distance", "Your current simulation distance.", "Sim", false,
                m -> mc().options.getSimulationDistance().getValue() + " chunks");

        line("fps_limit", "FPS Limit", "Your frame rate cap.", "Cap", false, m -> {
            int v = mc().options.getMaxFps().getValue();
            return v >= 260 ? "Unlimited" : String.valueOf(v);
        });

        line("vsync_state", "VSync", "Whether VSync is on.", "VSync", false,
                m -> mc().options.getEnableVsync().getValue() ? "On" : "Off");

        line("resolution", "Resolution", "Game window resolution in pixels.", "Res", false,
                m -> mc().getWindow().getFramebufferWidth() + "x" + mc().getWindow().getFramebufferHeight());

        line("gui_scale", "GUI Scale", "Current GUI scale factor.", "GUI", false,
                m -> fmt(mc().getWindow().getScaleFactor(), 0) + "x");

        line("java_version", "Java Version", "The Java runtime in use.", "Java", false,
                m -> System.getProperty("java.version"));

        line("mc_version", "Minecraft Version", "The game version.", "MC", false,
                m -> FabricLoader.getInstance().getModContainer("minecraft")
                        .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?"));

        line("mod_count", "Loaded Mods", "How many mods are loaded.", "Mods", false,
                m -> String.valueOf(FabricLoader.getInstance().getAllMods().size()));

        line("cpu_cores", "CPU Threads", "Available processor threads.", "CPU", false,
                m -> String.valueOf(Runtime.getRuntime().availableProcessors()));

        line("os_name", "Operating System", "Your operating system.", "OS", false,
                m -> System.getProperty("os.name"));

        line("uptime", "Game Uptime", "How long the game has been running.", "Uptime", false,
                m -> clock(ManagementFactory.getRuntimeMXBean().getUptime() / 1000));

        line("date", "Date", "Today's date.", "Date", false,
                m -> LocalDate.now().format(DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH)));

        line("avg_frame", "Average Frame Time", "Average of recent frame times.", "Avg", false, m -> {
            float[] r = FrameStats.recent(60);
            float sum = 0;
            int n = 0;
            for (float f : r) {
                if (f > 0) {
                    sum += f;
                    n++;
                }
            }
            return n == 0 ? null : fmt(sum / n, 1) + " ms";
        });
    }

    private static void registerCounters() {
        counter("c_diamond", "Diamond Counter", "Diamonds", Items.DIAMOND);
        counter("c_emerald", "Emerald Counter", "Emeralds", Items.EMERALD);
        counter("c_iron", "Iron Counter", "Iron", Items.IRON_INGOT);
        counter("c_gold", "Gold Counter", "Gold", Items.GOLD_INGOT);
        counter("c_copper", "Copper Counter", "Copper", Items.COPPER_INGOT);
        counter("c_netherite", "Netherite Counter", "Netherite", Items.NETHERITE_INGOT);
        counter("c_scrap", "Netherite Scrap Counter", "Scrap", Items.NETHERITE_SCRAP);
        counter("c_coal", "Coal Counter", "Coal", Items.COAL, Items.CHARCOAL);
        counter("c_redstone", "Redstone Counter", "Redstone", Items.REDSTONE);
        counter("c_lapis", "Lapis Counter", "Lapis", Items.LAPIS_LAZULI);
        counter("c_quartz", "Quartz Counter", "Quartz", Items.QUARTZ);
        counter("c_amethyst", "Amethyst Counter", "Amethyst", Items.AMETHYST_SHARD);
        counter("c_obsidian", "Obsidian Counter", "Obsidian", Items.OBSIDIAN);
        counter("c_cobble", "Cobblestone Counter", "Cobble", Items.COBBLESTONE);
        counter("c_torch", "Torch Counter", "Torches", Items.TORCH);
        counter("c_tnt", "TNT Counter", "TNT", Items.TNT);
        counter("c_water", "Water Bucket Counter", "Water", Items.WATER_BUCKET);
        counter("c_lava", "Lava Bucket Counter", "Lava", Items.LAVA_BUCKET);
        counter("c_bread", "Bread Counter", "Bread", Items.BREAD);
        counter("c_beef", "Steak Counter", "Steak", Items.COOKED_BEEF);
        counter("c_pork", "Porkchop Counter", "Porkchops", Items.COOKED_PORKCHOP);
        counter("c_carrot", "Golden Carrot Counter", "Golden carrots", Items.GOLDEN_CARROT);
        counter("c_potato", "Baked Potato Counter", "Potatoes", Items.BAKED_POTATO);
        counter("c_eye", "Eye of Ender Counter", "Eyes", Items.ENDER_EYE);
        counter("c_blaze", "Blaze Rod Counter", "Blaze rods", Items.BLAZE_ROD);
        counter("c_xp_bottle", "XP Bottle Counter", "XP bottles", Items.EXPERIENCE_BOTTLE);
        counter("c_string", "String Counter", "String", Items.STRING);
        counter("c_gunpowder", "Gunpowder Counter", "Gunpowder", Items.GUNPOWDER);
        counter("c_potion", "Potion Counter", "Potions", Items.POTION);
        counter("c_splash", "Splash Potion Counter", "Splash", Items.SPLASH_POTION);
    }
}
