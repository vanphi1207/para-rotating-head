package me.ihqqq.rotatingheads;

import me.ihqqq.rotatingheads.command.MmhCommand;
import me.ihqqq.rotatingheads.config.HeadRepository;
import me.ihqqq.rotatingheads.config.Language;
import me.ihqqq.rotatingheads.config.Settings;
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
        Settings settings = new SettingsLoader(this).load();
        Language language = new Language(this, settings);
        repository = new HeadRepository(this);
        repository.load();
        heads.putAll(repository.all());
        runtime = new HeadRuntime(this);
        runtime.cleanupOrphans();
        runtime.load(heads);
        MmhCommand command = new MmhCommand(repository, runtime, heads,
                settings.defaultTexture(), language);
        PluginCommand pluginCommand = getCommand("mmh");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }
        getServer().getPluginManager().registerEvents(
                new HeadListener(runtime, heads), this);
        getLogger().info("Loaded " + heads.size() + " configured head(s).");
    }

    @Override
    public void onDisable() {
        if (runtime != null) {
            runtime.close();
        }
    }
}