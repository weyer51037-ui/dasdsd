package dev.vortex.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;

/** Hit tracking for the combo and reach displays. */
public final class CombatStats {
    private static int combo;
    private static long lastHit;
    private static double lastReach;
    private static long lastReachTime;
    private static int prevHurt;
    private static long lastAttack;

    private CombatStats() {}

    public static void onAttack(Entity target) {
        MinecraftClient mc = MinecraftClient.getInstance();
        long now = System.currentTimeMillis();
        if (now - lastHit > 3000) combo = 0;
        combo++;
        lastHit = now;
        lastAttack = now;
        if (mc.player != null && mc.crosshairTarget instanceof EntityHitResult hit) {
            lastReach = mc.player.getEyePos().distanceTo(hit.getPos());
            lastReachTime = now;
        }
    }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null) return;
        int hurt = mc.player.hurtTime;
        if (hurt > 0 && prevHurt == 0) combo = 0;
        prevHurt = hurt;
        if (combo > 0 && System.currentTimeMillis() - lastHit > 3000) combo = 0;
    }

    public static int combo() { return combo; }

    public static long lastAttack() { return lastAttack; }

    /** Distance of the last hit, or a negative number when it is older than five seconds. */
    public static double lastReach() {
        return System.currentTimeMillis() - lastReachTime < 5000 ? lastReach : -1;
    }
}
