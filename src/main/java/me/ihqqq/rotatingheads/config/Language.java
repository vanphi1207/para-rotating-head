package me.ihqqq.rotatingheads.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class Language {
    private final YamlConfiguration messages;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public Language(JavaPlugin plugin, Settings settings) {
        String name = "languages/" + settings.language() + ".yml";
        if (!new File(plugin.getDataFolder(), name).exists()) {
            plugin.saveResource(name, false);
        }
        File file = new File(plugin.getDataFolder(), name);
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public Component message(String key, String... replacements) {
        String value = messages.getString(key);
        if (value == null) {
            value = fallback(key);
        }
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            value = value.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return miniMessage.deserialize(value);
    }

    private String fallback(String key) {
        return switch (key) {
            case "command.player-only" -> "<red>Only players can use this command.";
            case "command.head-unknown" -> "<red>Unknown head: <white>{id}</white>";
            case "command.invalid" -> "<red>Invalid value: {reason}";
            default -> "<red>Missing message: " + key;
        };
    }
}