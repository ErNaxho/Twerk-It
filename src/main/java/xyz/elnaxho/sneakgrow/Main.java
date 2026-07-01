package xyz.elnaxho.sneakgrow;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class Main extends JavaPlugin {
    private final Set<UUID> enabledPlayers = new HashSet<>();
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        configManager.loadConfig();

        GrowListener growListener = new GrowListener(this, configManager, enabledPlayers);
        getServer().getPluginManager().registerEvents(growListener, this);

        GrowCommand growCommand = new GrowCommand(this, configManager, enabledPlayers);
        PluginCommand command = Objects.requireNonNull(getCommand("grow"), "Command grow not defined in plugin.yml");
        command.setExecutor(growCommand);
        command.setTabCompleter(growCommand);

        getLogger().info("SneakGrow has been enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("SneakGrow has been disabled.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public boolean isEnabledFor(UUID playerId) {
        return enabledPlayers.contains(playerId);
    }

    public void setEnabledFor(UUID playerId, boolean enabled) {
        if (enabled) {
            enabledPlayers.add(playerId);
        } else {
            enabledPlayers.remove(playerId);
        }
    }

    public boolean toggleEnabled(UUID playerId) {
        boolean enabled = !enabledPlayers.contains(playerId);
        setEnabledFor(playerId, enabled);
        return enabled;
    }
}
