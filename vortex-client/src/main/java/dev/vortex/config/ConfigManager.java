package dev.vortex.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.vortex.VortexClient;
import dev.vortex.module.Module;
import dev.vortex.module.ModuleManager;
import dev.vortex.module.Setting;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Saves module states, key binds and settings to config/vortex.json. */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ConfigManager() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("vortex.json");
    }

    public static void load() {
        Path p = file();
        if (!Files.exists(p)) return;
        try (Reader r = Files.newBufferedReader(p)) {
            JsonElement parsed = JsonParser.parseReader(r);
            if (!parsed.isJsonObject()) return;
            JsonObject modules = parsed.getAsJsonObject().getAsJsonObject("modules");
            if (modules == null) return;
            for (Module m : ModuleManager.all()) {
                JsonObject o = modules.getAsJsonObject(m.id);
                if (o == null) continue;
                if (o.has("enabled")) m.setEnabledSilently(o.get("enabled").getAsBoolean());
                if (o.has("key")) m.key = o.get("key").getAsInt();
                JsonObject s = o.getAsJsonObject("settings");
                if (s == null) continue;
                for (Setting st : m.settings) {
                    if (s.has(st.name)) st.fromJson(s.get(st.name));
                }
            }
        } catch (Exception e) {
            VortexClient.LOGGER.warn("Could not read vortex.json, using defaults", e);
        }
    }

    public static synchronized void save() {
        JsonObject root = new JsonObject();
        JsonObject modules = new JsonObject();
        for (Module m : ModuleManager.all()) {
            JsonObject o = new JsonObject();
            if (m.isToggleable()) o.addProperty("enabled", m.isEnabled());
            o.addProperty("key", m.key);
            JsonObject s = new JsonObject();
            for (Setting st : m.settings) s.add(st.name, st.toJson());
            o.add("settings", s);
            modules.add(m.id, o);
        }
        root.add("modules", modules);
        try (Writer w = Files.newBufferedWriter(file())) {
            GSON.toJson(root, w);
        } catch (IOException e) {
            VortexClient.LOGGER.warn("Could not write vortex.json", e);
        }
    }
}
