package me.ihqqq.rotatingheads.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;

public final class ConfigUpdater {
    private ConfigUpdater() {
    }

    public static void updateAll(JavaPlugin plugin, Collection<String> resources) {
        for (String resource : resources) {
            try {
                update(plugin, resource);
            } catch (IOException | RuntimeException exception) {
                plugin.getLogger().warning("Could not update " + resource + ": "
                        + exception.getMessage());
            }
        }
    }

    private static void update(JavaPlugin plugin, String resource) throws IOException {
        YamlConfiguration bundled = readBundled(plugin, resource);
        if (bundled == null) {
            return;
        }
        File file = new File(plugin.getDataFolder(), resource);
        if (!file.exists()) {
            plugin.saveResource(resource, false);
            return;
        }
        YamlConfiguration current = new YamlConfiguration();
        try {
            current.load(file);
        } catch (org.bukkit.configuration.InvalidConfigurationException exception) {
            plugin.getLogger().warning(resource + " has a syntax error, so it was not "
                    + "updated: " + exception.getMessage());
            return;
        }
        int added = 0;
        for (String path : bundled.getKeys(true)) {
            if (bundled.isConfigurationSection(path) || current.contains(path, true)
                    || blockedByParent(current, path)) {
                continue;
            }
            current.set(path, bundled.get(path));
            current.setComments(path, bundled.getComments(path));
            current.setInlineComments(path, bundled.getInlineComments(path));
            added++;
        }
        if (added == 0) {
            return;
        }
        Path target = file.toPath();
        Files.copy(target, target.resolveSibling(file.getName() + ".bak"),
                StandardCopyOption.REPLACE_EXISTING);
        Path temporary = target.resolveSibling(file.getName() + ".tmp");
        current.save(temporary.toFile());
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
        plugin.getLogger().info("Added " + added + " new key(s) to " + resource
                + " (previous version saved as " + file.getName() + ".bak).");
    }

    private static boolean blockedByParent(YamlConfiguration current, String path) {
        int dot = path.lastIndexOf('.');
        while (dot > 0) {
            String parent = path.substring(0, dot);
            if (current.contains(parent, true) && !current.isConfigurationSection(parent)) {
                return true;
            }
            dot = parent.lastIndexOf('.');
        }
        return false;
    }

    private static YamlConfiguration readBundled(JavaPlugin plugin, String resource)
            throws IOException {
        try (InputStream stream = plugin.getResource(resource)) {
            if (stream == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }
}