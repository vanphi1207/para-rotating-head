package me.ihqqq.rotatingheads.model;

import org.bukkit.Location;

import java.util.List;

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
                              Brightness brightness) {
        public HeadOptions {
            texture = texture == null ? "" : texture.trim();
            scale = Math.max(0.05, Math.min(20, scale));
            speed = Math.max(-20, Math.min(20, speed));
        }
    }

    public record Interaction(boolean enabled, List<String> any, List<String> left,
                              List<String> right) {
        public Interaction {
            any = List.copyOf(any == null ? List.of() : any);
            left = List.copyOf(left == null ? List.of() : left);
            right = List.copyOf(right == null ? List.of() : right);
        }
    }

    public record Hologram(boolean enabled, double offsetY, int refreshTicks,
                           List<String> lines, double scale, boolean seeThrough,
                           boolean shadow, String background, Brightness brightness,
                           String provider) {
        public Hologram(boolean enabled, double offsetY, int refreshTicks,
                        List<String> lines, double scale, boolean seeThrough,
                        boolean shadow, String background, Brightness brightness) {
            this(enabled, offsetY, refreshTicks, lines, scale, seeThrough, shadow,
                    background, brightness, "native");
        }

        public Hologram {
            refreshTicks = Math.max(0, Math.min(72000, refreshTicks));
            scale = Math.max(0.05, Math.min(20, scale));
            lines = List.copyOf(lines == null ? List.of() : lines);
            background = background == null ? "default" : background;
            provider = provider == null ? "native" : provider.toLowerCase();
            if (!provider.equals("native") && !provider.equals("fancyholograms")) {
                provider = "native";
            }
        }
    }

    public record Head(String id, Location location, int displayRange,
                       HeadOptions options, Interaction interaction,
                       Hologram hologram) {
        public Head {
            location = location.clone();
            displayRange = Math.max(1, Math.min(256, displayRange));
        }

        public Head at(Location newLocation) {
            return new Head(id, newLocation, displayRange, options, interaction,
                    hologram);
        }
    }
}