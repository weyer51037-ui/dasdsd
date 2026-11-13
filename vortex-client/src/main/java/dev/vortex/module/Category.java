package dev.vortex.module;

public enum Category {
    HUD("HUD"),
    VISUAL("Visual"),
    GAMEPLAY("Gameplay"),
    TOOLS("Tools"),
    OPTIONS("Options"),
    PERFORMANCE("Performance"),
    CLIENT("Client");

    public final String display;

    Category(String display) {
        this.display = display;
    }
}
