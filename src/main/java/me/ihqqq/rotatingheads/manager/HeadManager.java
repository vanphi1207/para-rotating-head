package me.ihqqq.rotatingheads.manager;

import me.ihqqq.rotatingheads.model.Head;
import me.ihqqq.rotatingheads.model.Hologram;
import me.ihqqq.rotatingheads.hook.FancyHologramHook;
import me.ihqqq.rotatingheads.model.HeadOptions;
import me.ihqqq.rotatingheads.util.HeadUtil;
import me.ihqqq.rotatingheads.exception.MessageException;
import me.ihqqq.rotatingheads.util.TextureUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class HeadManager {
    private static final double MAX_STEP_DEGREES = 60.0;
    private static final int LINK_CHECK_TICKS = 100;
    private static final int MAX_INTERVAL_TICKS = 40;
    private static final double HEAD_HALF_WIDTH = 0.25;
    private static final double HEAD_BOTTOM = -0.5;
    private static final double HEAD_TOP = 0.0;
    private static final double HEAD_CENTER_Y = (HEAD_TOP + HEAD_BOTTOM) / 2.0;
    private static final double CORNER_RADIUS = Math.sqrt(
            2 * HEAD_HALF_WIDTH * HEAD_HALF_WIDTH + HEAD_BOTTOM * HEAD_BOTTOM);
    private static final double FACE_DIAGONAL = Math.sqrt(2);
    private static final double MIN_INTERACTION_SIZE = 0.25;

    private final JavaPlugin plugin;
    private final NamespacedKey entityKey;
    private final Map<String, Head> configured = new HashMap<>();
    private final Map<String, SpawnedHead> spawned = new HashMap<>();
    private final Map<UUID, String> interactions = new HashMap<>();
    private final Map<String, Set<String>> chunkIndex = new HashMap<>();
    private final Map<String, String> headChunk = new HashMap<>();
    private final Map<String, ItemStack> skullCache = new HashMap<>();
    private long tick;
    private final BukkitTask rotationTask;
    private final FancyHologramHook fancyHolograms;
    private boolean fancyWarningLogged;
    private boolean linkWarningLogged;
    private boolean linkErrorLogged;
    private boolean hologramWarningLogged;

    public HeadManager(JavaPlugin plugin) {
        this.plugin = plugin;
        entityKey = new NamespacedKey(plugin, "head_id");
        fancyHolograms = FancyHologramHook.connect(plugin);
        rotationTask = plugin.getServer().getScheduler().runTaskTimer(plugin,
                this::rotateAll, 1L, 1L);
    }

    public void load(Map<String, Head> heads) {
        for (String id : List.copyOf(spawned.keySet())) {
            removeSpawned(id);
        }
        spawned.clear();
        interactions.clear();
        chunkIndex.clear();
        headChunk.clear();
        skullCache.clear();
        configured.clear();
        configured.putAll(heads);
        for (Head head : heads.values()) {
            index(head);
            if (isChunkLoaded(head.location())) {
                spawnConfigured(head);
            }
        }
    }

    public void register(Head head) {
        configured.put(head.id(), head);
        unindex(head.id());
        removeSpawned(head.id());
        index(head);
        if (isChunkLoaded(head.location())) {
            spawnConfigured(head);
        }
    }

    public void remove(String id) {
        Head old = configured.remove(id);
        if (old != null) {
            unindex(id);
        }
        removeSpawned(id);
    }

    public void onChunkLoad(org.bukkit.Chunk chunk) {
        Set<String> ids = chunkIndex.get(HeadUtil.chunkKey(chunk.getWorld().getName(),
                chunk.getX(), chunk.getZ()));
        if (ids != null) {
            for (String id : List.copyOf(ids)) {
                Head head = configured.get(id);
                if (head != null) {
                    spawnConfigured(head);
                }
            }
        }
    }

    public void onChunkUnload(org.bukkit.Chunk chunk) {
        Set<String> ids = chunkIndex.get(HeadUtil.chunkKey(chunk.getWorld().getName(),
                chunk.getX(), chunk.getZ()));
        if (ids != null) {
            for (String id : List.copyOf(ids)) {
                removeSpawned(id);
            }
        }
    }

    public String headForInteraction(UUID uuid) {
        return interactions.get(uuid);
    }

    public Collection<String> activeIds() {
        return List.copyOf(configured.keySet());
    }

    public void cleanupOrphans() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getPersistentDataContainer().has(entityKey,
                        PersistentDataType.STRING)) {
                    entity.remove();
                }
            }
        }
    }

    public void close() {
        rotationTask.cancel();
        for (String id : List.copyOf(spawned.keySet())) {
            removeSpawned(id);
        }
        spawned.clear();
        interactions.clear();
        chunkIndex.clear();
        headChunk.clear();
        skullCache.clear();
    }

    public void reset() {
        for (String id : List.copyOf(spawned.keySet())) {
            removeSpawned(id);
        }
        spawned.clear();
        interactions.clear();
        chunkIndex.clear();
        headChunk.clear();
        skullCache.clear();
        configured.clear();
    }

    private void spawnConfigured(Head head) {
        if (!spawned.containsKey(head.id())) {
            SpawnedHead instance = spawn(head, head.id());
            if (instance != null) {
                spawned.put(head.id(), instance);
            }
        }
    }

    private SpawnedHead spawn(Head head, String id) {
        World world = head.location().getWorld();
        if (world == null) {
            return null;
        }
        ItemDisplay item = world.spawn(head.location(), ItemDisplay.class, display -> {
            display.setItemStack(createSkull(head.options()));
            display.getPersistentDataContainer().set(entityKey, PersistentDataType.STRING, id);
            display.setViewRange(head.displayRange() / 64.0f);
            display.setBrightness(new Display.Brightness(
                    head.options().brightness().block(), head.options().brightness().sky()));
            display.setPersistent(false);
            Transformation transformation = display.getTransformation();
            transformation.getScale().set((float) head.options().scale());
            applyPose(transformation, head.options(), world.getGameTime());
            display.setInterpolationDuration(0);
            display.setTransformation(transformation);
        });
        TextDisplay hologram = null;
        FancyHologramHook.Link fancy = null;
        boolean wantsHologram = head.hologram().enabled()
                && (head.hologram().linked() || !head.hologram().lines().isEmpty());
        if (!wantsHologram) {
        } else if (head.hologram().linked()) {
            fancy = attachLinked(head, id);
        } else if (head.hologram().provider().equals("fancyholograms")) {
            if (fancyHolograms != null) {
                try {
                    fancy = fancyHolograms.attach(head, id);
                } catch (RuntimeException exception) {
                    warnFancyFallback(exception);
                    hologram = spawnHologram(head, id);
                }
            } else {
                warnFancyFallback(null);
                hologram = spawnHologram(head, id);
            }
        } else {
            hologram = spawnHologram(head, id);
        }
        Interaction interaction = spawnInteraction(head, id);
        if (interaction != null) {
            interactions.put(interaction.getUniqueId(), id);
        }
        int interval = updateInterval(head.options());
        int refresh = head.hologram().refreshTicks();
        Schedule schedule = new Schedule(interval, refresh,
                interval > 0 ? Math.floorMod(id.hashCode(), interval) : 0,
                refresh > 0 ? Math.floorMod(id.hashCode(), refresh) : 0,
                tick);
        return new SpawnedHead(id, head, item, hologram, fancy, interaction, schedule);
    }

    private FancyHologramHook.Link attachLinked(Head head, String id) {
        if (fancyHolograms == null) {
            warnLinkUnavailable(head, null);
            return null;
        }
        try {
            return fancyHolograms.attach(head, id);
        } catch (RuntimeException exception) {
            warnLinkUnavailable(head, exception);
            return null;
        }
    }

    private void tickLink(SpawnedHead head) {
        try {
            fancyHolograms.tick(head.head(), head.fancy());
        } catch (RuntimeException exception) {
            if (!linkErrorLogged) {
                linkErrorLogged = true;
                plugin.getLogger().warning("Could not check the FancyHolograms link of head "
                        + head.id() + ": " + exception.getMessage());
            }
        }
    }

    private void warnLinkUnavailable(Head head, RuntimeException exception) {
        if (!linkWarningLogged) {
            linkWarningLogged = true;
            plugin.getLogger().warning("Head " + head.id() + " is linked to FancyHolograms "
                    + "hologram '" + head.hologram().link() + "' but FancyHolograms is not "
                    + "usable" + (exception == null ? "." : ": " + exception.getMessage()));
        }
    }

    public boolean fancyAvailable() {
        return fancyHolograms != null;
    }

    public boolean linkActive(String id) {
        SpawnedHead instance = spawned.get(id);
        return instance != null && instance.fancy() != null && instance.fancy().resolved();
    }

    public List<String> linkableHolograms() {
        if (fancyHolograms == null) {
            return List.of();
        }
        try {
            return fancyHolograms.linkableNames();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private void rotateAll() {
        tick++;
        for (String id : List.copyOf(spawned.keySet())) {
            SpawnedHead head = spawned.get(id);
            if (head == null) {
                continue;
            }
            ItemDisplay item = head.item();
            if (!item.isValid()
                    || (head.hologram() != null && !head.hologram().isValid())
                    || (head.interaction() != null && !head.interaction().isValid())) {
                removeSpawned(id);
                Head configuredHead = configured.get(id);
                if (configuredHead != null && isChunkLoaded(configuredHead.location())) {
                    spawnConfigured(configuredHead);
                }
                continue;
            }
            Schedule schedule = head.schedule();
            if (schedule.interval > 0 && tick >= schedule.nextRotation) {
                schedule.nextRotation = tick + schedule.interval;
                long target = item.getWorld().getGameTime() + schedule.interval;
                Transformation transformation = item.getTransformation();
                applyPose(transformation, head.head().options(), target);
                item.setInterpolationDelay(0);
                item.setInterpolationDuration(schedule.interval);
                item.setTransformation(transformation);
            }
            if (head.fancy() != null && !head.fancy().owned()
                    && tick % LINK_CHECK_TICKS == 0) {
                tickLink(head);
            }
            if (head.hologram() != null && schedule.refresh > 0
                    && (tick + schedule.refreshPhase) % schedule.refresh == 0
                    && head.hologram().isValid()) {
                head.hologram().text(renderText(head.head().hologram()));
            }
        }
    }

    private static void applyPose(Transformation transformation, HeadOptions options,
                                  long gameTime) {
        transformation.getLeftRotation().rotationYXZ(
                rotationRadians(options.speed(), gameTime),
                rotationRadians(options.speedX(), gameTime),
                rotationRadians(options.speedZ(), gameTime));
        double bob = 0;
        if (options.bobHeight() > 0) {
            double phase = (gameTime % options.bobPeriod()) / (double) options.bobPeriod();
            bob = Math.sin(phase * 2 * Math.PI) * options.bobHeight();
        }
        transformation.getTranslation().set(0f, (float) bob, 0f);
    }

    private static float rotationRadians(double speed, long gameTime) {
        double degrees = (speed * gameTime) % 720.0;
        return (float) -Math.toRadians(degrees);
    }

    static int updateInterval(HeadOptions options) {
        double total = Math.abs(options.speed()) + Math.abs(options.speedX())
                + Math.abs(options.speedZ());
        int rotation = total < 1.0e-6 ? 0 : Math.max(1, Math.min(MAX_INTERVAL_TICKS,
                (int) Math.floor(MAX_STEP_DEGREES / total)));
        int bob = options.bobHeight() > 0 ? Math.max(2, options.bobPeriod() / 8) : 0;
        if (rotation == 0) {
            return bob;
        }
        return bob == 0 ? rotation : Math.min(rotation, bob);
    }

    private TextDisplay spawnHologram(Head head, String id) {
        Hologram config = head.hologram();
        if (!config.enabled() || config.lines().isEmpty()) {
            return null;
        }
        Location location = head.location().clone().add(0, config.offsetY(), 0);
        return location.getWorld().spawn(location, TextDisplay.class, display -> {
            display.text(renderText(config));
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(config.seeThrough());
            display.setShadowed(config.shadow());
            applyBackground(display, config.background());
            display.setBrightness(new Display.Brightness(config.brightness().block(),
                    config.brightness().sky()));
            Transformation transformation = display.getTransformation();
            transformation.getScale().set((float) config.scale());
            display.setTransformation(transformation);
            display.setPersistent(false);
            display.getPersistentDataContainer().set(entityKey, PersistentDataType.STRING, id);
        });
    }

    private Component renderText(Hologram hologram) {
        String text = String.join("\n", hologram.lines());
        try {
            if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                text = PlaceholderAPI.setPlaceholders(null, text);
            }
            return MiniMessage.miniMessage().deserialize(text);
        } catch (RuntimeException exception) {
            if (!hologramWarningLogged) {
                hologramWarningLogged = true;
                plugin.getLogger().warning("Could not render a hologram line; displaying its "
                        + "literal text instead: " + exception.getMessage());
            }
            return Component.text(text);
        }
    }

    private void applyBackground(TextDisplay display, String value) {
        if ("default".equalsIgnoreCase(value)) {
            display.setDefaultBackground(true);
            return;
        }
        display.setDefaultBackground(false);
        if ("none".equalsIgnoreCase(value)) {
            display.setBackgroundColor(null);
            return;
        }
        try {
            String hex = value.startsWith("#") ? value.substring(1) : value;
            long color = Long.parseLong(hex, 16);
            if (hex.length() == 6) {
                display.setBackgroundColor(Color.fromRGB((int) (color >> 16) & 255,
                        (int) (color >> 8) & 255, (int) color & 255));
            } else if (hex.length() == 8) {
                display.setBackgroundColor(Color.fromARGB((int) (color >> 24) & 255,
                        (int) (color >> 16) & 255, (int) (color >> 8) & 255,
                        (int) color & 255));
            } else {
                display.setDefaultBackground(true);
            }
        } catch (NumberFormatException exception) {
            display.setDefaultBackground(true);
        }
    }

    public static double renderOffsetY(HeadOptions options) {
        return -HEAD_CENTER_Y * options.scale();
    }

    private Interaction spawnInteraction(Head head, String id) {
        if (!head.interaction().enabled()) {
            return null;
        }
        HeadOptions options = head.options();
        double scale = options.scale();
        double halfWidth = HEAD_HALF_WIDTH * scale;
        double bottom = HEAD_BOTTOM * scale;
        double top = HEAD_TOP * scale;
        if (options.speedX() != 0 || options.speedZ() != 0) {
            double radius = CORNER_RADIUS * scale;
            halfWidth = radius;
            bottom = -radius;
            top = radius;
        } else if (options.speed() != 0) {
            halfWidth *= FACE_DIAGONAL;
        }
        bottom -= options.bobHeight();
        top += options.bobHeight();

        float boxWidth = (float) Math.max(MIN_INTERACTION_SIZE, 2 * halfWidth);
        float boxHeight = (float) Math.max(MIN_INTERACTION_SIZE, top - bottom);
        double centerY = (top + bottom) / 2.0;
        Location location = head.location().clone().add(0, centerY - boxHeight / 2.0, 0);
        return location.getWorld().spawn(location, Interaction.class, entity -> {
            entity.setInteractionWidth(boxWidth);
            entity.setInteractionHeight(boxHeight);
            entity.setResponsive(false);
            entity.setPersistent(false);
            entity.getPersistentDataContainer().set(entityKey, PersistentDataType.STRING, id);
        });
    }

    private ItemStack createSkull(HeadOptions options) {
        ItemStack cached = skullCache.get(options.texture());
        if (cached == null) {
            cached = buildSkull(options);
            skullCache.put(options.texture(), cached);
        }
        return cached.clone();
    }

    private ItemStack buildSkull(HeadOptions options) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (options.texture().isBlank()) {
            return item;
        }
        try {
            String url = TextureUtil.urlFromBase64(TextureUtil.normalize(options.texture()));
            if (url == null) {
                throw new IllegalArgumentException("texture has no valid skin URL");
            }
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            profile.getTextures().setSkin(URI.create(url).toURL());
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            meta.setOwnerProfile(profile);
            item.setItemMeta(meta);
        } catch (MessageException exception) {
            plugin.getLogger().warning("Invalid texture ignored: it must be a Base64 value, "
                    + "a textures.minecraft.net URL or a texture hash.");
        } catch (Exception exception) {
            plugin.getLogger().warning("Invalid texture ignored: " + exception.getMessage());
        }
        return item;
    }

    private static boolean isChunkLoaded(Location location) {
        World world = location.getWorld();
        return world != null
                && world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    private void index(Head head) {
        Location location = head.location();
        String key = HeadUtil.chunkKey(location.getWorld().getName(),
                location.getBlockX() >> 4, location.getBlockZ() >> 4);
        chunkIndex.computeIfAbsent(key, ignored -> new HashSet<>()).add(head.id());
        headChunk.put(head.id(), key);
    }

    private void unindex(String id) {
        String key = headChunk.remove(id);
        if (key == null) {
            return;
        }
        Set<String> ids = chunkIndex.get(key);
        if (ids != null) {
            ids.remove(id);
            if (ids.isEmpty()) {
                chunkIndex.remove(key);
            }
        }
    }

    private void removeSpawned(String id) {
        SpawnedHead instance = spawned.remove(id);
        if (instance == null) {
            return;
        }
        if (instance.interaction() != null) {
            interactions.remove(instance.interaction().getUniqueId());
        }
        if (instance.fancy() != null && fancyHolograms != null) {
            fancyHolograms.release(instance.fancy());
        }
        instance.remove();
    }

    private void warnFancyFallback(RuntimeException exception) {
        if (!fancyWarningLogged) {
            fancyWarningLogged = true;
            plugin.getLogger().warning("FancyHolograms unavailable; using native holograms."
                    + (exception == null ? "" : " " + exception.getMessage()));
        }
    }

    private static final class Schedule {
        private final int interval;
        private final int refresh;
        private final int refreshPhase;
        private long nextRotation;

        private Schedule(int interval, int refresh, int rotationPhase, int refreshPhase,
                         long now) {
            this.interval = interval;
            this.refresh = refresh;
            this.refreshPhase = refreshPhase;
            this.nextRotation = now + rotationPhase;
        }
    }

    private record SpawnedHead(String id, Head head, ItemDisplay item,
                               TextDisplay hologram,
                               FancyHologramHook.Link fancy,
                               Interaction interaction,
                               Schedule schedule) {
        private void remove() {
            if (item.isValid()) {
                item.remove();
            }
            if (hologram != null && hologram.isValid()) {
                hologram.remove();
            }
            if (interaction != null && interaction.isValid()) {
                interaction.remove();
            }
        }
    }
}