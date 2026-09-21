package me.ihqqq.rotatingheads.util;

import me.ihqqq.rotatingheads.exception.MessageException;

import org.bukkit.Location;

import java.util.Locale;

public final class HeadUtil {
    private HeadUtil() {
    }

    public static String chunkKey(String world, int chunkX, int chunkZ) {
        return world + ":" + chunkX + ":" + chunkZ;
    }

    public static String normalizeId(String id) {
        String value = id.toLowerCase(Locale.ROOT);
        if (!value.matches("[a-z0-9_-]{1,32}")) {
            throw new MessageException("validation.id");
        }
        return value;
    }

    public static String linkName(String value) {
        String name = value.trim();
        if (name.equalsIgnoreCase("none") || name.equalsIgnoreCase("off")
                || name.equals("-")) {
            return "";
        }
        if (!name.matches("[A-Za-z0-9_.-]{1,64}")) {
            throw new MessageException("validation.link-name");
        }
        return name;
    }

    public static int boundedInt(String value, int min, int max) {
        final int parsed;
        try {
            parsed = Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new MessageException("validation.integer");
        }
        if (parsed < min || parsed > max) {
            throw new MessageException("validation.range", "min", String.valueOf(min),
                    "max", String.valueOf(max));
        }
        return parsed;
    }

    public static double boundedDouble(String value, double min, double max) {
        final double parsed;
        try {
            parsed = Double.parseDouble(value.trim());
        } catch (NumberFormatException exception) {
            throw new MessageException("validation.number");
        }
        if (!Double.isFinite(parsed) || parsed < min || parsed > max) {
            throw new MessageException("validation.range", "min", format(min),
                    "max", format(max));
        }
        return parsed;
    }

    public static boolean parseBoolean(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new MessageException("validation.boolean");
    }

    public static int[] parseBrightness(String value) {
        String[] parts = value.split("[/:,]");
        if (parts.length != 2) {
            throw new MessageException("validation.brightness");
        }
        return new int[] {boundedInt(parts[0], 0, 15), boundedInt(parts[1], 0, 15)};
    }

    public static int lineIndex(String value, int size) {
        if (size == 0) {
            throw new MessageException("validation.no-lines");
        }
        return boundedInt(value, 1, size) - 1;
    }

    public static Location blockCenter(Location location) {
        return new Location(location.getWorld(),
                Math.floor(location.getX()) + 0.5,
                Math.floor(location.getY()) + 0.5,
                Math.floor(location.getZ()) + 0.5);
    }

    public static String format(double value) {
        return value == Math.rint(value) && Math.abs(value) < 1.0e15
                ? String.valueOf((long) value) : String.valueOf(value);
    }
}