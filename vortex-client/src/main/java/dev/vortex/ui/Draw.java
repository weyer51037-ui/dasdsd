package dev.vortex.ui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Small drawing helpers. {@link #alpha} fades everything drawn through them (used by menu animations). */
public final class Draw {
    public static float alpha = 1f;

    private Draw() {}

    public static int fade(int c) {
        if (alpha >= 0.999f) return c;
        int a = (int) (((c >>> 24) & 0xFF) * alpha);
        return (a << 24) | (c & 0xFFFFFF);
    }

    public static int withAlpha(int c, int a) {
        return (a << 24) | (c & 0xFFFFFF);
    }

    public static int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (int) (((a >>> 24) & 0xFF) * (1 - t) + ((b >>> 24) & 0xFF) * t);
        int ar = (int) (((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int ag = (int) (((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int ab = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (aa << 24) | (ar << 16) | (ag << 8) | ab;
    }

    public static void rect(DrawContext g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + h, fade(c));
    }

    /** Rectangle with the four corner pixels cut off. */
    public static void rrect(DrawContext g, int x, int y, int w, int h, int c) {
        int f = fade(c);
        g.fill(x + 1, y, x + w - 1, y + h, f);
        g.fill(x, y + 1, x + 1, y + h - 1, f);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, f);
    }

    public static void gradient(DrawContext g, int x, int y, int w, int h, int top, int bottom) {
        g.fillGradient(x, y, x + w, y + h, fade(top), fade(bottom));
    }

    public static void text(DrawContext g, TextRenderer tr, String s, int x, int y, int c, boolean shadow) {
        g.drawText(tr, s, x, y, fade(c), shadow);
    }

    public static void centered(DrawContext g, TextRenderer tr, String s, int cx, int y, int c, boolean shadow) {
        g.drawText(tr, s, cx - tr.getWidth(s) / 2, y, fade(c), shadow);
    }

    public static void rightAligned(DrawContext g, TextRenderer tr, String s, int right, int y, int c, boolean shadow) {
        g.drawText(tr, s, right - tr.getWidth(s), y, fade(c), shadow);
    }

    /** The VORTEX wordmark with a colour sweep from accent to accent2. */
    public static void logo(DrawContext g, TextRenderer tr, int x, int y, Theme.Palette p) {
        String word = "VORTEX";
        int cx = x;
        for (int i = 0; i < word.length(); i++) {
            String ch = String.valueOf(word.charAt(i));
            int col = mix(p.accent(), p.accent2(), i / (float) (word.length() - 1));
            g.drawText(tr, ch, cx, y, fade(col), true);
            cx += tr.getWidth(ch) + 1;
        }
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
