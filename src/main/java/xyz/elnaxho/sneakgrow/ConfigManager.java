package xyz.elnaxho.sneakgrow;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class ConfigManager {
    private final JavaPlugin plugin;
    private final Map<Material, Boolean> allowedCrops = new HashMap<>();
    private final Set<Material> disabledCrops = new HashSet<>();

    private int growthChance = 15;
    private int growthRadius = 4;
    private boolean boneMealUse = false;
    private boolean hoeUse = false;
    private boolean triggerOnToggleOn = true;
    private boolean triggerOnToggleOff = false;
    private boolean stageGrowing = true;

    private String messageNoBoneMeal = "&cYou don't have bone meal to grow the crops.";
    private String messageNoHoe = "&cYou need a hoe to grow the crops.";
    private String messageToggleOn = "&aSneakGrow enabled.";
    private String messageToggleOff = "&cSneakGrow disabled.";
    private String messageReloadSuccess = "&aConfiguration reloaded successfully.";
    private String messageNoPermission = "&cYou don't have permission to do this.";

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        try {
            growthChance = clamp(config.getInt("yamlsettings.growth-chance", 15), 0, 100);
            growthRadius = clamp(config.getInt("yamlsettings.growth-radius", 4), 1, 10);
            boneMealUse = config.getBoolean("yamlsettings.bone-meal-use", false);
            hoeUse = config.getBoolean("yamlsettings.hoe-use", false);
            triggerOnToggleOn = config.getBoolean("yamlsettings.growth-chance-on-toggle-on", true);
            triggerOnToggleOff = config.getBoolean("yamlsettings.growth-chance-on-toggle-off", false);
            stageGrowing = config.getBoolean("yamlsettings.stage-growing", true);

            messageNoBoneMeal = translate(config.getString("messages.no-bone-meal", messageNoBoneMeal));
            messageNoHoe = translate(config.getString("messages.no-hoe", messageNoHoe));
            messageToggleOn = translate(config.getString("messages.toggle-on", messageToggleOn));
            messageToggleOff = translate(config.getString("messages.toggle-off", messageToggleOff));
            messageReloadSuccess = translate(config.getString("messages.reload-success", messageReloadSuccess));
            messageNoPermission = translate(config.getString("messages.no-permission", messageNoPermission));

            allowedCrops.clear();
            ConfigurationSection cropsSection = config.getConfigurationSection("crops");
            if (cropsSection != null) {
                for (String key : cropsSection.getKeys(false)) {
                    Material material = Material.matchMaterial(key);
                    if (material == null) {
                        plugin.getLogger().warning("Invalid crop material in config.yml: " + key);
                        continue;
                    }
                    boolean enabled = cropsSection.getBoolean(key, false);
                    allowedCrops.put(material, enabled);
                }
            }

            disabledCrops.clear();
            List<String> disabledList = config.getStringList("disabled-crops");
            for (String entry : disabledList) {
                Material material = Material.matchMaterial(entry);
                if (material == null) {
                    plugin.getLogger().warning("Invalid disabled crop material in config.yml: " + entry);
                    continue;
                }
                disabledCrops.add(material);
            }
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to load SneakGrow configuration. Using last known good values.", exception);
        }
    }

    public boolean isAllowedCrop(Material material) {
        return allowedCrops.getOrDefault(material, false) && !disabledCrops.contains(material);
    }

    public int getGrowthChance() {
        return growthChance;
    }

    public int getGrowthRadius() {
        return growthRadius;
    }

    public boolean isBoneMealUse() {
        return boneMealUse;
    }

    public boolean isHoeUse() {
        return hoeUse;
    }

    public boolean isTriggerOnToggleOn() {
        return triggerOnToggleOn;
    }

    public boolean isTriggerOnToggleOff() {
        return triggerOnToggleOff;
    }

    public boolean isStageGrowing() {
        return stageGrowing;
    }

    public String getMessageNoBoneMeal() {
        return messageNoBoneMeal;
    }

    public String getMessageNoHoe() {
        return messageNoHoe;
    }

    public String getMessageToggleOn() {
        return messageToggleOn;
    }

    public String getMessageToggleOff() {
        return messageToggleOff;
    }

    public String getMessageReloadSuccess() {
        return messageReloadSuccess;
    }

    public String getMessageNoPermission() {
        return messageNoPermission;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static String translate(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
