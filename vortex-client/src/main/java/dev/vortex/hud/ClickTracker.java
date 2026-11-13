package dev.vortex.hud;

import java.util.ArrayDeque;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/** Counts mouse clicks per second by polling the buttons once per frame. */
public final class ClickTracker {
    private static final ArrayDeque<Long> LEFT = new ArrayDeque<>();
    private static final ArrayDeque<Long> RIGHT = new ArrayDeque<>();
    private static boolean leftDown;
    private static boolean rightDown;

    private ClickTracker() {}

    public static void poll(MinecraftClient mc) {
        long w = mc.getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(w, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(w, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        long now = System.currentTimeMillis();
        boolean inGame = mc.currentScreen == null;
        if (l && !leftDown && inGame) LEFT.add(now);
        if (r && !rightDown && inGame) RIGHT.add(now);
        leftDown = l;
        rightDown = r;
        prune(LEFT, now);
        prune(RIGHT, now);
    }

    private static void prune(ArrayDeque<Long> q, long now) {
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
    }

    public static int left() { return LEFT.size(); }
    public static int right() { return RIGHT.size(); }
    public static boolean leftDown() { return leftDown; }
    public static boolean rightDown() { return rightDown; }
}
