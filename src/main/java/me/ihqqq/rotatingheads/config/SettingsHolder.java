package me.ihqqq.rotatingheads.config;

public final class SettingsHolder {
    private final SettingsLoader loader;
    private volatile Settings current;

    public SettingsHolder(SettingsLoader loader) {
        this.loader = loader;
        this.current = loader.load();
    }

    public Settings get() {
        return current;
    }

    public void reload() {
        current = loader.load();
    }
}