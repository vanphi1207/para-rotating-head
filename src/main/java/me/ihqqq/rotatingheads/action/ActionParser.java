package me.ihqqq.rotatingheads.action;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public final class ActionParser {
    private ActionParser() {
    }

    public static void execute(Plugin plugin, List<String> actions, Player player) {
        for (String raw : actions) {
            ActionSyntax.Action action = ActionSyntax.parse(raw, player.getName());
            if (action == null) {
                continue;
            }
            if (action.permission() != null && !player.hasPermission(action.permission())) {
                continue;
            }
            if (action.delayTicks() > 0) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        run(action, player);
                    }
                }, action.delayTicks());
            } else {
                run(action, player);
            }
        }
    }

    private static void run(ActionSyntax.Action action, Player player) {
        String payload = placeholders(player, action.payload());
        switch (action.type()) {
            case CONSOLE -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), payload);
            case PLAYER -> player.performCommand(payload);
            case MESSAGE -> player.sendMessage(MiniMessage.miniMessage().deserialize(payload));
            case SOUND -> playSound(player, payload);
        }
    }

    private static void playSound(Player player, String spec) {
        String[] parts = spec.split(":");
        int keyParts = parts.length > 1 && !isNumber(parts[1]) ? 2 : 1;
        StringBuilder key = new StringBuilder(parts[0]);
        for (int i = 1; i < keyParts; i++) {
            key.append(':').append(parts[i]);
        }
        float volume = keyParts < parts.length ? number(parts[keyParts], 1.0f) : 1.0f;
        float pitch = keyParts + 1 < parts.length ? number(parts[keyParts + 1], 1.0f) : 1.0f;
        player.playSound(player.getLocation(), key.toString(), volume, pitch);
    }

    private static boolean isNumber(String value) {
        try {
            Float.parseFloat(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static float number(String value, float fallback) {
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static String placeholders(Player player, String text) {
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")
                ? Papi.apply(player, text) : text;
    }

    private static final class Papi {
        static String apply(Player player, String text) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }
    }
}