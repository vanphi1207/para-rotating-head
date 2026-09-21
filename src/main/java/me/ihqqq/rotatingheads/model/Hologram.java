package me.ihqqq.rotatingheads.model;

import java.util.List;
import java.util.Locale;

public record Hologram(boolean enabled, double offsetY, int refreshTicks,
                       List<String> lines, double scale, boolean seeThrough,
                       boolean shadow, String background, Brightness brightness,
                       String provider, String link, boolean followHead) {
    public Hologram(boolean enabled, double offsetY, int refreshTicks,
                    List<String> lines, double scale, boolean seeThrough,
                    boolean shadow, String background, Brightness brightness) {
        this(enabled, offsetY, refreshTicks, lines, scale, seeThrough, shadow,
                background, brightness, "native");
    }

    public Hologram(boolean enabled, double offsetY, int refreshTicks,
                    List<String> lines, double scale, boolean seeThrough,
                    boolean shadow, String background, Brightness brightness,
                    String provider) {
        this(enabled, offsetY, refreshTicks, lines, scale, seeThrough, shadow,
                background, brightness, provider, "", true);
    }

    public Hologram {
        refreshTicks = Math.max(0, Math.min(72000, refreshTicks));
        scale = Math.max(0.05, Math.min(20, scale));
        lines = List.copyOf(lines == null ? List.of() : lines);
        background = background == null ? "default" : background;
        provider = provider == null ? "native" : provider.toLowerCase(Locale.ROOT);
        if (!provider.equals("native") && !provider.equals("fancyholograms")) {
            provider = "native";
        }
        link = link == null ? "" : link.trim();
    }

    public boolean linked() {
        return !link.isEmpty();
    }

    public Hologram withEnabled(boolean value) {
        return new Hologram(value, offsetY, refreshTicks, lines, scale, seeThrough,
                shadow, background, brightness, provider, link, followHead);
    }

    public Hologram withOffsetY(double value) {
        return new Hologram(enabled, value, refreshTicks, lines, scale, seeThrough,
                shadow, background, brightness, provider, link, followHead);
    }

    public Hologram withRefreshTicks(int value) {
        return new Hologram(enabled, offsetY, value, lines, scale, seeThrough,
                shadow, background, brightness, provider, link, followHead);
    }

    public Hologram withLines(List<String> value) {
        return new Hologram(enabled, offsetY, refreshTicks, value, scale, seeThrough,
                shadow, background, brightness, provider, link, followHead);
    }

    public Hologram withProvider(String value) {
        return new Hologram(enabled, offsetY, refreshTicks, lines, scale, seeThrough,
                shadow, background, brightness, value, link, followHead);
    }

    public Hologram withLink(String value) {
        return new Hologram(enabled, offsetY, refreshTicks, lines, scale, seeThrough,
                shadow, background, brightness, provider, value, followHead);
    }

    public Hologram withFollowHead(boolean value) {
        return new Hologram(enabled, offsetY, refreshTicks, lines, scale, seeThrough,
                shadow, background, brightness, provider, link, value);
    }
}
