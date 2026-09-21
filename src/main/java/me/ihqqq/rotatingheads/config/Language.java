package me.ihqqq.rotatingheads.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class Language {
    private static final List<String> BUNDLED_LANGUAGES = List.of("en_us", "vi_vn");
    private static final String DEFAULT_PREFIX =
            "<dark_gray>[<gold>ParaRotatingHead</gold><dark_gray>]</dark_gray> ";

    private final JavaPlugin plugin;
    private final SettingsHolder settings;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private YamlConfiguration messages;
    private Component prefix;

    public Language(JavaPlugin plugin, SettingsHolder settings) {
        this.plugin = plugin;
        this.settings = settings;
        extractBundled();
        reload();
    }

    private void extractBundled() {
        for (String code : BUNDLED_LANGUAGES) {
            String name = "languages/" + code + ".yml";
            File file = new File(plugin.getDataFolder(), name);
            if (!file.exists() && plugin.getResource(name) != null) {
                plugin.saveResource(name, false);
            }
        }
    }

    public void reload() {
        extractBundled();
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
        prefix = miniMessage.deserialize(messages.getString("prefix", DEFAULT_PREFIX));
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
        return prefix.append(fragment(key, replacements));
    }

    public Component fragment(String key, String... replacements) {
        String value = messages.getString(key);
        if (value == null) {
            value = "<red>Missing message: " + key;
        }
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            value = value.replace("{" + replacements[i] + "}",
                    miniMessage.escapeTags(replacements[i + 1]));
        }
        return miniMessage.deserialize(value);
    }
}