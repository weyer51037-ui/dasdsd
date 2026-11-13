package dev.vortex.feature;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.option.SimpleOption;

/**
 * Changes vanilla options while remembering the player's own value, so turning a module off
 * (or closing the game) puts everything back.
 */
public final class OptionTweaks {
    private static final List<OptionTweaks> ALL = new ArrayList<>();

    private final Map<SimpleOption<?>, Object> saved = new LinkedHashMap<>();

    public OptionTweaks() {
        ALL.add(this);
    }

    /** With on=true sets the option, with on=false puts the original value back. */
    @SuppressWarnings("unchecked")
    public <T> void apply(SimpleOption<T> opt, T value, boolean on) {
        if (on) {
            saved.putIfAbsent(opt, opt.getValue());
            if (!Objects.equals(opt.getValue(), value)) opt.setValue(value);
        } else if (saved.containsKey(opt)) {
            T old = (T) saved.remove(opt);
            if (!Objects.equals(opt.getValue(), old)) opt.setValue(old);
        }
    }

    @SuppressWarnings("unchecked")
    public void restore() {
        for (Map.Entry<SimpleOption<?>, Object> e : saved.entrySet()) {
            ((SimpleOption<Object>) e.getKey()).setValue(e.getValue());
        }
        saved.clear();
    }

    public static void restoreAll() {
        for (OptionTweaks t : ALL) {
            try {
                t.restore();
            } catch (RuntimeException ignored) {
                // options may already be gone while the game is closing
            }
        }
    }
}
