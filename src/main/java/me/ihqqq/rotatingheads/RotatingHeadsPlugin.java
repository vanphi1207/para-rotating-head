package me.ihqqq.rotatingheads;

import me.ihqqq.rotatingheads.command.PrhCommand;
import me.ihqqq.rotatingheads.config.HeadRepository;
import me.ihqqq.rotatingheads.config.Language;
import me.ihqqq.rotatingheads.config.SettingsHolder;
import me.ihqqq.rotatingheads.config.SettingsLoader;
import me.ihqqq.rotatingheads.listener.HeadListener;
import me.ihqqq.rotatingheads.model.HeadModels.Head;
import me.ihqqq.rotatingheads.runtime.HeadRuntime;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RotatingHeadsPlugin extends JavaPlugin {
    private final Map<String, Head> heads = new LinkedHashMap<>();
    private HeadRepository repository;
    private HeadRuntime runtime;

    @Override
    public void onEnable() {
        saveResource("heads.yml", false);
        SettingsHolder settings = new SettingsHolder(new SettingsLoader(this));
        Language language = new Language(this, settings);
        repository = new HeadRepository(this);
        repository.load();
        heads.putAll(repository.all());
        runtime = new HeadRuntime(this);
        runtime.cleanupOrphans();
        runtime.load(heads);
        PrhCommand command = new PrhCommand(repository, runtime, heads, settings, language);
        PluginCommand pluginCommand = getCommand("prh");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }
        getServer().getPluginManager().registerEvents(
                new HeadListener(this, runtime, heads, repository, settings), this);
        getLogger().info("Loaded " + heads.size() + " configured head(s).");
    }

    @Override
    public void onDisable() {
        if (runtime != null) {
            runtime.close();
        }
    }
}