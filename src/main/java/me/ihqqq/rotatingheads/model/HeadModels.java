package me.ihqqq.rotatingheads.model;

import org.bukkit.Location;

import java.util.List;
import java.util.Locale;

public final class HeadModels {
    private HeadModels() {
    }

    public record Brightness(int sky, int block) {
        public Brightness {
            sky = Math.max(0, Math.min(15, sky));
            block = Math.max(0, Math.min(15, block));
        }
    }

    public record HeadOptions(String texture, double scale, double speed,
                              Brightness brightness, double speedX, double speedZ,
                              double bobHeight, int bobPeriod, String item) {
        public static final String DEFAULT_ITEM = "PLAYER_HEAD";

        public HeadOptions {
            texture = texture == null ? "" : texture.trim();
            scale = Math.max(0.05, Math.min(20, scale));
            speed = Math.max(-20, Math.min(20, speed));
            speedX = Math.max(-20, Math.min(20, speedX));
            speedZ = Math.max(-20, Math.min(20, speedZ));
            bobHeight = Math.max(0, Math.min(5, bobHeight));
            bobPeriod = Math.max(10, Math.min(1200, bobPeriod));
            item = item == null || item.isBlank()
                    ? DEFAULT_ITEM : item.trim().toUpperCase(Locale.ROOT);
        }

        public HeadOptions(String texture, double scale, double speed,
                           Brightness brightness) {
            this(texture, scale, speed, brightness, 0, 0, 0, 60, DEFAULT_ITEM);
        }

        public HeadOptions withTexture(String value) {
            return new HeadOptions(value, scale, speed, brightness, speedX, speedZ,
                    bobHeight, bobPeriod, item);
        }

        public HeadOptions withScale(double value) {
            return new HeadOptions(texture, value, speed, brightness, speedX, speedZ,
                    bobHeight, bobPeriod, item);
        }

        public HeadOptions withSpeed(double value) {
            return new HeadOptions(texture, scale, value, brightness, speedX, speedZ,
                    bobHeight, bobPeriod, item);
        }

        public HeadOptions withSpeedX(double value) {
            return new HeadOptions(texture, scale, speed, brightness, value, speedZ,
                    bobHeight, bobPeriod, item);
        }

        public HeadOptions withSpeedZ(double value) {
            return new HeadOptions(texture, scale, speed, brightness, speedX, value,
                    bobHeight, bobPeriod, item);
        }

        public HeadOptions withBrightness(Brightness value) {
            return new HeadOptions(texture, scale, speed, value, speedX, speedZ,
                    bobHeight, bobPeriod, item);
        }

        public HeadOptions withBobHeight(double value) {
            return new HeadOptions(texture, scale, speed, brightness, speedX, speedZ,
                    value, bobPeriod, item);
        }

        public HeadOptions withBobPeriod(int value) {
            return new HeadOptions(texture, scale, speed, brightness, speedX, speedZ,
                    bobHeight, value, item);
        }

        public HeadOptions withItem(String value) {
            return new HeadOptions(texture, scale, speed, brightness, speedX, speedZ,
                    bobHeight, bobPeriod, value);
        }
    }

    public record Interaction(boolean enabled, List<String> any, List<String> left,
                              List<String> right) {
        public Interaction {
            any = List.copyOf(any == null ? List.of() : any);
            left = List.copyOf(left == null ? List.of() : left);
            right = List.copyOf(right == null ? List.of() : right);
        }

        public Interaction withEnabled(boolean value) {
            return new Interaction(value, any, left, right);
        }
    }

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

    public record Head(String id, Location location, int displayRange,
                       HeadOptions options, Interaction interaction,
                       Hologram hologram) {
        public Head {
            location = location.clone();
            location.setYaw(0);
            location.setPitch(0);
            displayRange = Math.max(1, Math.min(256, displayRange));
        }

        public Head at(Location newLocation) {
            return new Head(id, newLocation, displayRange, options, interaction,
                    hologram);
        }

        public Head withId(String newId) {
            return new Head(newId, location, displayRange, options, interaction, hologram);
        }

        public Head withDisplayRange(int value) {
            return new Head(id, location, value, options, interaction, hologram);
        }

        public Head withOptions(HeadOptions value) {
            return new Head(id, location, displayRange, value, interaction, hologram);
        }

        public Head withInteraction(Interaction value) {
            return new Head(id, location, displayRange, options, value, hologram);
        }

        public Head withHologram(Hologram value) {
            return new Head(id, location, displayRange, options, interaction, value);
        }
    }
}