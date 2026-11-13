package dev.vortex.ui;

import dev.vortex.module.Module;
import dev.vortex.module.ModuleManager;

/** Colour palettes for the menu and the HUD. All colours are ARGB. */
public final class Theme {
    public record Palette(String name, int accent, int accent2, int panel, int panelAlt, int line,
                          int text, int dim, int bgTop, int bgBottom) {}

    public static final String[] NAMES = {"Vortex", "Ocean", "Ember", "Rose", "Matrix", "Mono"};

    private static final Palette[] PALETTES = {
        new Palette("Vortex", 0xFF8B5CF6, 0xFF22D3EE, 0xF0141425, 0xFF1D1D35, 0xFF2C2C4A, 0xFFF1F1FA, 0xFF8C8CA8, 0xFF0B0B18, 0xFF241446),
        new Palette("Ocean",  0xFF38BDF8, 0xFF34D399, 0xF00E1A2B, 0xFF16263D, 0xFF24405F, 0xFFEFF8FF, 0xFF8AA4BD, 0xFF06101C, 0xFF0B3050),
        new Palette("Ember",  0xFFF97316, 0xFFFACC15, 0xF0201309, 0xFF2D1B0E, 0xFF4A2F18, 0xFFFFF4E8, 0xFFB89A7C, 0xFF140A04, 0xFF4A1F08),
        new Palette("Rose",   0xFFFB7185, 0xFFC084FC, 0xF0211018, 0xFF2E1823, 0xFF4B2A3B, 0xFFFFF0F4, 0xFFBE94A4, 0xFF150A10, 0xFF4A1530),
        new Palette("Matrix", 0xFF22C55E, 0xFF86EFAC, 0xF00A150D, 0xFF10231A, 0xFF1D3D2A, 0xFFEFFFF4, 0xFF84A892, 0xFF040A06, 0xFF0A3318),
        new Palette("Mono",   0xFFE5E7EB, 0xFF9CA3AF, 0xF01A1A1A, 0xFF262626, 0xFF3A3A3A, 0xFFFFFFFF, 0xFF9A9A9A, 0xFF0C0C0C, 0xFF2A2A2A),
    };

    private Theme() {}

    public static Palette get() {
        Module m = ModuleManager.get("menu_style");
        if (m == null) return PALETTES[0];
        int i = m.setting("Theme").getIndex();
        return PALETTES[Math.max(0, Math.min(PALETTES.length - 1, i))];
    }
}
