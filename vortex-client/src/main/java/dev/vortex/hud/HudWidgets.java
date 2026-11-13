package dev.vortex.hud;

import dev.vortex.module.Module;
import dev.vortex.ui.Draw;
import dev.vortex.ui.Theme;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;

/** The HUD elements that draw their own graphics instead of a single line of text. */
public final class HudWidgets {
    public static final String[] POSITIONS =
            {"Top Left", "Top Right", "Middle Left", "Middle Right", "Bottom Left", "Bottom Right"};

    private HudWidgets() {}

    public static int anchorX(String pos, int sw, int w, int margin) {
        return pos.endsWith("Right") ? sw - w - margin : margin;
    }

    public static int anchorY(String pos, int sh, int h, int margin) {
        if (pos.startsWith("Top")) return margin;
        if (pos.startsWith("Bottom")) return sh - h - margin;
        return (sh - h) / 2;
    }

    // ---- keystrokes ----------------------------------------------------------------------

    public static void keystrokes(DrawContext g, MinecraftClient mc, int sw, int sh, Module m) {
        TextRenderer tr = mc.textRenderer;
        Theme.Palette p = Theme.get();
        boolean mouse = m.getBool("Show Mouse");
        boolean space = m.getBool("Show Space");
        int k = 20, gap = 2, w = k * 3 + gap * 2;
        int h = k * 2 + gap + (mouse ? k + gap : 0) + (space ? 12 + gap : 0);
        String pos = m.getChoice("Position");
        int x = anchorX(pos, sw, w, 6);
        int y = anchorY(pos, sh, h, 6);

        key(g, tr, p, x + k + gap, y, k, k, "W", mc.options.forwardKey.isPressed());
        int ry = y + k + gap;
        key(g, tr, p, x, ry, k, k, "A", mc.options.leftKey.isPressed());
        key(g, tr, p, x + k + gap, ry, k, k, "S", mc.options.backKey.isPressed());
        key(g, tr, p, x + (k + gap) * 2, ry, k, k, "D", mc.options.rightKey.isPressed());
        int cy = ry + k + gap;
        if (mouse) {
            int half = (w - gap) / 2;
            mouseKey(g, tr, p, x, cy, half, k, "LMB", ClickTracker.left(), ClickTracker.leftDown(), m.getBool("Show CPS"));
            mouseKey(g, tr, p, x + half + gap, cy, half, k, "RMB", ClickTracker.right(), ClickTracker.rightDown(), m.getBool("Show CPS"));
            cy += k + gap;
        }
        if (space) {
            boolean down = mc.options.jumpKey.isPressed();
            Draw.rect(g, x, cy, w, 12, down ? p.accent() : 0x70000000);
            Draw.rect(g, x + w / 2 - 10, cy + 5, 20, 1, down ? 0xFF101010 : 0xFFDDDDDD);
        }
    }

    private static void key(DrawContext g, TextRenderer tr, Theme.Palette p, int x, int y, int w, int h, String label, boolean down) {
        Draw.rect(g, x, y, w, h, down ? p.accent() : 0x70000000);
        Draw.centered(g, tr, label, x + w / 2, y + (h - 8) / 2, down ? 0xFF101010 : 0xFFFFFFFF, false);
    }

    private static void mouseKey(DrawContext g, TextRenderer tr, Theme.Palette p, int x, int y, int w, int h,
                                 String label, int cps, boolean down, boolean showCps) {
        Draw.rect(g, x, y, w, h, down ? p.accent() : 0x70000000);
        int fg = down ? 0xFF101010 : 0xFFFFFFFF;
        if (showCps) {
            Draw.centered(g, tr, label, x + w / 2, y + 2, fg, false);
            Draw.centered(g, tr, cps + " CPS", x + w / 2, y + 11, down ? 0xFF202020 : 0xFFB0B0B0, false);
        } else {
            Draw.centered(g, tr, label, x + w / 2, y + (h - 8) / 2, fg, false);
        }
    }

    // ---- armor ---------------------------------------------------------------------------

    public static void armor(DrawContext g, MinecraftClient mc, int sw, int sh, Module m) {
        ClientPlayerEntity player = mc.player;
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack s : player.getArmorItems()) stacks.add(s);
        Collections.reverse(stacks); // head first
        if (m.getBool("Show Held Items")) {
            stacks.add(player.getMainHandStack());
            stacks.add(player.getOffHandStack());
        }
        boolean numbers = m.getBool("Durability Numbers");
        int rowH = 18;
        int w = 18 + (numbers ? 36 : 0);
        int h = stacks.size() * rowH;
        String pos = m.getChoice("Position");
        int x = anchorX(pos, sw, w, 6);
        int y = anchorY(pos, sh, h, 6);
        boolean rightSide = pos.endsWith("Right");
        TextRenderer tr = mc.textRenderer;
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack s = stacks.get(i);
            int ry = y + i * rowH;
            if (s.isEmpty()) continue;
            int ix = rightSide && numbers ? x + 36 : x;
            g.drawItem(s, ix, ry);
            g.drawItemInSlot(tr, s, ix, ry);
            if (numbers && s.isDamageable()) {
                int left = s.getMaxDamage() - s.getDamage();
                float pct = left / (float) s.getMaxDamage();
                int col = Draw.mix(0xFFFF4444, 0xFF55FF55, pct);
                String t = String.valueOf(left);
                int tx = rightSide ? x + 34 - tr.getWidth(t) : x + 20;
                g.drawText(tr, t, tx, ry + 5, col, true);
            }
        }
    }

    // ---- potion effects ------------------------------------------------------------------

    public static void effects(DrawContext g, MinecraftClient mc, int sw, int sh, Module m) {
        List<StatusEffectInstance> list = new ArrayList<>(mc.player.getStatusEffects());
        if (list.isEmpty()) return;
        TextRenderer tr = mc.textRenderer;
        List<String> names = new ArrayList<>();
        List<String> times = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        int w = 0;
        for (StatusEffectInstance e : list) {
            String name = e.getEffectType().value().getName().getString();
            int amp = e.getAmplifier();
            if (amp > 0) name += " " + roman(amp + 1);
            String time = e.isInfinite() ? "inf" : format(e.getDuration() / 20);
            int col = e.isInfinite() || e.getDuration() > 200 ? 0xFFFFFFFF : Draw.mix(0xFFFF5555, 0xFFFFFFFF, e.getDuration() / 200f);
            names.add(name);
            times.add(time);
            colors.add(col);
            w = Math.max(w, tr.getWidth(name) + 8 + tr.getWidth(time));
        }
        int h = list.size() * 11;
        String pos = m.getChoice("Position");
        int x = anchorX(pos, sw, w + 6, 6);
        int y = anchorY(pos, sh, h, 6);
        for (int i = 0; i < names.size(); i++) {
            int ry = y + i * 11;
            Draw.rect(g, x, ry, w + 6, 11, 0x66000000);
            g.drawText(tr, names.get(i), x + 3, ry + 2, 0xFFFFFFFF, true);
            g.drawText(tr, times.get(i), x + 3 + w - tr.getWidth(times.get(i)), ry + 2, colors.get(i), true);
        }
    }

    private static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n < r.length ? r[n] : String.valueOf(n);
    }

    private static String format(int seconds) {
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    // ---- frame graph ---------------------------------------------------------------------

    public static void frameGraph(DrawContext g, MinecraftClient mc, int sw, int sh, Module m) {
        int w = 120, h = 32;
        String pos = m.getChoice("Position");
        int x = anchorX(pos, sw, w, 6);
        int y = anchorY(pos, sh, h, 6);
        Theme.Palette p = Theme.get();
        Draw.rect(g, x, y, w, h, 0x66000000);
        float[] ms = FrameStats.recent(w);
        float cap = 33.3f; // bars hit the top at 30 FPS
        for (int i = 0; i < w; i++) {
            if (ms[i] <= 0) continue;
            int bh = Math.max(1, Math.min(h, Math.round(ms[i] / cap * h)));
            int col = ms[i] > 25f ? 0xFFFF5555 : ms[i] > 16.7f ? 0xFFFFD34D : p.accent2();
            Draw.rect(g, x + i, y + h - bh, 1, bh, col);
        }
        Draw.rect(g, x, y + h - Math.round(16.7f / cap * h), w, 1, 0x33FFFFFF);
    }

    // ---- warnings ------------------------------------------------------------------------

    public static void lowHealth(DrawContext g, MinecraftClient mc, int sw, int sh, Module m) {
        float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        float limit = (float) m.getNum("Hearts") * 2f;
        if (hp > limit || mc.player.isDead()) return;
        double pulse = 0.55 + 0.45 * Math.sin(System.currentTimeMillis() / 220.0);
        float strength = (float) m.getNum("Intensity");
        int a = (int) (110 * strength * pulse * (1.0 - hp / (limit + 1f)));
        a = Math.max(0, Math.min(200, a));
        int red = a << 24 | 0xFF1010;
        int none = 0x00FF1010;
        int edge = Math.max(24, sh / 5);
        g.fillGradient(0, 0, sw, edge, red, none);
        g.fillGradient(0, sh - edge, sw, sh, none, red);
        int sideW = Math.max(24, sw / 8);
        g.fill(0, edge, 2, sh - edge, red);
        g.fill(sw - 2, edge, sw, sh - edge, red);
        // side gradients drawn as thin vertical strips
        for (int i = 0; i < sideW; i += 2) {
            int sa = (int) (a * (1.0 - i / (double) sideW));
            int col = sa << 24 | 0xFF1010;
            g.fill(i, edge, i + 2, sh - edge, col);
            g.fill(sw - i - 2, edge, sw - i, sh - edge, col);
        }
    }

    public static void lowDurability(DrawContext g, MinecraftClient mc, int sw, int sh, Module m) {
        double limit = m.getNum("Percent") / 100.0;
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(mc.player.getMainHandStack());
        for (ItemStack s : mc.player.getArmorItems()) stacks.add(s);
        String warn = null;
        double worst = 2;
        for (ItemStack s : stacks) {
            if (s.isEmpty() || !s.isDamageable()) continue;
            double pct = (s.getMaxDamage() - s.getDamage()) / (double) s.getMaxDamage();
            if (pct <= limit && pct < worst) {
                worst = pct;
                warn = s.getName().getString() + " is at " + Math.round(pct * 100) + "%";
            }
        }
        if (warn == null) return;
        boolean flash = (System.currentTimeMillis() / 500) % 2 == 0;
        Draw.centered(g, mc.textRenderer, "Low durability: " + warn, sw / 2, sh / 2 + 34,
                flash ? 0xFFFF5555 : 0xFFFFD34D, true);
    }
}
