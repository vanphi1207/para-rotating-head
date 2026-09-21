package me.ihqqq.rotatingheads.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class SettingsLoader {
    private final JavaPlugin plugin;

    public SettingsLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public Settings load() {
        File file = new File(plugin.getDataFolder(), "setting.yml");
        if (!file.exists()) {
            plugin.saveResource("setting.yml", false);
        }
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().severe("Could not parse setting.yml, falling back to "
                    + "defaults until this is fixed: " + exception.getMessage());
        }
        return new Settings(config.getString("language", "en_us"),
                config.getString("default_head_texture", ""),
                Math.max(0L, config.getLong("interaction_cooldown_ms", 500L)));
    }
}