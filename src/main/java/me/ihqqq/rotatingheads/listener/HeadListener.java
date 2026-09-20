package me.ihqqq.rotatingheads.listener;

import me.ihqqq.rotatingheads.model.HeadModels.Head;
import me.ihqqq.rotatingheads.runtime.HeadRuntime;
import me.ihqqq.rotatingheads.util.ActionParser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;
import java.util.Map;

public final class HeadListener implements Listener {
    private final HeadRuntime runtime;
    private final Map<String, Head> heads;

    public HeadListener(HeadRuntime runtime, Map<String, Head> heads) {
        this.runtime = runtime;
        this.heads = heads;
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

    private void dispatch(Player player, Head head, String side) {
        if (head == null) {
            return;
        }
        List<String> actions = !head.interaction().any().isEmpty()
                ? head.interaction().any()
                : "left".equals(side)
                ? head.interaction().left() : head.interaction().right();
        ActionParser.execute(actions, player);
    }
}