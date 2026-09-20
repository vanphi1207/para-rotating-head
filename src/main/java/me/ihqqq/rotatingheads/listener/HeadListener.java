package me.ihqqq.rotatingheads.listener;

import me.ihqqq.rotatingheads.config.HeadRepository;
import me.ihqqq.rotatingheads.config.SettingsHolder;
import me.ihqqq.rotatingheads.model.HeadModels.Head;
import me.ihqqq.rotatingheads.runtime.HeadRuntime;
import me.ihqqq.rotatingheads.util.ActionParser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class HeadListener implements Listener {
    private final HeadRuntime runtime;
    private final Map<String, Head> heads;
    private final HeadRepository repository;
    private final SettingsHolder settings;
    private final Plugin plugin;
    private final Map<UUID, Long> lastClick = new HashMap<>();

    public HeadListener(Plugin plugin, HeadRuntime runtime, Map<String, Head> heads,
                        HeadRepository repository, SettingsHolder settings) {
        this.plugin = plugin;
        this.runtime = runtime;
        this.heads = heads;
        this.repository = repository;
        this.settings = settings;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        runtime.onChunkLoad(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        runtime.onChunkUnload(event.getChunk());
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        for (Head head : repository.forWorld(event.getWorld().getName()).values()) {
            if (!heads.containsKey(head.id())) {
                heads.put(head.id(), head);
                runtime.register(head);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastClick.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onRightClick(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        String id = runtime.headForInteraction(event.getRightClicked().getUniqueId());
        if (id == null) {
            return;
        }
        event.setCancelled(true);
        dispatch(event.getPlayer(), heads.get(id), "right");
    }

    @EventHandler
    public void onLeftClick(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        String id = runtime.headForInteraction(event.getEntity().getUniqueId());
        if (id == null) {
            return;
        }
        event.setCancelled(true);
        dispatch(player, heads.get(id), "left");
    }

    private boolean onCooldown(Player player) {
        long cooldownMillis = settings.get().interactionCooldownMillis();
        if (cooldownMillis <= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long previous = lastClick.get(player.getUniqueId());
        if (previous != null && now - previous < cooldownMillis) {
            return true;
        }
        lastClick.put(player.getUniqueId(), now);
        return false;
    }

    private void dispatch(Player player, Head head, String side) {
        if (head == null || onCooldown(player)) {
            return;
        }
        List<String> actions = !head.interaction().any().isEmpty()
                ? head.interaction().any()
                : "left".equals(side)
                ? head.interaction().left() : head.interaction().right();
        ActionParser.execute(plugin, actions, player);
    }
}