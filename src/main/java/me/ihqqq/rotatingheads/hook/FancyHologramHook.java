package me.ihqqq.rotatingheads.hook;

import me.ihqqq.rotatingheads.model.HeadModels.Head;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public final class FancyHologramHook {
    public static final String OWNED_PREFIX = "para_rotating_head_";
    private static final double MOVE_EPSILON = 1.0e-4;

    public static final class Link {
        private final String name;
        private final boolean owned;
        private Object hologram;

        private Link(String name, boolean owned, Object hologram) {
            this.name = name;
            this.owned = owned;
            this.hologram = hologram;
        }

        public String name() {
            return name;
        }

        public boolean owned() {
            return owned;
        }

        public boolean resolved() {
            return hologram != null;
        }
    }

    private final FancyBridge bridge;
    private final Logger logger;

    public FancyHologramHook(FancyBridge bridge, Logger logger) {
        this.bridge = bridge;
        this.logger = logger;
    }

    public static FancyHologramHook connect(JavaPlugin plugin) {
        FancyBridge bridge = ReflectiveFancyBridge.connect(plugin);
        return bridge == null ? null : new FancyHologramHook(bridge, plugin.getLogger());
    }

    public Link attach(Head head, String runtimeId) {
        if (head.hologram().linked()) {
            Link link = new Link(head.hologram().link(), false, null);
            resolve(head, link);
            return link;
        }
        String name = nameFor(runtimeId);
        Object hologram = bridge.create(name, target(head), head.hologram(),
                head.displayRange());
        return new Link(name, true, hologram);
    }

    public boolean tick(Head head, Link link) {
        if (link.owned) {
            return false;
        }
        if (link.hologram != null) {
            if (bridge.find(link.name) == link.hologram) {
                return false;
            }
            link.hologram = null;
            logger.info("FancyHolograms hologram '" + link.name
                    + "' linked to head " + head.id() + " is gone; waiting for it.");
            return true;
        }
        return resolve(head, link);
    }

    public void release(Link link) {
        if (link == null || !link.owned || link.hologram == null) {
            return;
        }
        try {
            bridge.remove(link.hologram);
        } catch (RuntimeException exception) {
            logger.warning("Could not remove FancyHolograms hologram " + link.name + ": "
                    + exception.getMessage());
        }
        link.hologram = null;
    }

    public List<String> linkableNames() {
        Collection<String> all = bridge.names();
        List<String> result = new ArrayList<>();
        for (String name : all) {
            if (!name.toLowerCase(Locale.ROOT).startsWith(OWNED_PREFIX)) {
                result.add(name);
            }
        }
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    public static String nameFor(String runtimeId) {
        String safe = runtimeId.replaceAll("[^A-Za-z0-9_-]", "_");
        return OWNED_PREFIX + (safe.length() > 48 ? safe.substring(0, 48) : safe);
    }

    private boolean resolve(Head head, Link link) {
        Object hologram = bridge.find(link.name);
        if (hologram == null) {
            return false;
        }
        link.hologram = hologram;
        if (head.hologram().followHead()) {
            Location wanted = target(head);
            if (!sameLocation(bridge.locationOf(hologram), wanted)) {
                bridge.move(hologram, wanted);
            }
        }
        return true;
    }

    private static Location target(Head head) {
        return head.location().clone().add(0, head.hologram().offsetY(), 0);
    }

    private static boolean sameLocation(Location a, Location b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getWorld() == null ? b.getWorld() != null : !a.getWorld().equals(b.getWorld())) {
            return false;
        }
        return Math.abs(a.getX() - b.getX()) < MOVE_EPSILON
                && Math.abs(a.getY() - b.getY()) < MOVE_EPSILON
                && Math.abs(a.getZ() - b.getZ()) < MOVE_EPSILON;
    }
}