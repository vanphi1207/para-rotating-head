package me.ihqqq.rotatingheads.util;

import java.util.Locale;

public final class ActionSyntax {
    public static final int MAX_DELAY_TICKS = 72000;

    public enum Type { CONSOLE, PLAYER, MESSAGE, SOUND }

    public record Action(Type type, String payload, int delayTicks, String permission) {
    }

    private ActionSyntax() {
    }

    public static Action parse(String raw, String playerName) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.trim().replace("%player%", playerName);
        Type type = Type.CONSOLE;
        int delay = 0;
        String permission = null;
        while (text.startsWith("[")) {
            int end = text.indexOf(']');
            if (end < 0) {
                break;
            }
            String tag = text.substring(1, end).trim();
            String lower = tag.toLowerCase(Locale.ROOT);
            if (lower.equals("console")) {
                type = Type.CONSOLE;
            } else if (lower.equals("player")) {
                type = Type.PLAYER;
            } else if (lower.equals("message")) {
                type = Type.MESSAGE;
            } else if (lower.equals("sound")) {
                type = Type.SOUND;
            } else if (lower.startsWith("delay:")) {
                Integer parsed = parseDelay(tag.substring(6));
                if (parsed == null) {
                    break;
                }
                delay = parsed;
            } else if (lower.startsWith("permission:") && tag.length() > 11) {
                permission = tag.substring(11).trim();
            } else {
                break;
            }
            text = text.substring(end + 1).trim();
        }
        if ((type == Type.CONSOLE || type == Type.PLAYER) && text.startsWith("/")) {
            text = text.substring(1).trim();
        }
        if (text.isEmpty()) {
            return null;
        }
        return new Action(type, text, delay, permission);
    }

    private static Integer parseDelay(String value) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed < 0 ? null : Math.min(parsed, MAX_DELAY_TICKS);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}