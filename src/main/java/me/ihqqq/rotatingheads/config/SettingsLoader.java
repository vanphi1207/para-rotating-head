package me.ihqqq.rotatingheads.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class SettingsLoader {
    private final JavaPlugin plugin;

    public SettingsLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public Settings load() {
        if (!new File(plugin.getDataFolder(), "setting.yml").exists()) {
            plugin.saveResource("setting.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "setting.yml"));
        return new Settings(config.getString("language", "en_us"),
                config.getString("default_head_texture", ""),
                Math.max(0L, config.getLong("interaction_cooldown_ms", 500L)));
    }
}