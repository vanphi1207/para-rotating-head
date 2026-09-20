package me.ihqqq.rotatingheads.runtime;

import me.ihqqq.rotatingheads.model.HeadModels.Head;
import me.ihqqq.rotatingheads.model.HeadModels.Hologram;
import me.ihqqq.rotatingheads.hook.FancyHologramHook;
import me.ihqqq.rotatingheads.util.HeadUtil;
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

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HeadRuntime {
    private static final Pattern TEXTURE_URL =
            Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");

    private final JavaPlugin plugin;
    private final NamespacedKey entityKey;
    private final Map<String, Head> configured = new HashMap<>();
    private final Map<String, SpawnedHead> spawned = new HashMap<>();
    private final Map<UUID, String> interactions = new HashMap<>();
    private final Map<String, Set<String>> chunkIndex = new HashMap<>();
    private final Set<String> benchmarkIds = new HashSet<>();
    private final Map<String, Long> hologramTicks = new HashMap<>();
    private final BukkitTask rotationTask;
    private final FancyHologramHook fancyHolograms;
    private boolean fancyWarningLogged;

    public HeadRuntime(JavaPlugin plugin) {
        this.plugin = plugin;
        entityKey = new NamespacedKey(plugin, "head_id");
        fancyHolograms = FancyHologramHook.connect(plugin);
        rotationTask = plugin.getServer().getScheduler().runTaskTimer(plugin,
                this::rotateAll, 1L, 1L);
    }

    public void load(Map<String, Head> heads) {
        configured.clear();
        configured.putAll(heads);
        for (Head head : heads.values()) {
            index(head);
            if (head.location().getChunk().isLoaded()) {
                spawnConfigured(head);
            }
        }
    }

    public void register(Head head) {
        configured.put(head.id(), head);
        unindex(head.id());
        removeSpawned(head.id());
        index(head);
        if (head.location().getChunk().isLoaded()) {
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

    public void spawnBenchmark(Head source, Location center, int count) {
        clearBenchmarks();
        int side = (int) Math.ceil(Math.sqrt(count));
        double spacing = source.options().scale() * 1.5;
        for (int index = 0; index < count; index++) {
            Location location = center.clone().add((index % side) * spacing, 1,
                    (index / side) * spacing);
            String id = "benchmark-" + UUID.randomUUID();
            SpawnedHead instance = spawn(source.at(location), id);
            if (instance != null) {
                spawned.put(id, instance);
                benchmarkIds.add(id);
            }
        }
    }

    public void clearBenchmarks() {
        for (String id : List.copyOf(benchmarkIds)) {
            removeSpawned(id);
        }
        benchmarkIds.clear();
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
        clearBenchmarks();
        for (String id : List.copyOf(spawned.keySet())) {
            removeSpawned(id);
        }
        spawned.clear();
        interactions.clear();
        chunkIndex.clear();
        hologramTicks.clear();
    }

    public void reset() {
        clearBenchmarks();
        for (String id : List.copyOf(spawned.keySet())) {
            removeSpawned(id);
        }
        spawned.clear();
        interactions.clear();
        chunkIndex.clear();
        configured.clear();
        hologramTicks.clear();
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
            display.setItemStack(createSkull(head.options().texture()));
            display.setViewRange(head.displayRange() / 64.0f);
            display.setBrightness(new Display.Brightness(
                    head.options().brightness().block(), head.options().brightness().sky()));
            display.setPersistent(false);
            Transformation transformation = display.getTransformation();
            transformation.getScale().set((float) head.options().scale());
            display.setTransformation(transformation);
        });
        TextDisplay hologram = null;
        Object fancy = null;
        if (head.hologram().provider().equals("fancyholograms")
                && fancyHolograms != null) {
            try {
                fancy = fancyHolograms.create(head, id);
            } catch (RuntimeException exception) {
                warnFancyFallback(exception);
                hologram = spawnHologram(head);
            }
        } else {
            if (head.hologram().provider().equals("fancyholograms")) {
                warnFancyFallback(null);
            }
            hologram = spawnHologram(head);
        }
        Interaction interaction = spawnInteraction(head, id);
        if (interaction != null) {
            interactions.put(interaction.getUniqueId(), id);
        }
        return new SpawnedHead(id, head, item, hologram, fancy, interaction);
    }

    private void rotateAll() {
        for (SpawnedHead head : spawned.values()) {
            if (!head.item().isValid()) {
                continue;
            }
            long gameTime = head.item().getWorld().getGameTime();
            float angle = (float) ((gameTime * head.head().options().speed())
                    % 360.0) ;
            head.item().setRotation(angle, 0);
            int refresh = head.head().hologram().refreshTicks();
            if (head.hologram() != null && refresh > 0) {
                long tick = hologramTicks.merge(head.id(), 1L, Long::sum);
                if (tick % refresh == 0 && head.hologram().isValid()) {
                    head.hologram().text(renderText(head.head().hologram()));
                }
            }
        }
    }

    private TextDisplay spawnHologram(Head head) {
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
        });
    }

    private Component renderText(Hologram hologram) {
        String text = String.join("\n", hologram.lines());
        if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            text = PlaceholderAPI.setPlaceholders(null, text);
        }
        return MiniMessage.miniMessage().deserialize(text);
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

    private Interaction spawnInteraction(Head head, String id) {
        if (!head.interaction().enabled()) {
            return null;
        }
        Location location = head.location().clone().subtract(0, 1, 0);
        return location.getWorld().spawn(location, Interaction.class, entity -> {
            float size = (float) head.options().scale() * 2;
            entity.setInteractionWidth(size);
            entity.setInteractionHeight(size);
            entity.setResponsive(false);
            entity.setPersistent(false);
            entity.getPersistentDataContainer().set(entityKey, PersistentDataType.STRING, id);
        });
    }

    private ItemStack createSkull(String texture) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (texture == null || texture.isBlank()) {
            return item;
        }
        try {
            String json = new String(Base64.getDecoder().decode(texture),
                    StandardCharsets.UTF_8);
            Matcher matcher = TEXTURE_URL.matcher(json);
            if (!matcher.find()) {
                throw new IllegalArgumentException("texture has no URL");
            }
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            profile.getTextures().setSkin(new URL(matcher.group(1)));
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            meta.setOwnerProfile(profile);
            item.setItemMeta(meta);
        } catch (Exception exception) {
            plugin.getLogger().warning("Invalid texture ignored: " + exception.getMessage());
        }
        return item;
    }

    private void index(Head head) {
        Location location = head.location();
        String key = HeadUtil.chunkKey(location.getWorld().getName(),
                location.getChunk().getX(), location.getChunk().getZ());
        chunkIndex.computeIfAbsent(key, ignored -> new HashSet<>()).add(head.id());
    }

    private void unindex(String id) {
        for (Set<String> ids : chunkIndex.values()) {
            ids.remove(id);
        }
        chunkIndex.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    private void removeSpawned(String id) {
        SpawnedHead instance = spawned.remove(id);
        if (instance == null) {
            return;
        }
        if (instance.interaction() != null) {
            interactions.remove(instance.interaction().getUniqueId());
        }
        hologramTicks.remove(id);
        if (instance.fancy() != null && fancyHolograms != null) {
            fancyHolograms.remove(instance.fancy());
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

    private record SpawnedHead(String id, Head head, ItemDisplay item,
                               TextDisplay hologram,
                               Object fancy,
                               Interaction interaction) {
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