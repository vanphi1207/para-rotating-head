package me.ihqqq.rotatingheads.util;

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
            throw new IllegalArgumentException(
                    "id must contain only a-z, 0-9, _ or -");
        }
        return value;
    }

    public static int boundedInt(String value, int min, int max) {
        final int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("must be an integer");
        }
        if (parsed < min || parsed > max) {
            throw new IllegalArgumentException("must be between " + min + " and " + max);
        }
        return parsed;
    }

    public static double boundedDouble(String value, double min, double max) {
        final double parsed;
        try {
            parsed = Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("must be a number");
        }
        if (!Double.isFinite(parsed) || parsed < min || parsed > max) {
            throw new IllegalArgumentException("must be between " + min + " and " + max);
        }
        return parsed;
    }
}