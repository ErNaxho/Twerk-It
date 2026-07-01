package xyz.elnaxho.sneakgrow;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Loads and exposes every configurable value from config.yml.
 * All feature classes read from here - nothing should call
 * plugin.getConfig() directly outside of this class.
 */
public class ConfigManager {
    private final JavaPlugin plugin;

    // --- General ---
    private boolean debug = false;
    private boolean worldGuardEnabled = true;

    // --- AutoGrow ---
    private int autoGrowArea = 4;
    private int autoGrowChance = 100;
    private boolean autoGrowBoneMealUse = false;
    private boolean autoGrowHoeUse = false;
    private boolean autoGrowStageGrowing = true;
    private final Set<Material> allowedCrops = EnumSet.noneOf(Material.class);

    // --- AutoPlant ---
    private int autoPlantArea = 4;
    private boolean autoPlantFarmlandEnabled = true;
    private boolean autoPlantSoulSandEnabled = true;
    private final Set<Material> farmlandSeeds = EnumSet.noneOf(Material.class);
    private final Set<Material> soulSandSeeds = EnumSet.noneOf(Material.class);

    // --- Messages ---
    private String messageNoBoneMeal = "<red>You don't have bone meal to grow the crops.";
    private String messageNoHoe = "<red>You need a hoe to grow the crops.";
    private String messageGrowBothOn = "<green>AutoGrow enabled (Sneak + Move).";
    private String messageGrowBothOff = "<red>AutoGrow disabled (Sneak + Move).";
    private String messageGrowSneakOn = "<green>Sneak Grow enabled.";
    private String messageGrowSneakOff = "<red>Sneak Grow disabled.";
    private String messageGrowMoveOn = "<green>Move Grow enabled.";
    private String messageGrowMoveOff = "<red>Move Grow disabled.";
    private String messagePlantOn = "<green>AutoPlant enabled.";
    private String messagePlantOff = "<red>AutoPlant disabled.";
    private String messageReloadSuccess = "<green>Configuration reloaded successfully.";
    private String messageNoPermission = "<red>You don't have permission to do this.";
    private String messagePlayersOnly = "<red>This command can only be used by a player.";

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        try {
            debug = config.getBoolean("debug", false);
            worldGuardEnabled = config.getBoolean("worldguard.enabled", true);

            autoGrowArea = Math.max(1, config.getInt("autogrow.area", 4));
            autoGrowChance = clamp(config.getInt("autogrow.growth-chance", 100), 0, 100);
            autoGrowBoneMealUse = config.getBoolean("autogrow.bone-meal-use", false);
            autoGrowHoeUse = config.getBoolean("autogrow.hoe-use", false);
            autoGrowStageGrowing = config.getBoolean("autogrow.stage-growing", true);

            allowedCrops.clear();
            var cropsSection = config.getConfigurationSection("crops");
            if (cropsSection != null) {
                for (String key : cropsSection.getKeys(false)) {
                    Material material = Material.matchMaterial(key);
                    if (material == null) {
                        plugin.getLogger().warning("Invalid crop material in config.yml: " + key);
                        continue;
                    }
                    if (cropsSection.getBoolean(key, false)) {
                        allowedCrops.add(material);
                    }
                }
            }

            autoPlantArea = Math.max(1, config.getInt("autoplant.area", 4));
            autoPlantFarmlandEnabled = config.getBoolean("autoplant.plant-farmland", true);
            autoPlantSoulSandEnabled = config.getBoolean("autoplant.plant-soul-sand", true);

            farmlandSeeds.clear();
            addMaterials(farmlandSeeds, config.getStringList("autoplant.farmland-seeds"));

            soulSandSeeds.clear();
            addMaterials(soulSandSeeds, config.getStringList("autoplant.soul-sand-seeds"));

            messageNoBoneMeal = translate(config.getString("messages.no-bone-meal", messageNoBoneMeal));
            messageNoHoe = translate(config.getString("messages.no-hoe", messageNoHoe));
            messageGrowBothOn = translate(config.getString("messages.grow-both-on", messageGrowBothOn));
            messageGrowBothOff = translate(config.getString("messages.grow-both-off", messageGrowBothOff));
            messageGrowSneakOn = translate(config.getString("messages.grow-sneak-on", messageGrowSneakOn));
            messageGrowSneakOff = translate(config.getString("messages.grow-sneak-off", messageGrowSneakOff));
            messageGrowMoveOn = translate(config.getString("messages.grow-move-on", messageGrowMoveOn));
            messageGrowMoveOff = translate(config.getString("messages.grow-move-off", messageGrowMoveOff));
            messagePlantOn = translate(config.getString("messages.plant-on", messagePlantOn));
            messagePlantOff = translate(config.getString("messages.plant-off", messagePlantOff));
            messageReloadSuccess = translate(config.getString("messages.reload-success", messageReloadSuccess));
            messageNoPermission = translate(config.getString("messages.no-permission", messageNoPermission));
            messagePlayersOnly = translate(config.getString("messages.players-only", messagePlayersOnly));
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to load SneakGrow configuration. Using last known good values.", exception);
        }
    }

    private void addMaterials(Set<Material> target, List<String> names) {
        for (String entry : names) {
            Material material = Material.matchMaterial(entry);
            if (material == null) {
                plugin.getLogger().warning("Invalid material in config.yml: " + entry);
                continue;
            }
            target.add(material);
        }
    }

    // --- General ---
    public boolean isDebug() {
        return debug;
    }

    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }

    // --- AutoGrow ---
    public int getAutoGrowArea() {
        return autoGrowArea;
    }

    public int getAutoGrowChance() {
        return autoGrowChance;
    }

    public boolean isAutoGrowBoneMealUse() {
        return autoGrowBoneMealUse;
    }

    public boolean isAutoGrowHoeUse() {
        return autoGrowHoeUse;
    }

    public boolean isAutoGrowStageGrowing() {
        return autoGrowStageGrowing;
    }

    public boolean isAllowedCrop(Material material) {
        return allowedCrops.contains(material);
    }

    // --- AutoPlant ---
    public int getAutoPlantArea() {
        return autoPlantArea;
    }

    public boolean isAutoPlantFarmlandEnabled() {
        return autoPlantFarmlandEnabled;
    }

    public boolean isAutoPlantSoulSandEnabled() {
        return autoPlantSoulSandEnabled;
    }

    public Set<Material> getFarmlandSeeds() {
        return farmlandSeeds;
    }

    public Set<Material> getSoulSandSeeds() {
        return soulSandSeeds;
    }

    // --- Messages ---
    public String getMessageNoBoneMeal() {
        return messageNoBoneMeal;
    }

    public String getMessageNoHoe() {
        return messageNoHoe;
    }

    public String getMessageGrowBothOn() {
        return messageGrowBothOn;
    }

    public String getMessageGrowBothOff() {
        return messageGrowBothOff;
    }

    public String getMessageGrowSneakOn() {
        return messageGrowSneakOn;
    }

    public String getMessageGrowSneakOff() {
        return messageGrowSneakOff;
    }

    public String getMessageGrowMoveOn() {
        return messageGrowMoveOn;
    }

    public String getMessageGrowMoveOff() {
        return messageGrowMoveOff;
    }

    public String getMessagePlantOn() {
        return messagePlantOn;
    }

    public String getMessagePlantOff() {
        return messagePlantOff;
    }

    public String getMessageReloadSuccess() {
        return messageReloadSuccess;
    }

    public String getMessageNoPermission() {
        return messageNoPermission;
    }

    public String getMessagePlayersOnly() {
        return messagePlayersOnly;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Deserializes a MiniMessage-formatted string (e.g. "<red>Hello") into a
     * legacy '&sect;'-coded String, so the rest of the plugin can keep using
     * plain Strings while config.yml supports MiniMessage tags.
     */
    private static String translate(String message) {
        Component component = MiniMessage.miniMessage().deserialize(message);
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
}