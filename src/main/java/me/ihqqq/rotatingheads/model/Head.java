package me.ihqqq.rotatingheads.model;

import org.bukkit.Location;

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
