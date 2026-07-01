package xyz.elnaxho.sneakgrow;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Set;

/**
 * Handles AutoPlant: when enabled, automatically plants seeds on farmland
 * and nether wart on soul sand within the configured area as the player
 * moves. Consumes items from the player's inventory normally (no
 * duplication) and only plants where there is air above and the player
 * actually has the required item, respecting survival mechanics.
 */
public final class PlantListener implements Listener {
    private static final Set<Material> FARMLAND_TYPES = Set.of(Material.FARMLAND);
    private static final Set<Material> SOUL_SAND_TYPES = Set.of(Material.SOUL_SAND);

    private final ConfigManager config;
    private final PlayerFeatureState state;
    private final WorldGuardHook worldGuard;
    private final DebugLogger debug;

    public PlantListener(ConfigManager config, PlayerFeatureState state, WorldGuardHook worldGuard, DebugLogger debug) {
        this.config = config;
        this.state = state;
        this.worldGuard = worldGuard;
        this.debug = debug;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }
        Player player = event.getPlayer();
        if (!state.isAutoPlant(player.getUniqueId())) {
            return;
        }
        if (!player.hasPermission(Permissions.FEATURE_AUTOPLANT)) {
            debug.log(player.getName() + " denied AutoPlant: missing " + Permissions.FEATURE_AUTOPLANT);
            return;
        }
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (!worldGuard.isAutoPlantAllowed(to)) {
            debug.log("Region denied AutoPlant for " + player.getName() + " at " + describe(to));
            return;
        }

        debug.log(player.getName() + " triggered AutoPlant.");
        PlayerInventory inventory = player.getInventory();
        int area = config.getAutoPlantArea();

        AreaUtil.forEachInArea(to, area, block -> tryPlant(player, inventory, block));
    }

    private void tryPlant(Player player, PlayerInventory inventory, Block soilBlock) {
        Material soilType = soilBlock.getType();

        if (config.isAutoPlantFarmlandEnabled() && FARMLAND_TYPES.contains(soilType)) {
            plantFromCandidates(player, inventory, soilBlock, config.getFarmlandSeeds());
            return;
        }
        if (config.isAutoPlantSoulSandEnabled() && SOUL_SAND_TYPES.contains(soilType)) {
            plantFromCandidates(player, inventory, soilBlock, config.getSoulSandSeeds());
        }
    }

    private void plantFromCandidates(Player player, PlayerInventory inventory, Block soilBlock, Set<Material> candidates) {
        Block above = soilBlock.getRelative(org.bukkit.block.BlockFace.UP);
        if (above.getType() != Material.AIR) {
            return;
        }

        for (Material seed : candidates) {
            int slot = inventory.first(seed);
            if (slot < 0) {
                continue;
            }
            Material plantMaterial = seedToPlant(seed);
            if (plantMaterial == null) {
                continue;
            }

            above.setType(plantMaterial);
            if (above.getBlockData() instanceof Ageable ageable) {
                ageable.setAge(0);
                above.setBlockData(ageable, true);
            }

            consumeOne(inventory, slot);
            debug.log("Planted " + plantMaterial + " at " + describe(above.getLocation())
                    + " for " + player.getName());
            return;
        }

        debug.log("No matching seeds found in " + player.getName() + "'s inventory for " + describe(soilBlock.getLocation()));
    }

    /**
     * Maps a seed/item Material to the block Material it places into the world.
     * Kept as an explicit map (rather than assuming name similarity) because
     * several seed items do not share a name with their planted block
     * (e.g. CARROT -> CARROTS, POTATO -> POTATOES, NETHER_WART item -> NETHER_WART block).
     */
    private Material seedToPlant(Material seed) {
        return switch (seed) {
            case WHEAT_SEEDS -> Material.WHEAT;
            case BEETROOT_SEEDS -> Material.BEETROOTS;
            case CARROT -> Material.CARROTS;
            case POTATO -> Material.POTATOES;
            case PITCHER_POD -> Material.PITCHER_CROP;
            case TORCHFLOWER_SEEDS -> Material.TORCHFLOWER_CROP;
            case NETHER_WART -> Material.NETHER_WART;
            default -> null;
        };
    }

    private void consumeOne(PlayerInventory inventory, int slot) {
        ItemStack stack = inventory.getItem(slot);
        if (stack == null) {
            return;
        }
        int amount = stack.getAmount() - 1;
        if (amount <= 0) {
            inventory.setItem(slot, null);
        } else {
            stack.setAmount(amount);
            inventory.setItem(slot, stack);
        }
    }

    private String describe(Location location) {
        return location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }
}
