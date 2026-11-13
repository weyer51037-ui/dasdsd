package dev.vortex.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.Locale;

/** A single configurable value on a module: a switch, a slider or a list of choices. */
public final class Setting {
    public enum Kind { BOOL, NUMBER, CHOICE }

    public final String name;
    public final Kind kind;
    public final double min, max, step;
    public final String[] choices;

    private final boolean defBool;
    private final double defNum;
    private final int defIndex;
    private boolean bool;
    private double num;
    private int index;

    private Setting(String name, Kind kind, double min, double max, double step, String[] choices,
                    boolean b, double n, int idx) {
        this.name = name;
        this.kind = kind;
        this.min = min;
        this.max = max;
        this.step = step;
        this.choices = choices;
        this.defBool = b;
        this.defNum = n;
        this.defIndex = idx;
        this.bool = b;
        this.num = n;
        this.index = idx;
    }

    public static Setting bool(String name, boolean def) {
        return new Setting(name, Kind.BOOL, 0, 1, 1, null, def, 0, 0);
    }

    public static Setting number(String name, double def, double min, double max, double step) {
        return new Setting(name, Kind.NUMBER, min, max, step, null, false, def, 0);
    }

    public static Setting choice(String name, String def, String... choices) {
        int idx = 0;
        for (int i = 0; i < choices.length; i++) {
            if (choices[i].equals(def)) idx = i;
        }
        return new Setting(name, Kind.CHOICE, 0, choices.length - 1, 1, choices, false, 0, idx);
    }

    public boolean getBool() { return bool; }
    public double getNum() { return num; }
    public int getInt() { return (int) Math.round(num); }
    public int getIndex() { return index; }
    public String getChoice() { return choices[index]; }

    public void toggle() {
        bool = !bool;
        ConfigDirty.mark();
    }

    public void cycle(int dir) {
        int n = choices.length;
        index = ((index + dir) % n + n) % n;
        ConfigDirty.mark();
    }

    public void setNum(double v) {
        if (step > 0) v = Math.round(v / step) * step;
        v = Math.max(min, Math.min(max, v));
        num = Math.round(v * 10000.0) / 10000.0;
        ConfigDirty.mark();
    }

    public double norm() {
        return max == min ? 0 : (num - min) / (max - min);
    }

    public void reset() {
        bool = defBool;
        num = defNum;
        index = defIndex;
        ConfigDirty.mark();
    }

    public String display() {
        switch (kind) {
            case BOOL:
                return bool ? "On" : "Off";
            case CHOICE:
                return getChoice();
            default:
                if (step >= 1) return String.valueOf(getInt());
                return String.format(Locale.ROOT, step < 0.1 ? "%.2f" : "%.1f", num);
        }
    }

    public JsonElement toJson() {
        switch (kind) {
            case BOOL:
                return new JsonPrimitive(bool);
            case CHOICE:
                return new JsonPrimitive(getChoice());
            default:
                return new JsonPrimitive(num);
        }
    }

    public void fromJson(JsonElement e) {
        try {
            switch (kind) {
                case BOOL:
                    bool = e.getAsBoolean();
                    break;
                case CHOICE: {
                    String s = e.getAsString();
                    for (int i = 0; i < choices.length; i++) {
                        if (choices[i].equals(s)) index = i;
                    }
                    break;
                }
                default:
                    num = Math.max(min, Math.min(max, e.getAsDouble()));
            }
        } catch (RuntimeException ignored) {
            // bad value in the config file: keep the default
        }
    }
}
