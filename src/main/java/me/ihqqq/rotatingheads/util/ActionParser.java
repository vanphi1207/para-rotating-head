package me.ihqqq.rotatingheads.util;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class ActionParser {
    private ActionParser() {
    }

    public static List<String> commands(List<String> actions, Player player) {
        return commands(actions, player.getName());
    }

    public static List<String> commands(List<String> actions, String playerName) {
        List<String> commands = new ArrayList<>();
        for (String action : actions) {
            if (action == null || action.isBlank()) {
                continue;
            }
            commands.add(action.replace("%player%", playerName));
        }
        return commands;
    }

    public static void execute(List<String> actions, Player player) {
        for (String command : commands(actions, player)) {
            if (command.regionMatches(true, 0, "[player]", 0, 8)) {
                player.performCommand(command.substring(8).trim());
            } else if (command.regionMatches(true, 0, "[console]", 0, 9)) {
                dispatch(Bukkit.getConsoleSender(), command.substring(9).trim());
            } else {
                dispatch(Bukkit.getConsoleSender(), command);
            }
        }
    }

    private static void dispatch(CommandSender sender, String command) {
        if (!command.isBlank()) {
            Bukkit.dispatchCommand(sender, command);
        }
    }
}