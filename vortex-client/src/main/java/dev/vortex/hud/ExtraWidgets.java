package dev.vortex.hud;

import dev.vortex.feature.CombatStats;
import dev.vortex.module.Module;
import dev.vortex.ui.Draw;
import dev.vortex.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.effect.StatusEffectInstance;

/** Overlays and graphs that draw their own pixels. */
public final class ExtraWidgets {
    public static final String[] COLORS =
            {"Theme", "White", "Red", "Green", "Blue", "Yellow", "Cyan", "Pink", "Orange", "Black"};

    private static final int SAMPLES = 120;
    private static final int[] PING = new int[SAMPLES];
    private static final int[] MEM = new int[SAMPLES];
    private static long lastPingSample;
    private static long lastMemSample;

    private ExtraWidgets() {}

    public static int color(String name, int alpha) {
        int rgb;
        switch (name) {
            case "White": rgb = 0xFFFFFF; break;
            case "Red": rgb = 0xFF4D4D; break;
            case "Green": rgb = 0x4DFF6A; break;
            case "Blue": rgb = 0x4D8DFF; break;
            case "Yellow": rgb = 0xFFE14D; break;
            case "Cyan": rgb = 0x4DEBFF; break;
            case "Pink": rgb = 0xFF6AD5; break;
            case "Orange": rgb = 0xFF9A3D; break;
            case "Black": rgb = 0x000000; break;
            default: rgb = Theme.get().accent() & 0xFFFFFF;
        }
        return Draw.withAlpha(rgb, Math.max(0, Math.min(255, alpha)));
    }

    private static double speed(ClientPlayerEntity p) {
        double dx = p.getX() - p.prevX, dz = p.getZ() - p.prevZ;
        return Math.sqrt(dx * dx + dz * dz) * 20;
    }

    // ---- crosshair -----------------------------------------------------------------------

    private static void px(DrawContext g, int x, int y, int w, int h, int col, boolean outline) {
        if (outline) g.fill(x - 1, y - 1, x + w + 1, y + h + 1, col & 0xFF000000);
        g.fill(x, y, x + w, y + h, col);
    }

    public static void crosshair(DrawContext g, MinecraftClient mc, int sw, int sh, Module m) {
        int cx = sw / 2, cy = sh / 2;
        int size = m.getInt("Size"), th = m.getInt("Thickness"), gap = m.getInt("Gap");
        boolean outline = m.getBool("Outline");
        if (m.getBool("Dynamic")) {
            if (speed(mc.player) > 0.5) gap += 2;
            if (mc.player.isSprinting()) gap += 2;
        }
        int col = color(m.getChoice("Color"), (int) (m.getNum("Opacity") * 2.55));
        int h0 = th / 2;
        switch (m.getChoice("Style")) {
            case "Dot" -> px(g, cx - th, cy - th, th * 2, th * 2, col, outline);
            case "T-Shape" -> {
                px(g, cx - gap - size, cy - h0, size, th, col, outline);
                px(g, cx + gap, cy - h0, size, th, col, outline);
                px(g, cx - h0, cy + gap, th, size, col, outline);
            }
            case "X" -> {
                for (int i = gap; i < gap + size; i++) {
                    px(g, cx - i, cy - i, th, th, col, false);
                    px(g, cx + i, cy - i, th, th, col, false);
                    px(g, cx - i, cy + i, th, th, col, false);
                    px(g, cx + i, cy + i, th, th, col, false);
                }
            }
            case "Circle" -> {
                int r = size / 2 + gap + 2;
                int pts = Math.max(16, r * 6);
                for (int i = 0; i < pts; i++) {
                    double a = i * 2 * Math.PI / pts;
                    px(g, cx + (int) Math.round(Math.cos(a) * r) - h0, cy + (int) Math.round(Math.sin(a) * r) - h0, th, th, col, false);
                }
            }
            case "Square" -> {
                int s = size / 2 + gap + 1;
                px(g, cx - s, cy - s, s * 2 + th, th, col, false);
                px(g, cx - s, cy + s, s * 2 + th, th, col, false);
                px(g, cx - s, cy - s, th, s * 2 + th, col, false);
                px(g, cx + s, cy - s, th, s * 2 + th, col, false);
            }
            default -> { // "Cross" and "Cross + Dot"
                px(g, cx - gap - size, cy - h0, size, th, col, outline);
                px(g, cx + gap, cy - h0, size, th, col, outline);
                px(g, cx - h0, cy - gap - size, th, size, col, outline);
                px(g, cx - h0, cy + gap, th, size, col, outline);
                if (m.getChoice("Style").equals("Cross + Dot")) px(g, cx - h0, cy - h0, th, th, col, outline);
            }
        }
    }

    public static void hitMarker(DrawContext g, MinecraftClient mc, int sw, int sh, Module m) {
        long dt = System.currentTimeMillis() - CombatStats.lastAttack();
        long life = (long) m.getNum("Duration");
        if (dt < 0 || dt > life) return;
        int alpha = (int) (255 * (1.0 - dt / (double) life));
        int col = color(m.getChoice("Color"), alpha);
        int cx = sw / 2, cy = sh / 2;
        for (int i = 4; i < 4 + m.getInt("Size"); i++) {
            g.fill(cx - i, cy - i, cx - i + 1, cy - i + 1, col);
            g.fill(cx + i, cy - i, cx + i + 1, cy - i + 1, col);
            g.fill(cx - i, cy + i, cx - i + 1, cy + i + 1, col);
            g.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, col);
        }
    }

    public static void cooldownBar(DrawContext g, MinecraftClient mc, int sw, int sh, Module m) {
        float c = mc.player.getAttackCooldownProgress(0f);
        if (c >= 1f && m.getBool("Hide When Full")) return;
        int w = 26, x = sw / 2 - w / 2, y = sh / 2 + 12;
        Draw.rect(g, x - 1, y - 1, w + 2, 5, 0x88000000);
        Draw.rect(g, x, y, (int) (w * c), 3, c >= 1f ? Theme.get().accent2() : Theme.get().accent());
    }

    // ---- bars and graphs -----------------------------------------------------------------

    public static void timeBar(DrawContext g, MinecraftClient mc, int sw, int sh, Module m) {
        int w = 120, h = 6;
        String pos = m.getChoice("Position");
        int x = HudWidgets.anchorX(pos, sw, w, 6);
        int y = HudWidgets.anchorY(pos, sh, h + 10, 6);
        long t = mc.world.getTimeOfDay() % 24000;
        Draw.rect(g, x - 1, y - 1, w + 2, h + 2, 0x88000000);
        Draw.rect(g, x, y, w / 2, h, 0xFFFFD34D);
        Draw.rect(g, x + w / 2, y, w - w / 2, h, 0xFF23306B);
        int mx = x + (int) (w * (t / 24000.0));
        Draw.rect(g, mx - 1, y - 2, 2, h + 4, 0xFFFFFFFF);
        TextRenderer tr = mc.textRenderer;
        Draw.centered(g, tr, t < 12000 ? "Day" : "Night", x + w / 2, y + h + 2, 0xFFFFFFFF, true);
    }

    public static void speedBar(DrawContext g, MinecraftClient mc, int sw, int sh, Module m) {
        int w = 100, h = 6;
        String pos = m.getChoice("Position");
        int x = HudWidgets.anchorX(pos, sw, w, 6);
        int y = HudWidgets.anchorY(pos, sh, h + 10, 6);
        double s = speed(mc.player);
        double max = m.getNum("Max Speed");
        Theme.Palette p = Theme.get();
        Draw.rect(g, x - 1, y - 1, w + 2, h + 2, 0x88000000);
        Draw.rect(g, x, y, (int) (w * Math.min(1.0, s / max)), h, Draw.mix(p.accent2(), 0xFFFF5555, (float) (s / max)));
        Draw.centered(g, mc.textRenderer, String.format("%.1f b/s", s), x + w / 2, y + h + 2, 0xFFFFFFFF, true);
    }

    public static void pingGraph(DrawContext g, MinecraftClient mc, int sw, int sh, Module m) {
        long now = System.currentTimeMillis();
        if (now - lastPingSample > 500) {
            lastPingSample = now;
            System.arraycopy(PING, 1, PING, 0, SAMPLES - 1);
            int v = 0;
            ClientPlayNetworkHandler nh = mc.getNetworkHandler();
            if (nh != null) {
                PlayerListEntry e = nh.getPlayerListEntry(mc.player.getUuid());
                if (e != null) v = e.getLatency();
            }
            PING[SAMPLES - 1] = v;
        }
        drawGraph(g, mc, sw, sh, m, PING, 300, "Ping " + PING[SAMPLES - 1] + " ms");
    }

    public static void memoryGraph(DrawContext g, MinecraftClient mc, int sw, int sh, Module m) {
        long now = System.currentTimeMillis();
        Runtime r = Runtime.getRuntime();
        if (now - lastMemSample > 500) {
            lastMemSample = now;
            System.arraycopy(MEM, 1, MEM, 0, SAMPLES - 1);
            MEM[SAMPLES - 1] = (int) ((r.totalMemory() - r.freeMemory()) >> 20);
        }
        drawGraph(g, mc, sw, sh, m, MEM, (int) (r.maxMemory() >> 20), "RAM " + MEM[SAMPLES - 1] + " MB");
    }

    private static void drawGraph(DrawContext g, MinecraftClient mc, int sw, int sh, Module m, int[] data, int cap, String label) {
        int w = SAMPLES, h = 30;
        String pos = m.getChoice("Position");
        int x = HudWidgets.anchorX(pos, sw, w, 6);
        int y = HudWidgets.anchorY(pos, sh, h + 10, 6);
        Theme.Palette p = Theme.get();
        Draw.rect(g, x, y + 10, w, h, 0x66000000);
        for (int i = 0; i < SAMPLES; i++) {
            int bh = Math.min(h, (int) Math.round(data[i] / (double) Math.max(1, cap) * h));
            if (bh > 0) Draw.rect(g, x + i, y + 10 + h - bh, 1, bh, p.accent());
        }
        g.drawText(mc.textRenderer, label, x + 2, y + 1, 0xFFFFFFFF, true);
    }

    // ---- warnings ------------------------------------------------------------------------

    public static void effectWarning(DrawContext g, MinecraftClient mc, int sw, int sh, Module m) {
        int limit = (int) (m.getNum("Seconds") * 20);
        int row = 0;
        for (StatusEffectInstance e : mc.player.getStatusEffects()) {
            if (e.isInfinite() || e.getDuration() > limit) continue;
            String name = e.getEffectType().value().getName().getString();
            String text = name + " ends in " + (e.getDuration() / 20 + 1) + "s";
            boolean flash = (System.currentTimeMillis() / 400) % 2 == 0;
            Draw.centered(g, mc.textRenderer, text, sw / 2, sh - 76 - row * 11, flash ? 0xFFFF6666 : 0xFFFFD34D, true);
            row++;
        }
    }

    // ---- screen decoration ---------------------------------------------------------------

    public static void watermark(DrawContext g, MinecraftClient mc, int sw, int sh, Module m) {
        StringBuilder sb = new StringBuilder("Vortex");
        if (m.getBool("Show FPS")) sb.append("  |  ").append(FrameStats.fps()).append(" FPS");
        String s = sb.toString();
        TextRenderer tr = mc.textRenderer;
        int w = tr.getWidth(s) + 10;
        String pos = m.getChoice("Position");
        int x = HudWidgets.anchorX(pos, sw, w, 6);
        int y = HudWidgets.anchorY(pos, sh, 14, 6);
        Theme.Palette p = Theme.get();
        Draw.rect(g, x, y, w, 14, 0x88000000);
        Draw.rect(g, x, y + 13, w, 1, p.accent());
        g.drawText(tr, s, x + 5, y + 3, 0xFFFFFFFF, true);
    }

    public static void colorOverlay(DrawContext g, MinecraftClient mc, int sw, int sh, Module m) {
        g.fill(0, 0, sw, sh, color(m.getChoice("Color"), (int) (m.getNum("Opacity") * 2.55)));
    }

    public static void ruleOfThirds(DrawContext g, MinecraftClient mc, int sw, int sh, Module m) {
        int col = 0x66FFFFFF;
        g.fill(sw / 3, 0, sw / 3 + 1, sh, col);
        g.fill(sw * 2 / 3, 0, sw * 2 / 3 + 1, sh, col);
        g.fill(0, sh / 3, sw, sh / 3 + 1, col);
        g.fill(0, sh * 2 / 3, sw, sh * 2 / 3 + 1, col);
        if (m.getBool("Center Lines")) {
            g.fill(sw / 2, 0, sw / 2 + 1, sh, 0x44FFFFFF);
            g.fill(0, sh / 2, sw, sh / 2 + 1, 0x44FFFFFF);
        }
    }

    public static void letterbox(DrawContext g, MinecraftClient mc, int sw, int sh, Module m) {
        int bar = (int) (sh * m.getNum("Size") / 100.0);
        g.fill(0, 0, sw, bar, 0xFF000000);
        g.fill(0, sh - bar, sw, sh, 0xFF000000);
    }

    public static void screenBorder(DrawContext g, MinecraftClient mc, int sw, int sh, Module m) {
        int t = m.getInt("Thickness");
        int col = color(m.getChoice("Color"), 200);
        g.fill(0, 0, sw, t, col);
        g.fill(0, sh - t, sw, sh, col);
        g.fill(0, t, t, sh - t, col);
        g.fill(sw - t, t, sw, sh - t, col);
    }
}
