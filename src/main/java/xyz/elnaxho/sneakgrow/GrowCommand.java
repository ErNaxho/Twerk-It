package xyz.elnaxho.sneakgrow;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * /grow            -> toggles BOTH sneak and move grow together (see rules below)
 * /grow sneak       -> toggles Sneak Grow only
 * /grow move        -> toggles Move Grow only
 * /grow reload      -> reloads config.yml
 *
 * Combined toggle rule for bare "/grow":
 *   sneak=true,  move=true   -> both false
 *   sneak=false, move=false  -> both true
 *   sneak != move (mixed)    -> both false
 */
public final class GrowCommand implements CommandExecutor, TabCompleter {
    private final ConfigManager config;
    private final PlayerFeatureState state;

    public GrowCommand(ConfigManager config, PlayerFeatureState state) {
        this.config = config;
        this.state = state;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.getMessagePlayersOnly());
            return true;
        }

        if (args.length == 0) {
            return handleBoth(player);
        }

        return switch (args[0].toLowerCase()) {
            case "sneak" -> handleSneak(player);
            case "move" -> handleMove(player);
            case "reload" -> handleReload(sender);
            default -> {
                player.sendMessage("Usage: /grow [sneak|move|reload]");
                yield true;
            }
        };
    }

    private boolean handleBoth(Player player) {
        if (!player.hasPermission(Permissions.COMMAND_GROW)) {
            player.sendMessage(config.getMessageNoPermission());
            return true;
        }
        UUID id = player.getUniqueId();
        boolean sneak = state.isSneakGrow(id);
        boolean move = state.isMoveGrow(id);

        // Both on -> disable both. Both off -> enable both. Mixed -> disable both.
        boolean enableBoth;
        if (sneak && move) {
            enableBoth = false;
        } else if (!sneak && !move) {
            enableBoth = true;
        } else {
            enableBoth = false; // mixed state -> disable both
        }

        state.setSneakGrow(id, enableBoth);
        state.setMoveGrow(id, enableBoth);
        player.sendMessage(enableBoth ? config.getMessageGrowBothOn() : config.getMessageGrowBothOff());
        return true;
    }

    private boolean handleSneak(Player player) {
        if (!player.hasPermission(Permissions.COMMAND_GROW_SNEAK)) {
            player.sendMessage(config.getMessageNoPermission());
            return true;
        }
        boolean enabled = state.toggleSneakGrow(player.getUniqueId());
        player.sendMessage(enabled ? config.getMessageGrowSneakOn() : config.getMessageGrowSneakOff());
        return true;
    }

    private boolean handleMove(Player player) {
        if (!player.hasPermission(Permissions.COMMAND_GROW_MOVE)) {
            player.sendMessage(config.getMessageNoPermission());
            return true;
        }
        boolean enabled = state.toggleMoveGrow(player.getUniqueId());
        player.sendMessage(enabled ? config.getMessageGrowMoveOn() : config.getMessageGrowMoveOff());
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission(Permissions.COMMAND_RELOAD)) {
            sender.sendMessage(config.getMessageNoPermission());
            return true;
        }
        config.loadConfig();
        sender.sendMessage(config.getMessageReloadSuccess());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            for (String option : List.of("sneak", "move", "reload")) {
                if (option.startsWith(args[0].toLowerCase())) {
                    completions.add(option);
                }
            }
            return completions;
        }
        return Collections.emptyList();
    }
}
