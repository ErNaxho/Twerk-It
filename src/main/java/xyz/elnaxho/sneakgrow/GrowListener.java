package xyz.elnaxho.sneakgrow;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.TreeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class GrowListener implements Listener {
    private final Main plugin;
    private final ConfigManager config;
    private final Set<java.util.UUID> enabledPlayers;
    private final Random random = new Random();
    private final Map<Material, TreeType> saplingTreeTypes = new HashMap<>();
    private final Map<UUID, Long> lastBoneMealWarningTime = new HashMap<>();

    public GrowListener(Main plugin, ConfigManager config, Set<java.util.UUID> enabledPlayers) {
        this.plugin = plugin;
        this.config = config;
        this.enabledPlayers = enabledPlayers;
        initializeSaplingTypes();
    }

    private void initializeSaplingTypes() {
        saplingTreeTypes.put(Material.OAK_SAPLING, TreeType.TREE);
        saplingTreeTypes.put(Material.SPRUCE_SAPLING, TreeType.REDWOOD);
        saplingTreeTypes.put(Material.BIRCH_SAPLING, TreeType.BIRCH);
        saplingTreeTypes.put(Material.JUNGLE_SAPLING, TreeType.JUNGLE);
        saplingTreeTypes.put(Material.ACACIA_SAPLING, TreeType.ACACIA);
        saplingTreeTypes.put(Material.DARK_OAK_SAPLING, TreeType.DARK_OAK);
        saplingTreeTypes.put(Material.CHERRY_SAPLING, TreeType.CHERRY);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }
        Player player = event.getPlayer();
        if (!isEnabled(player)) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        List<Location> path = getPlayerBlockPath(from, to);
        if (path.isEmpty()) {
            return;
        }
        processGrowth(player, path);
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }
        Player player = event.getPlayer();
        if (!isEnabled(player)) {
            return;
        }
        processGrowth(player, player.getLocation());
    }

    @EventHandler
    public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
        if (!event.isSprinting()) {
            return;
        }
        Player player = event.getPlayer();
        if (!isEnabled(player)) {
            return;
        }
        processGrowth(player, player.getLocation());
    }

    private boolean isEnabled(Player player) {
        return enabledPlayers.contains(player.getUniqueId()) && player.hasPermission("sneakgrow.use");
    }

    private void processGrowth(Player player, Location center) {
        processGrowth(player, List.of(center));
    }

    private void processGrowth(Player player, List<Location> path) {
        PlayerInventory inventory = player.getInventory();
        if (config.isBoneMealUse() && !inventory.contains(Material.BONE_MEAL)) {
            sendBoneMealWarning(player);
            return;
        }

        ItemStack hoe = null;
        Integer hoeSlot = null;
        if (config.isHoeUse()) {
            ItemStack mainHand = inventory.getItemInMainHand();
            if (isHoe(mainHand)) {
                hoe = mainHand;
                hoeSlot = inventory.getHeldItemSlot();
            } else {
                for (int slot = 0; slot < inventory.getSize(); slot++) {
                    ItemStack stack = inventory.getItem(slot);
                    if (isHoe(stack)) {
                        hoe = stack;
                        hoeSlot = slot;
                        break;
                    }
                }
            }
            if (hoe == null) {
                player.sendMessage(config.getMessageNoHoe());
                return;
            }
        }

        World world = player.getWorld();
        int radius = config.getGrowthRadius();
        int radiusSquared = radius * radius;

        int changes = 0;
        boolean hoeBroken = false;

        for (Location center : path) {
            int centerX = center.getBlockX();
            int centerY = center.getBlockY();
            int centerZ = center.getBlockZ();
            for (int dx = -radius; dx <= radius && !hoeBroken; dx++) {
                for (int dy = -radius; dy <= radius && !hoeBroken; dy++) {
                    for (int dz = -radius; dz <= radius && !hoeBroken; dz++) {
                        if (dx * dx + dy * dy + dz * dz > radiusSquared) {
                            continue;
                        }
                        Block block = world.getBlockAt(centerX + dx, centerY + dy, centerZ + dz);
                        if (!config.isAllowedCrop(block.getType())) {
                            continue;
                        }

                        if (!shouldGrow()) {
                            continue;
                        }

                        if (tryGrowCrop(block)) {
                            changes++;
                            if (config.isBoneMealUse()) {
                                consumeBoneMeal(inventory, player);
                            }
                            if (config.isHoeUse() && hoe != null && hoeSlot != null) {
                                hoeBroken = damageHoe(hoe, hoeSlot, player);
                            }
                        }
                    }
                }
            }
        }

        if (changes > 0 && config.isHoeUse() && hoeBroken) {
            // Stop further growth when the hoe breaks.
        }
    }

    private void consumeBoneMeal(PlayerInventory inventory, Player player) {
        int boneMealSlot = inventory.first(Material.BONE_MEAL);
        if (boneMealSlot < 0) {
            return;
        }
        ItemStack boneMeal = inventory.getItem(boneMealSlot);
        if (boneMeal == null) {
            return;
        }

        int amount = boneMeal.getAmount() - 1;
        if (amount <= 0) {
            inventory.setItem(boneMealSlot, null);
        } else {
            boneMeal.setAmount(amount);
            inventory.setItem(boneMealSlot, boneMeal);
        }
    }

    private void sendBoneMealWarning(Player player) {
        long now = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        long lastWarning = lastBoneMealWarningTime.getOrDefault(playerId, 0L);
        if (now - lastWarning < config.getMessageNoBoneMealCooldownMs()) {
            return;
        }

        lastBoneMealWarningTime.put(playerId, now);
        player.sendTitle(config.getMessageNoBoneMealTitle(), "", 10, config.getMessageNoBoneMealTitleDuration(), 10);
    }

    private boolean isHoe(ItemStack itemStack) {
        return itemStack != null && itemStack.getType().name().endsWith("_HOE");
    }

    private List<Location> getPlayerBlockPath(Location from, Location to) {
        List<Location> path = new ArrayList<>();
        int fromX = from.getBlockX();
        int fromY = from.getBlockY();
        int fromZ = from.getBlockZ();
        int toX = to.getBlockX();
        int toY = to.getBlockY();
        int toZ = to.getBlockZ();

        int dx = Integer.compare(toX, fromX);
        int dy = Integer.compare(toY, fromY);
        int dz = Integer.compare(toZ, fromZ);

        int currentX = fromX;
        int currentY = fromY;
        int currentZ = fromZ;

        path.add(from.clone());
        while (currentX != toX || currentY != toY || currentZ != toZ) {
            if (currentX != toX) {
                currentX += dx;
            }
            if (currentY != toY) {
                currentY += dy;
            }
            if (currentZ != toZ) {
                currentZ += dz;
            }
            path.add(new Location(from.getWorld(), currentX, currentY, currentZ));
        }
        return path;
    }

    private boolean shouldGrow() {
        int chance = config.getGrowthChance();
        return random.nextInt(100) < chance;
    }

    private boolean tryGrowCrop(Block block) {
        Material material = block.getType();
        if (saplingTreeTypes.containsKey(material)) {
            return growSapling(block);
        }

        if (block.getBlockData() instanceof Ageable ageable) {
            int currentAge = ageable.getAge();
            int maxAge = ageable.getMaximumAge();
            if (currentAge < maxAge) {
                if (config.isStageGrowing()) {
                    ageable.setAge(currentAge + 1);
                } else {
                    ageable.setAge(maxAge);
                }
                block.setBlockData(ageable, true);
                return true;
            }
        }

        return false;
    }

    private boolean growSapling(Block block) {
        World world = block.getWorld();
        TreeType treeType = saplingTreeTypes.get(block.getType());
        if (treeType == null) {
            return false;
        }
        Location location = block.getLocation();
        return world.generateTree(location, treeType);
    }

    private boolean damageHoe(ItemStack hoe, int slot, Player player) {
        if (hoe == null || slot < 0) {
            return false;
        }
        if (!hoe.hasItemMeta() || !(hoe.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable)) {
            return false;
        }

        org.bukkit.inventory.meta.Damageable damageMeta = (org.bukkit.inventory.meta.Damageable) hoe.getItemMeta();
        int currentDamage = damageMeta.getDamage();
        int newDamage = currentDamage + 1;
        int maxDurability = hoe.getType().getMaxDurability();

        if (newDamage >= maxDurability) {
            player.getInventory().setItem(slot, null);
            return true;
        }

        damageMeta.setDamage(newDamage);
        hoe.setItemMeta((org.bukkit.inventory.meta.ItemMeta) damageMeta);
        return false;
    }
}
