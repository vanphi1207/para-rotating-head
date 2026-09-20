package me.ihqqq.rotatingheads.config;

import me.ihqqq.rotatingheads.model.HeadModels.Brightness;
import me.ihqqq.rotatingheads.model.HeadModels.Head;
import me.ihqqq.rotatingheads.model.HeadModels.HeadOptions;
import me.ihqqq.rotatingheads.model.HeadModels.Hologram;
import me.ihqqq.rotatingheads.model.HeadModels.Interaction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HeadRepository {
    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration yaml;

    public HeadRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), "heads.yml");
    }

    public void load() {
        if (!file.exists()) {
            plugin.saveResource("heads.yml", false);
        }
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    public Map<String, Head> all() {
        Map<String, Head> result = new LinkedHashMap<>();
        for (String id : yaml.getKeys(false)) {
            try {
                Head head = decode(id, yaml.getConfigurationSection(id));
                if (head != null) {
                    result.put(id, head);
                }
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Skipping malformed head " + id + ": "
                        + exception.getMessage());
            }
        }
        return result;
    }

    public Map<String, Head> forWorld(String worldName) {
        Map<String, Head> result = new LinkedHashMap<>();
        for (String id : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(id);
            if (section == null
                    || !worldName.equals(section.getString("location.world"))) {
                continue;
            }
            try {
                Head head = decode(id, section);
                if (head != null) {
                    result.put(id, head);
                }
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Skipping malformed head " + id + ": "
                        + exception.getMessage());
            }
        }
        return result;
    }

    public void save(Head head) {
        String path = head.id();
        Location location = head.location();
        yaml.set(path + ".location.world", location.getWorld().getName());
        yaml.set(path + ".location.x", location.getX());
        yaml.set(path + ".location.y", location.getY());
        yaml.set(path + ".location.z", location.getZ());
        yaml.set(path + ".display_range", head.displayRange());
        yaml.set(path + ".options.texture", head.options().texture());
        yaml.set(path + ".options.scale", head.options().scale());
        yaml.set(path + ".options.speedY", head.options().speed());
        yaml.set(path + ".options.speedX", head.options().speedX());
        yaml.set(path + ".options.speedZ", head.options().speedZ());
        yaml.set(path + ".options.bob.height", head.options().bobHeight());
        yaml.set(path + ".options.bob.period", head.options().bobPeriod());
        yaml.set(path + ".options.brightness.sky", head.options().brightness().sky());
        yaml.set(path + ".options.brightness.block", head.options().brightness().block());
        yaml.set(path + ".interaction.enable", head.interaction().enabled());
        yaml.set(path + ".interaction.any-click", head.interaction().any());
        yaml.set(path + ".interaction.left-click", head.interaction().left());
        yaml.set(path + ".interaction.right-click", head.interaction().right());
        yaml.set(path + ".holograms.enable", head.hologram().enabled());
        yaml.set(path + ".holograms.offsetY", head.hologram().offsetY());
        yaml.set(path + ".holograms.refresh_interval", head.hologram().refreshTicks());
        yaml.set(path + ".holograms.lines", head.hologram().lines());
        yaml.set(path + ".holograms.options.scale", head.hologram().scale());
        yaml.set(path + ".holograms.options.see_through", head.hologram().seeThrough());
        yaml.set(path + ".holograms.options.text_shadow", head.hologram().shadow());
        yaml.set(path + ".holograms.options.background", head.hologram().background());
        yaml.set(path + ".holograms.options.brightness.sky",
                head.hologram().brightness().sky());
        yaml.set(path + ".holograms.options.brightness.block",
                head.hologram().brightness().block());
        yaml.set(path + ".holograms.provider", head.hologram().provider());
        yaml.set(path + ".holograms.link", head.hologram().link());
        yaml.set(path + ".holograms.follow_head", head.hologram().followHead());
        yaml.set(path + ".options.item", null);
        saveAtomic();
    }

    public void remove(String id) {
        yaml.set(id, null);
        saveAtomic();
    }

    private Head decode(String id, ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        ConfigurationSection locationSection = section.getConfigurationSection("location");
        if (locationSection == null) {
            throw new IllegalArgumentException("missing location");
        }
        World world = Bukkit.getWorld(locationSection.getString("world", ""));
        if (world == null) {
            throw new IllegalArgumentException("world is not loaded");
        }
        Location location = new Location(world, locationSection.getDouble("x"),
                locationSection.getDouble("y"), locationSection.getDouble("z"));
        ConfigurationSection options = section.getConfigurationSection("options");
        if (options == null) {
            options = section;
        }
        String texture = options.getString("texture", "");
        double scale = options.getDouble("scale", 2);
        double speed = options.contains("speedY")
                ? options.getDouble("speedY") : section.getDouble("speed", 1);
        Brightness brightness = brightness(options.getConfigurationSection("brightness"));
        double speedX = options.getDouble("speedX", 0);
        double speedZ = options.getDouble("speedZ", 0);
        double bobHeight = options.getDouble("bob.height", 0);
        int bobPeriod = options.getInt("bob.period", 60);
        ConfigurationSection interaction = section.getConfigurationSection("interaction");
        Interaction interactionValue = new Interaction(interaction != null
                && interaction.getBoolean("enable", false),
                list(interaction, "any-click"), list(interaction, "left-click"),
                list(interaction, "right-click"));
        ConfigurationSection hologram = section.getConfigurationSection("holograms");
        ConfigurationSection hologramOptions = hologram == null ? null
                : hologram.getConfigurationSection("options");
        Hologram hologramValue = new Hologram(hologram != null
                && hologram.getBoolean("enable", false),
                hologram == null ? 1 : hologram.getDouble("offsetY", 1),
                hologram == null ? 0 : hologram.getInt("refresh_interval", 0),
                hologram == null ? List.of() : hologram.getStringList("lines"),
                hologramOptions == null ? 1 : hologramOptions.getDouble("scale", 1),
                hologramOptions != null && hologramOptions.getBoolean("see_through", false),
                hologramOptions == null || hologramOptions.getBoolean("text_shadow", true),
                hologramOptions == null ? "default"
                        : hologramOptions.getString("background", "default"),
                brightness(hologramOptions == null ? null
                        : hologramOptions.getConfigurationSection("brightness")),
                hologram == null ? "native" : hologram.getString("provider", "native"),
                hologram == null ? "" : hologram.getString("link", ""),
                hologram == null || hologram.getBoolean("follow_head", true));
        return new Head(id, location, section.getInt("display_range", 64),
                new HeadOptions(texture, scale, speed, brightness, speedX, speedZ,
                        bobHeight, bobPeriod), interactionValue,
                hologramValue);
    }

    private Brightness brightness(ConfigurationSection section) {
        return new Brightness(section == null ? 15 : section.getInt("sky", 15),
                section == null ? 15 : section.getInt("block", 15));
    }

    private List<String> list(ConfigurationSection section, String key) {
        return section == null ? List.of() : section.getStringList(key);
    }

    private void saveAtomic() {
        try {
            Path temporary = Path.of(file.getPath() + ".tmp");
            yaml.save(temporary.toFile());
            try {
                Files.move(temporary, file.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save heads.yml: " + exception.getMessage());
        }
    }
}