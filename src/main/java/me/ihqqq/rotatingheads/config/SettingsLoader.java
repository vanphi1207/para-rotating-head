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
                Math.max(0L, config.getLong("interaction_cooldown_ms", 500L)),
                config.getBoolean("bedrock_proxy", true),
                Math.max(0.05, Math.min(5.0, config.getDouble("bedrock_scale_multiplier", 0.85))),
                config.getDouble("bedrock_y_offset", 0.0),
                Math.max(1, Math.min(20, config.getInt("bedrock_update_ticks", 2))));
    }
}