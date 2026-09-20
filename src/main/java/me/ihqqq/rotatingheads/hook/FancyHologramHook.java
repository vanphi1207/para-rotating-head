package me.ihqqq.rotatingheads.hook;

import me.clip.placeholderapi.PlaceholderAPI;
import me.ihqqq.rotatingheads.model.HeadModels.Head;
import me.ihqqq.rotatingheads.model.HeadModels.Hologram;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.joml.Vector3f;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Reflection adapter: FancyHolograms remains an optional runtime dependency.
 */
public final class FancyHologramHook {
    private final JavaPlugin plugin;
    private final Object manager;

    private FancyHologramHook(JavaPlugin plugin, Object manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public static FancyHologramHook connect(JavaPlugin plugin) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("FancyHolograms")) {
            return null;
        }
        try {
            Class<?> api = Class.forName(
                    "de.oliver.fancyholograms.api.FancyHologramsPlugin");
            boolean enabled = (boolean) api.getMethod("isEnabled").invoke(null);
            if (!enabled) {
                return null;
            }
            Object instance = api.getMethod("get").invoke(null);
            Object manager = instance.getClass().getMethod("getHologramManager")
                    .invoke(instance);
            return new FancyHologramHook(plugin, manager);
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().warning("FancyHolograms was detected but its API could not be loaded.");
            return null;
        }
    }

    public Object create(Head head, String runtimeId) {
        try {
            String name = nameFor(runtimeId);
            Location location = head.location().clone().add(0,
                    head.hologram().offsetY(), 0);
            Class<?> dataClass = Class.forName(
                    "de.oliver.fancyholograms.api.data.TextHologramData");
            Object data = dataClass.getConstructor(String.class, Location.class)
                    .newInstance(name, location);
            invoke(data, "setPersistent", false);
            invoke(data, "setText", lines(head.hologram()));
            invoke(data, "setScale", new Vector3f((float) head.hologram().scale()));
            invoke(data, "setBillboard", Display.Billboard.CENTER);
            invoke(data, "setSeeThrough", head.hologram().seeThrough());
            invoke(data, "setTextShadow", head.hologram().shadow());
            invoke(data, "setTextUpdateInterval", head.hologram().refreshTicks());
            invoke(data, "setVisibilityDistance", head.displayRange());
            invoke(data, "setBrightness", new Display.Brightness(
                    head.hologram().brightness().block(),
                    head.hologram().brightness().sky()));
            invoke(data, "setBackground", background(head.hologram().background()));
            Object hologram = manager.getClass().getMethod("create",
                            Class.forName("de.oliver.fancyholograms.api.data.HologramData"))
                    .invoke(manager, data);
            manager.getClass().getMethod("addHologram",
                            Class.forName("de.oliver.fancyholograms.api.hologram.Hologram"))
                    .invoke(manager, hologram);
            return hologram;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("FancyHolograms API setup failed", exception);
        }
    }

    public void remove(Object hologram) {
        if (hologram == null) {
            return;
        }
        try {
            Class<?> hologramType = Class.forName(
                    "de.oliver.fancyholograms.api.hologram.Hologram");
            manager.getClass().getMethod("removeHologram", hologramType)
                    .invoke(manager, hologram);
            hologram.getClass().getMethod("deleteHologram").invoke(hologram);
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("Could not remove FancyHolograms hologram: "
                    + exception.getMessage());
        }
    }

    public static String nameFor(String runtimeId) {
        String safe = runtimeId.replaceAll("[^A-Za-z0-9_-]", "_");
        return "para_rotating_head_" + (safe.length() > 48
                ? safe.substring(0, 48) : safe);
    }

    private List<String> lines(Hologram hologram) {
        List<String> lines = new ArrayList<>(hologram.lines());
        if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            lines.replaceAll(line -> PlaceholderAPI.setPlaceholders(null, line));
        }
        return lines;
    }

    private Color background(String value) {
        if (value.equalsIgnoreCase("none")) {
            return null;
        }
        if (value.equalsIgnoreCase("default")) {
            return Color.fromARGB(0, 0, 0, 0);
        }
        try {
            String hex = value.startsWith("#") ? value.substring(1) : value;
            long color = Long.parseLong(hex, 16);
            if (hex.length() == 6) {
                return Color.fromRGB((int) (color >> 16) & 255,
                        (int) (color >> 8) & 255, (int) color & 255);
            }
            return Color.fromARGB((int) (color >> 24) & 255,
                    (int) (color >> 16) & 255, (int) (color >> 8) & 255,
                    (int) color & 255);
        } catch (NumberFormatException exception) {
            return Color.fromARGB(0, 0, 0, 0);
        }
    }

    private static void invoke(Object target, String name, Object value)
            throws ReflectiveOperationException {
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 1) {
                method.invoke(target, value);
                return;
            }
        }
        throw new NoSuchMethodException(name);
    }
}