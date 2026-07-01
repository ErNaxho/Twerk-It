package xyz.elnaxho.sneakgrow;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles both AutoGrow modes:
 *
 *  - Move Grow: triggers whenever the player moves into a new block.
 *               Only advances Ageable crops (wheat, carrots, stems, etc).
 *               Never touches saplings.
 *
 *  - Sneak Grow: triggers while the player is sneaking (both on the initial
 *                toggle-sneak and on subsequent movement while sneaking).
 *                Advances Ageable crops AND saplings.
 *
 * Sapling growth uses {@link Block#applyBoneMeal(BlockFace)}, which is
 * vanilla's own bone-meal-application logic. This is deliberate: it means
 * every current and future vanilla sapling layout (1x1 AND 2x2 giant trees -
 * dark oak, spruce, jungle, pale oak, etc) is supported automatically,
 * without hardcoding a Material -> TreeType map that would need to be
 * updated every time Mojang adds a new tree type.
 */
public final class GrowListener implements Listener {
    private final ConfigManager config;
    private final PlayerFeatureState state;
    private final WorldGuardHook worldGuard;
    private final DebugLogger debug;
    private final Random random = new Random();

    public GrowListener(ConfigManager config, PlayerFeatureState state, WorldGuardHook worldGuard, DebugLogger debug) {
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
        boolean sneaking = player.isSneaking();
        boolean sneakMode = sneaking && state.isSneakGrow(player.getUniqueId());
        boolean moveMode = !sneaking && state.isMoveGrow(player.getUniqueId());

        if (!sneakMode && !moveMode) {
            return;
        }
        if (!hasFeaturePermission(player, sneakMode)) {
            return;
        }

        Location to = event.getTo();
        if (to == null) {
            return;
        }

        debug.log(player.getName() + (sneakMode
                ? " triggered Sneak Grow (moving while sneaking)."
                : " triggered Movement Grow."));
        runGrowth(player, to, sneakMode);
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }
        Player player = event.getPlayer();
        if (!state.isSneakGrow(player.getUniqueId())) {
            return;
        }
        if (!hasFeaturePermission(player, true)) {
            return;
        }
        debug.log(player.getName() + " triggered Sneak Grow (started sneaking).");
        runGrowth(player, player.getLocation(), true);
    }

    private boolean hasFeaturePermission(Player player, boolean sneakMode) {
        if (!player.hasPermission(Permissions.FEATURE_AUTOGROW)) {
            debug.log(player.getName() + " denied AutoGrow: missing " + Permissions.FEATURE_AUTOGROW);
            return false;
        }
        String specific = sneakMode ? Permissions.FEATURE_AUTOGROW_SNEAK : Permissions.FEATURE_AUTOGROW_MOVE;
        if (!player.hasPermission(specific)) {
            debug.log(player.getName() + " denied AutoGrow: missing " + specific);
            return false;
        }
        return true;
    }

    private void runGrowth(Player player, Location center, boolean allowSaplings) {
        if (!worldGuard.isAutoGrowAllowed(center)) {
            debug.log("Region denied AutoGrow for " + player.getName() + " at " + describe(center));
            return;
        }

        PlayerInventory inventory = player.getInventory();

        if (config.isAutoGrowBoneMealUse() && !inventory.contains(Material.BONE_MEAL)) {
            debug.log(player.getName() + " has no bone meal - AutoGrow skipped.");
            return;
        }
        if (config.isAutoGrowHoeUse() && !holdsOrCarriesHoe(inventory)) {
            player.sendMessage(config.getMessageNoHoe());
            debug.log(player.getName() + " has no hoe - AutoGrow skipped.");
            return;
        }

        int area = config.getAutoGrowArea();
        int chance = config.getAutoGrowChance();
        AtomicBoolean grewSomething = new AtomicBoolean(false);

        AreaUtil.forEachInArea(center, area, block -> {
            if (random.nextInt(100) >= chance) {
                return;
            }
            Material type = block.getType();
            boolean grew;
            if (isSapling(type)) {
                grew = allowSaplings && growSapling(block);
            } else {
                grew = growAgeable(block);
            }
            if (grew) {
                grewSomething.set(true);
            }
        });

        if (grewSomething.get() && config.isAutoGrowBoneMealUse()) {
            consumeOne(inventory, Material.BONE_MEAL);
        }
    }

    private boolean isSapling(Material material) {
        return material.name().endsWith("_SAPLING") || material == Material.MANGROVE_PROPAGULE;
    }

    private boolean growSapling(Block block) {
        boolean grew = block.applyBoneMeal(BlockFace.UP);
        if (grew) {
            debug.log("Sapling successfully grown into a tree at " + describe(block.getLocation()));
        }
        return grew;
    }

    private boolean growAgeable(Block block) {
        if (!(block.getBlockData() instanceof Ageable ageable)) {
            return false;
        }
        int currentAge = ageable.getAge();
        int maxAge = ageable.getMaximumAge();
        if (currentAge >= maxAge) {
            return false;
        }
        ageable.setAge(config.isAutoGrowStageGrowing() ? currentAge + 1 : maxAge);
        block.setBlockData(ageable, true);
        debug.log("Crop advanced at " + describe(block.getLocation()));
        return true;
    }

    private boolean holdsOrCarriesHoe(PlayerInventory inventory) {
        if (isHoe(inventory.getItemInMainHand())) {
            return true;
        }
        for (ItemStack stack : inventory.getContents()) {
            if (isHoe(stack)) {
                return true;
            }
        }
        return false;
    }

    private boolean isHoe(ItemStack itemStack) {
        return itemStack != null && itemStack.getType().name().endsWith("_HOE");
    }

    private void consumeOne(PlayerInventory inventory, Material material) {
        int slot = inventory.first(material);
        if (slot < 0) {
            return;
        }
        ItemStack stack = inventory.getItem(slot);
        if (stack == null) {
            return;
        }
        int amount = stack.getAmount() - 1;
        inventory.setItem(slot, amount <= 0 ? null : withAmount(stack, amount));
    }

    private ItemStack withAmount(ItemStack stack, int amount) {
        stack.setAmount(amount);
        return stack;
    }

    private String describe(Location location) {
        return location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }
}
