package dev.vortex.hud;

import dev.vortex.module.Module;
import dev.vortex.module.ModuleManager;
import dev.vortex.ui.Draw;
import dev.vortex.ui.Theme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public final class HudRenderer {
    private HudRenderer() {}

    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        FrameStats.frame();
        ClickTracker.poll(mc);
        if (mc.player == null || mc.world == null || mc.options.hudHidden || mc.options.debugEnabled) return;

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        for (Module m : ModuleManager.all()) {
            if (m.hasDraw()) m.drawHud(ctx, mc, sw, sh);
        }
        drawTextList(ctx, mc, sw, sh);
    }

    private static void drawTextList(DrawContext ctx, MinecraftClient mc, int sw, int sh) {
        Module style = ModuleManager.get("hud_style");
        List<String[]> rows = new ArrayList<>();
        for (Module m : ModuleManager.all()) {
            if (!m.hasValue()) continue;
            String v = m.currentValue();
            if (v == null || v.isEmpty()) continue;
            rows.add(new String[] {m.hudLabel(), v});
        }
        if (rows.isEmpty()) return;

        float scale = (float) style.getNum("Scale");
        int vw = (int) (sw / scale);
        int vh = (int) (sh / scale);
        int margin = style.getInt("Margin");
        boolean bg = style.getBool("Background");
        boolean shadow = style.getBool("Shadow");
        boolean bar = style.getBool("Accent Bar");
        String pos = style.getChoice("Position");
        boolean right = pos.endsWith("Right");
        TextRenderer tr = mc.textRenderer;
        Theme.Palette p = Theme.get();

        int lineH = 11;
        int y = HudWidgets.anchorY(pos, vh, rows.size() * lineH, margin);

        ctx.getMatrices().push();
        ctx.getMatrices().scale(scale, scale, 1f);
        for (String[] row : rows) {
            String label = row[0];
            String value = row[1];
            int labelW = label.isEmpty() ? 0 : tr.getWidth(label) + 4;
            int w = labelW + tr.getWidth(value) + 6;
            int x = right ? vw - margin - w : margin;
            if (bg) Draw.rect(ctx, x, y, w, lineH, 0x66000000);
            if (bar) Draw.rect(ctx, right ? x + w - 1 : x, y, 1, lineH, p.accent());
            int tx = x + (right ? 3 : 4);
            if (!label.isEmpty()) {
                ctx.drawText(tr, label, tx, y + 2, p.accent(), shadow);
            }
            ctx.drawText(tr, value, tx + labelW, y + 2, 0xFFFFFFFF, shadow);
            y += lineH;
        }
        ctx.getMatrices().pop();
    }
}
