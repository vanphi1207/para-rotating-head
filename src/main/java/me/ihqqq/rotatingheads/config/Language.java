package me.ihqqq.rotatingheads.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class Language {
    private final JavaPlugin plugin;
    private final SettingsHolder settings;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private YamlConfiguration messages;

    public Language(JavaPlugin plugin, SettingsHolder settings) {
        this.plugin = plugin;
        this.settings = settings;
        reload();
    }

    public void reload() {
        String name = "languages/" + settings.get().language() + ".yml";
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists() && plugin.getResource(name) != null) {
            plugin.saveResource(name, false);
        }
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);

        YamlConfiguration defaults = readBundled(name);
        if (defaults == null) {
            defaults = readBundled("languages/en_us.yml");
        }
        if (defaults != null) {
            loaded.setDefaults(defaults);
            loaded.options().copyDefaults(false);
        }
        messages = loaded;
    }

    private YamlConfiguration readBundled(String name) {
        try (InputStream stream = plugin.getResource(name)) {
            if (stream == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception exception) {
            return null;
        }
    }

    public Component message(String key, String... replacements) {
        String value = messages.getString(key);
        if (value == null) {
            value = "<red>Missing message: " + key;
        }
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            // Escaped so ids, exception text or usage strings can never inject tags.
            value = value.replace("{" + replacements[i] + "}",
                    miniMessage.escapeTags(replacements[i + 1]));
        }
        return miniMessage.deserialize(value);
    }
}