package me.ihqqq.rotatingheads.util;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

public final class BedrockDetector {
    private final Plugin plugin;
    private Object floodgateApi;
    private Method floodgateCheck;
    private Object geyserApi;
    private Method geyserCheck;

    public BedrockDetector(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean bridgePresent() {
        var manager = plugin.getServer().getPluginManager();
        return manager.isPluginEnabled("Geyser-Spigot") || manager.isPluginEnabled("floodgate");
    }

    public boolean isBedrock(Player player) {
        UUID uuid = player.getUniqueId();
        if (uuid.getMostSignificantBits() == 0L) {
            return true;
        }
        return viaFloodgate(uuid) || viaGeyser(uuid);
    }

    private boolean viaFloodgate(UUID uuid) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("floodgate")) {
            return false;
        }
        try {
            if (floodgateApi == null) {
                Class<?> type = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                floodgateApi = type.getMethod("getInstance").invoke(null);
                floodgateCheck = type.getMethod("isFloodgatePlayer", UUID.class);
            }
            return Boolean.TRUE.equals(floodgateCheck.invoke(floodgateApi, uuid));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return false;
        }
    }

    private boolean viaGeyser(UUID uuid) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("Geyser-Spigot")) {
            return false;
        }
        try {
            if (geyserApi == null) {
                Class<?> type = Class.forName("org.geysermc.geyser.api.GeyserApi");
                geyserApi = type.getMethod("api").invoke(null);
                geyserCheck = type.getMethod("isBedrockPlayer", UUID.class);
            }
            return Boolean.TRUE.equals(geyserCheck.invoke(geyserApi, uuid));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return false;
        }
    }
}