package xyz.elnaxho.sneakgrow;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class GrowCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final ConfigManager config;
    private final Set<UUID> enabledPlayers;

    public GrowCommand(JavaPlugin plugin, ConfigManager config, Set<UUID> enabledPlayers) {
        this.plugin = plugin;
        this.config = config;
        this.enabledPlayers = enabledPlayers;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used by a player.");
                return true;
            }
            if (!sender.hasPermission("sneakgrow.use")) {
                sender.sendMessage(config.getMessageNoPermission());
                return true;
            }
            UUID playerId = player.getUniqueId();
            boolean enabled = !enabledPlayers.contains(playerId);
            if (enabled) {
                enabledPlayers.add(playerId);
                player.sendMessage(config.getMessageToggleOn());
            } else {
                enabledPlayers.remove(playerId);
                player.sendMessage(config.getMessageToggleOff());
            }
            return true;
        }

        String subcommand = args[0].toLowerCase();
        switch (subcommand) {
            case "enable", "on" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("This command can only be used by a player.");
                    return true;
                }
                if (!sender.hasPermission("sneakgrow.use")) {
                    sender.sendMessage(config.getMessageNoPermission());
                    return true;
                }
                enabledPlayers.add(player.getUniqueId());
                player.sendMessage(config.getMessageToggleOn());
                return true;
            }
            case "disable", "off" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("This command can only be used by a player.");
                    return true;
                }
                if (!sender.hasPermission("sneakgrow.use")) {
                    sender.sendMessage(config.getMessageNoPermission());
                    return true;
                }
                enabledPlayers.remove(player.getUniqueId());
                player.sendMessage(config.getMessageToggleOff());
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("sneakgrow.reload")) {
                    sender.sendMessage(config.getMessageNoPermission());
                    return true;
                }
                config.loadConfig();
                sender.sendMessage(config.getMessageReloadSuccess());
                return true;
            }
            default -> {
                sender.sendMessage("Usage: /grow [enable|disable|reload]");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            for (String option : List.of("enable", "disable", "reload")) {
                if (option.startsWith(args[0].toLowerCase())) {
                    completions.add(option);
                }
            }
            return completions;
        }
        return Collections.emptyList();
    }
}
