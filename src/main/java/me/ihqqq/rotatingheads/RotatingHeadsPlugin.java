package me.ihqqq.rotatingheads;

import me.ihqqq.rotatingheads.command.RotatingHeadCommand;
import me.ihqqq.rotatingheads.config.HeadRepository;
import me.ihqqq.rotatingheads.config.Language;
import me.ihqqq.rotatingheads.config.SettingsHolder;
import me.ihqqq.rotatingheads.config.SettingsLoader;
import me.ihqqq.rotatingheads.listener.HeadListener;
import me.ihqqq.rotatingheads.model.Head;
import me.ihqqq.rotatingheads.manager.HeadManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RotatingHeadsPlugin extends JavaPlugin {
    private final Map<String, Head> heads = new LinkedHashMap<>();
    private HeadRepository repository;
    private HeadManager headManager;

    @Override
    public void onEnable() {
        saveResource("heads.yml", false);
        SettingsHolder settings = new SettingsHolder(new SettingsLoader(this));
        Language language = new Language(this, settings);
        repository = new HeadRepository(this);
        if (!repository.load()) {
            getLogger().severe("Starting with no heads loaded; fix heads.yml and run "
                    + "/prh reload once it is valid.");
        }
        heads.putAll(repository.all());
        headManager = new HeadManager(this);
        headManager.cleanupOrphans();
        headManager.load(heads);
        RotatingHeadCommand command = new RotatingHeadCommand(repository, headManager, heads, settings, language);
        PluginCommand pluginCommand = getCommand("prh");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }
        getServer().getPluginManager().registerEvents(
                new HeadListener(this, headManager, heads, repository, settings), this);
        getLogger().info("Loaded " + heads.size() + " configured head(s).");
    }

    @Override
    public void onDisable() {
        if (headManager != null) {
            headManager.close();
        }
    }
}