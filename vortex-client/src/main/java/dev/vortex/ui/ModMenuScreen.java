package dev.vortex.ui;

import dev.vortex.VortexClient;
import dev.vortex.config.ConfigManager;
import dev.vortex.module.Category;
import dev.vortex.module.Module;
import dev.vortex.module.ModuleManager;
import dev.vortex.module.Setting;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * The Vortex menu. Everything is drawn directly (no vanilla widgets); each frame the layout
 * code registers click regions, which keeps drawing and hit-testing in one place.
 */
public final class ModMenuScreen extends Screen {
    private static final int SIDE_W = 86;
    private static final int HEADER_H = 30;
    private static final int FOOT_H = 16;
    private static final int ROW_H = 20;

    private interface Click { void on(double mx, double my, int button); }

    private record Hit(int x1, int y1, int x2, int y2, Click click) {}

    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private final long openedAt = System.currentTimeMillis();

    private Category filter;
    private Module selected;
    private String search = "";
    private int listScroll;
    private int setScroll;
    private int setContentH;
    private int listContentH;
    private Module binding;
    private Setting dragging;
    private int dragX;
    private int dragW;

    // regions remembered from the last frame, for scrolling
    private int listX, listY, listW, listH;
    private int setX, setY, setW, setH;

    public ModMenuScreen(Screen parent) {
        super(Text.literal("Vortex"));
        this.parent = parent;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        ConfigManager.save();
        if (client != null) client.setScreen(parent);
    }

    @Override
    public void removed() {
        ConfigManager.save();
    }

    // ---- background ----------------------------------------------------------------------

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        Module style = ModuleManager.get("menu_style");
        String mode = style.getChoice("Background");
        Theme.Palette p = Theme.get();
        boolean inWorld = client != null && client.world != null;

        if (mode.equals("Vanilla") || !inWorld) {
            // Vanilla background. The blur part of it is switched off by the "No Menu Blur" module.
            if (mode.equals("Clear") && !inWorld) {
                Draw.gradient(ctx, 0, 0, width, height, p.bgTop(), p.bgBottom());
            } else {
                super.renderBackground(ctx, mouseX, mouseY, delta);
            }
            return;
        }
        if (mode.equals("Dim")) {
            Draw.gradient(ctx, 0, 0, width, height,
                    Draw.withAlpha(p.bgTop(), 0xB0), Draw.withAlpha(p.bgBottom(), 0xD0));
        }
    }

    // ---- main layout ---------------------------------------------------------------------

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        hits.clear();
        Module style = ModuleManager.get("menu_style");
        float t = Math.min(1f, (System.currentTimeMillis() - openedAt) / 200f);
        float ease = 1f - (1f - t) * (1f - t);
        boolean anim = style.getBool("Animations");
        Draw.alpha = anim ? ease : 1f;
        try {
            renderBackground(ctx, mouseX, mouseY, delta);
            drawPanel(ctx, mouseX, mouseY, anim ? (int) ((1f - ease) * 10) : 0);
        } finally {
            Draw.alpha = 1f;
        }
    }

    private void drawPanel(DrawContext ctx, int mx, int my, int yoff) {
        Theme.Palette p = Theme.get();
        int pw = Math.max(360, Math.min(width - 24, 580));
        int ph = Math.max(220, Math.min(height - 24, 340));
        int px = (width - pw) / 2;
        int py = (height - ph) / 2 + yoff;

        Draw.rrect(ctx, px - 1, py - 1, pw + 2, ph + 2, Draw.withAlpha(p.accent(), 0x90));
        Draw.rrect(ctx, px, py, pw, ph, p.panel());

        // header
        Draw.gradient(ctx, px + 1, py + 1, pw - 2, HEADER_H - 1, Draw.withAlpha(p.accent(), 0x30), 0x00000000);
        Draw.logo(ctx, textRenderer, px + 10, py + 11, p);
        Draw.text(ctx, textRenderer, "Client  v0.2", px + 10 + 46, py + 11, p.dim(), false);
        Draw.rect(ctx, px + 1, py + HEADER_H, pw - 2, 1, p.line());

        // close button
        int cx = px + pw - 22, cy = py + 7;
        boolean closeHover = Draw.inside(mx, my, cx, cy, 15, 15);
        Draw.rrect(ctx, cx, cy, 15, 15, closeHover ? 0xFFB03A48 : p.panelAlt());
        Draw.centered(ctx, textRenderer, "x", cx + 8, cy + 4, p.text(), false);
        hit(cx, cy, 15, 15, (x, y, b) -> close());

        // search box
        int sw = 118, sx = cx - 8 - sw, sy = py + 7;
        Draw.rrect(ctx, sx, sy, sw, 15, p.panelAlt());
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        String shown = search.isEmpty() ? "Search modules..." : search + (blink ? "_" : "");
        Draw.text(ctx, textRenderer, textRenderer.trimToWidth(shown, sw - 8), sx + 4, sy + 4,
                search.isEmpty() ? p.dim() : p.text(), false);

        // sidebar
        int sbx = px + 6, sby = py + HEADER_H + 6;
        drawSidebar(ctx, p, mx, my, sbx, sby, SIDE_W - 6);

        // content areas
        int top = py + HEADER_H + 6;
        int bottom = py + ph - FOOT_H - 4;
        int contentW = pw - SIDE_W - 18;
        listW = (int) (contentW * 0.5);
        setW = contentW - listW - 6;
        listX = px + SIDE_W + 6;
        setX = listX + listW + 6;
        listY = setY = top;
        listH = setH = bottom - top;

        drawModuleList(ctx, p, mx, my);
        drawSettings(ctx, p, mx, my);

        // footer
        Draw.rect(ctx, px + 1, py + ph - FOOT_H, pw - 2, 1, p.line());
        Draw.text(ctx, textRenderer, "Click a name for settings  -  click the switch to toggle",
                px + 8, py + ph - FOOT_H + 4, p.dim(), false);
        String keyName = VortexClient.menuKey.getBoundKeyLocalizedText().getString();
        Draw.rightAligned(ctx, textRenderer, ModuleManager.all().size() + " modules  -  " + keyName + " closes",
                px + pw - 8, py + ph - FOOT_H + 4, p.dim(), false);
    }

    private void drawSidebar(DrawContext ctx, Theme.Palette p, int mx, int my, int x, int y, int w) {
        Category[] cats = Category.values();
        for (int i = -1; i < cats.length; i++) {
            Category c = i < 0 ? null : cats[i];
            String name = c == null ? "All" : c.display;
            int count = 0;
            for (Module m : ModuleManager.all()) {
                if (c == null || m.category == c) count++;
            }
            int ry = y + (i + 1) * 20;
            boolean sel = filter == c;
            boolean hov = Draw.inside(mx, my, x, ry, w, 18);
            if (sel) {
                Draw.rrect(ctx, x, ry, w, 18, Draw.withAlpha(p.accent(), 0x55));
                Draw.rect(ctx, x, ry + 2, 2, 14, p.accent());
            } else if (hov) {
                Draw.rrect(ctx, x, ry, w, 18, p.panelAlt());
            }
            Draw.text(ctx, textRenderer, name, x + 7, ry + 5, sel ? p.text() : p.dim(), false);
            Draw.rightAligned(ctx, textRenderer, String.valueOf(count), x + w - 5, ry + 5, p.dim(), false);
            final Category target = c;
            hit(x, ry, w, 18, (cx, cy, b) -> {
                filter = target;
                listScroll = 0;
            });
        }
    }

    private List<Module> visibleModules() {
        List<Module> out = new ArrayList<>();
        String q = search.toLowerCase(Locale.ROOT);
        for (Module m : ModuleManager.all()) {
            if (filter != null && m.category != filter) continue;
            if (!q.isEmpty() && !m.name.toLowerCase(Locale.ROOT).contains(q)
                    && !m.description.toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(m);
        }
        return out;
    }

    private void drawModuleList(DrawContext ctx, Theme.Palette p, int mx, int my) {
        Draw.rrect(ctx, listX, listY, listW, listH, p.panelAlt());
        List<Module> mods = visibleModules();
        listContentH = mods.size() * ROW_H + 4;
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, listContentH - listH)));

        ctx.enableScissor(listX, listY, listX + listW, listY + listH);
        if (mods.isEmpty()) {
            Draw.centered(ctx, textRenderer, "No modules found", listX + listW / 2, listY + 14, p.dim(), false);
        }
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int ry = listY + 2 + i * ROW_H - listScroll;
            if (ry + ROW_H < listY || ry > listY + listH) continue;
            boolean sel = m == selected;
            boolean hov = Draw.inside(mx, my, listX, ry, listW, ROW_H) && my >= listY && my < listY + listH;
            if (sel) Draw.rect(ctx, listX + 2, ry, listW - 4, ROW_H - 1, Draw.withAlpha(p.accent(), 0x40));
            else if (hov) Draw.rect(ctx, listX + 2, ry, listW - 4, ROW_H - 1, 0x22FFFFFF);
            if (sel) Draw.rect(ctx, listX + 2, ry, 2, ROW_H - 1, p.accent());

            boolean on = m.isEnabled();
            int nameCol = on || m.isAction() || !m.isToggleable() ? p.text() : p.dim();
            Draw.text(ctx, textRenderer, textRenderer.trimToWidth(m.name, listW - 52), listX + 9, ry + 6, nameCol, false);

            clippedHit(listX, ry, listW, ROW_H, listY, listY + listH, (cx, cy, b) -> selected = m);

            int pillX = listX + listW - 30;
            if (m.isAction()) {
                Draw.rrect(ctx, pillX - 4, ry + 4, 30, 12, p.accent());
                Draw.centered(ctx, textRenderer, "RUN", pillX + 11, ry + 6, 0xFF101010, false);
                clippedHit(pillX - 4, ry + 4, 30, 12, listY, listY + listH, (cx, cy, b) -> m.activate());
            } else if (m.isToggleable()) {
                drawSwitch(ctx, p, pillX, ry + 5, on);
                clippedHit(pillX - 3, ry + 2, 30, ROW_H - 4, listY, listY + listH, (cx, cy, b) -> m.toggle());
            }
        }
        ctx.disableScissor();
        drawScrollbar(ctx, p, listX + listW - 3, listY, listH, listContentH, listScroll);
    }

    private void drawSwitch(DrawContext ctx, Theme.Palette p, int x, int y, boolean on) {
        Draw.rrect(ctx, x, y, 24, 10, on ? p.accent() : 0xFF3A3A4C);
        int kx = on ? x + 14 : x + 2;
        Draw.rrect(ctx, kx, y + 2, 8, 6, 0xFFFFFFFF);
    }

    private void drawScrollbar(DrawContext ctx, Theme.Palette p, int x, int y, int h, int content, int scroll) {
        if (content <= h) return;
        int barH = Math.max(14, h * h / content);
        int barY = y + (int) ((h - barH) * (scroll / (double) (content - h)));
        Draw.rect(ctx, x, barY, 2, barH, Draw.withAlpha(p.accent(), 0xAA));
    }

    // ---- settings pane -------------------------------------------------------------------

    private void drawSettings(DrawContext ctx, Theme.Palette p, int mx, int my) {
        Draw.rrect(ctx, setX, setY, setW, setH, p.panelAlt());
        if (selected == null) {
            Draw.centered(ctx, textRenderer, "Select a module", setX + setW / 2, setY + setH / 2 - 4, p.dim(), false);
            return;
        }
        Module m = selected;
        int ix = setX + 9;
        int iw = setW - 18;
        int vy1 = setY, vy2 = setY + setH;
        int y = setY + 7 - setScroll;
        int start = y;

        ctx.enableScissor(setX, setY, setX + setW, setY + setH);

        Draw.text(ctx, textRenderer, m.name, ix, y, p.accent(), true);
        y += 12;
        for (OrderedText line : textRenderer.wrapLines(Text.literal(m.description), iw)) {
            ctx.drawText(textRenderer, line, ix, y, Draw.fade(p.dim()), false);
            y += 10;
        }
        y += 2;
        Draw.text(ctx, textRenderer, m.category.display, ix, y, Draw.mix(p.accent2(), p.dim(), 0.35f), false);
        y += 13;
        Draw.rect(ctx, ix, y - 3, iw, 1, p.line());
        y += 3;

        if (m.isAction()) {
            Draw.rrect(ctx, ix, y, iw, 16, p.accent());
            Draw.centered(ctx, textRenderer, "Run now", ix + iw / 2, y + 4, 0xFF101010, false);
            clippedHit(ix, y, iw, 16, vy1, vy2, (cx, cy, b) -> m.activate());
            y += 22;
        }

        // keybind row
        Draw.text(ctx, textRenderer, "Keybind", ix, y + 4, p.text(), false);
        String keyText = binding == m ? "Press a key..." : m.key < 0 ? "None" : keyName(m.key);
        int kw = Math.max(54, textRenderer.getWidth(keyText) + 10);
        int kx = ix + iw - kw;
        Draw.rrect(ctx, kx, y, kw, 16, binding == m ? p.accent() : p.panel());
        Draw.centered(ctx, textRenderer, keyText, kx + kw / 2, y + 4, binding == m ? 0xFF101010 : p.text(), false);
        clippedHit(kx, y, kw, 16, vy1, vy2, (cx, cy, b) -> {
            if (b == 1) {
                m.key = -1;
                binding = null;
            } else {
                binding = m;
            }
        });
        y += 22;

        for (Setting s : m.settings) {
            switch (s.kind) {
                case BOOL -> {
                    Draw.text(ctx, textRenderer, s.name, ix, y + 4, p.text(), false);
                    drawSwitch(ctx, p, ix + iw - 24, y + 3, s.getBool());
                    clippedHit(ix, y, iw, 16, vy1, vy2, (cx, cy, b) -> s.toggle());
                    y += 20;
                }
                case CHOICE -> {
                    Draw.text(ctx, textRenderer, s.name, ix, y + 4, p.text(), false);
                    String v = "< " + s.getChoice() + " >";
                    int w = textRenderer.getWidth(v) + 10;
                    int bx = ix + iw - w;
                    Draw.rrect(ctx, bx, y, w, 16, p.panel());
                    Draw.text(ctx, textRenderer, v, bx + 5, y + 4, p.accent2(), false);
                    clippedHit(bx, y, w, 16, vy1, vy2, (cx, cy, b) -> s.cycle(b == 1 ? -1 : 1));
                    y += 20;
                }
                case NUMBER -> {
                    Draw.text(ctx, textRenderer, s.name, ix, y + 2, p.text(), false);
                    Draw.rightAligned(ctx, textRenderer, s.display(), ix + iw, y + 2, p.accent2(), false);
                    int ty = y + 15;
                    Draw.rrect(ctx, ix, ty, iw, 5, p.panel());
                    int fillW = (int) (iw * s.norm());
                    if (fillW > 0) Draw.rrect(ctx, ix, ty, Math.max(2, fillW), 5, p.accent());
                    Draw.rrect(ctx, ix + Math.max(0, Math.min(iw - 4, fillW - 2)), ty - 2, 4, 9, 0xFFFFFFFF);
                    final int sliderX = ix, sliderW = iw;
                    clippedHit(ix, y + 10, iw, 14, vy1, vy2, (cx, cy, b) -> {
                        dragging = s;
                        dragX = sliderX;
                        dragW = sliderW;
                        updateSlider(cx);
                    });
                    y += 28;
                }
            }
        }

        if (!m.settings.isEmpty()) {
            Draw.rrect(ctx, ix, y + 2, iw, 15, p.panel());
            Draw.centered(ctx, textRenderer, "Reset to defaults", ix + iw / 2, y + 6, p.dim(), false);
            clippedHit(ix, y + 2, iw, 15, vy1, vy2, (cx, cy, b) -> m.resetSettings());
            y += 22;
        }

        ctx.disableScissor();
        setContentH = y - start + 14;
        setScroll = Math.max(0, Math.min(setScroll, Math.max(0, setContentH - setH)));
        drawScrollbar(ctx, p, setX + setW - 3, setY, setH, setContentH, setScroll);
    }

    private void updateSlider(double mx) {
        if (dragging == null) return;
        double f = Math.max(0, Math.min(1, (mx - dragX) / dragW));
        dragging.setNum(dragging.min + (dragging.max - dragging.min) * f);
    }

    private static String keyName(int key) {
        try {
            return InputUtil.fromKeyCode(key, 0).getLocalizedText().getString();
        } catch (RuntimeException e) {
            return "Key " + key;
        }
    }

    // ---- hit regions ---------------------------------------------------------------------

    private void hit(int x, int y, int w, int h, Click c) {
        hits.add(new Hit(x, y, x + w, y + h, c));
    }

    private void clippedHit(int x, int y, int w, int h, int clipTop, int clipBottom, Click c) {
        int y1 = Math.max(y, clipTop);
        int y2 = Math.min(y + h, clipBottom);
        if (y2 <= y1) return;
        hits.add(new Hit(x, y1, x + w, y2, c));
    }

    // ---- input ---------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (mx >= h.x1() && mx < h.x2() && my >= h.y1() && my < h.y2()) {
                h.click().on(mx, my, button);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) {
            updateSlider(mx);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null) {
            dragging = null;
            ConfigManager.save();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        int step = (int) (-vertical * 18);
        if (Draw.inside(mx, my, listX, listY, listW, listH)) {
            listScroll = Math.max(0, Math.min(listScroll + step, Math.max(0, listContentH - listH)));
            return true;
        }
        if (Draw.inside(mx, my, setX, setY, setW, setH)) {
            setScroll = Math.max(0, Math.min(setScroll + step, Math.max(0, setContentH - setH)));
            return true;
        }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                binding = null;
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                binding.key = -1;
                binding = null;
            } else {
                binding.key = keyCode;
                binding = null;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (!search.isEmpty()) {
                search = "";
                listScroll = 0;
            } else {
                close();
            }
            return true;
        }
        if (VortexClient.menuKey.matchesKey(keyCode, scanCode)) {
            close();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            search = search.substring(0, search.length() - 1);
            listScroll = 0;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (binding != null) return true;
        if (chr >= 32 && chr != 127 && search.length() < 24) {
            search += chr;
            listScroll = 0;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }
}
