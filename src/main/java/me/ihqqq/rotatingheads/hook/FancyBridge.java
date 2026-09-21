package me.ihqqq.rotatingheads.hook;

import me.ihqqq.rotatingheads.model.Hologram;
import org.bukkit.Location;

import java.util.Collection;


public interface FancyBridge {
    Object find(String name);

    Collection<String> names();

    Location locationOf(Object hologram);

    void move(Object hologram, Location location);

    Object create(String name, Location location, Hologram style, int visibilityDistance);

    void remove(Object hologram);
}