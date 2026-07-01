package xyz.elnaxho.sneakgrow;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /plant - toggles AutoPlant for the sender. */
public final class PlantCommand implements CommandExecutor {
    private final ConfigManager config;
    private final PlayerFeatureState state;

    public PlantCommand(ConfigManager config, PlayerFeatureState state) {
        this.config = config;
        this.state = state;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.getMessagePlayersOnly());
            return true;
        }
        if (!player.hasPermission(Permissions.COMMAND_PLANT)) {
            player.sendMessage(config.getMessageNoPermission());
            return true;
        }
        boolean enabled = state.toggleAutoPlant(player.getUniqueId());
        player.sendMessage(enabled ? config.getMessagePlantOn() : config.getMessagePlantOff());
        return true;
    }
}
