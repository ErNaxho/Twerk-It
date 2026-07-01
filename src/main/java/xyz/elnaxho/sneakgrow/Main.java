package xyz.elnaxho.sneakgrow;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class Main extends JavaPlugin {
    private ConfigManager configManager;
    private PlayerFeatureState featureState;
    private DebugLogger debugLogger;
    private WorldGuardHook worldGuardHook;
    private FaweHook faweHook;
    private LuckPermsHook luckPermsHook;

    @Override
    public void onLoad() {
        // WorldGuard flags must be registered before WorldGuard's own onEnable
        // locks its flag registry, so this has to happen in onLoad(), not onEnable().
        WorldGuardHook.registerFlags(this);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        configManager.loadConfig();

        featureState = new PlayerFeatureState();
        debugLogger = new DebugLogger(this, configManager);
        worldGuardHook = new WorldGuardHook(this, configManager.isWorldGuardEnabled());
        faweHook = new FaweHook(this);
        luckPermsHook = new LuckPermsHook(this);

        getServer().getPluginManager().registerEvents(
                new GrowListener(configManager, featureState, worldGuardHook, debugLogger), this);
        getServer().getPluginManager().registerEvents(
                new PlantListener(configManager, featureState, worldGuardHook, debugLogger), this);

        registerCommand("grow", new GrowCommand(configManager, featureState));
        registerCommand("plant", new PlantCommand(configManager, featureState));

        logIntegrationStatus();
        getLogger().info("SneakGrow has been enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("SneakGrow has been disabled.");
    }

    private void registerCommand(String name, Object executor) {
        PluginCommand command = Objects.requireNonNull(getCommand(name), "Command " + name + " not defined in plugin.yml");
        if (executor instanceof org.bukkit.command.CommandExecutor commandExecutor) {
            command.setExecutor(commandExecutor);
        }
        if (executor instanceof org.bukkit.command.TabCompleter tabCompleter) {
            command.setTabCompleter(tabCompleter);
        }
    }

    private void logIntegrationStatus() {
        getLogger().info("WorldGuard integration: " + (worldGuardHook.isActive() ? "active" : "not active"));
        getLogger().info("FastAsyncWorldEdit integration: " + (faweHook.isAvailable() ? "available" : "not available"));
        getLogger().info("LuckPerms integration: " + (luckPermsHook.isAvailable() ? "available" : "not available"));
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PlayerFeatureState getFeatureState() {
        return featureState;
    }

    public WorldGuardHook getWorldGuardHook() {
        return worldGuardHook;
    }

    public FaweHook getFaweHook() {
        return faweHook;
    }

    public LuckPermsHook getLuckPermsHook() {
        return luckPermsHook;
    }
}
