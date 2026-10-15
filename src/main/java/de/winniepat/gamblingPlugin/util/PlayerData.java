package de.winniepat.gamblingPlugin.util;

import de.winniepat.gamblingPlugin.GamblingPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class PlayerData {

    private final GamblingPlugin plugin;
    private final File file;
    private FileConfiguration data;

    public PlayerData(GamblingPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "playerdata.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException ignored) {
            }
        }
        data = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save playerdata.yml: " + e.getMessage());
        }
    }

    public String getLastDailyClaim(UUID uuid) {
        return data.getString("daily." + uuid, "");
    }

    public void setLastDailyClaim(UUID uuid, String day) {
        data.set("daily." + uuid, day);
    }

    public int getSlotStreak(UUID uuid) {
        return data.getInt("slot-streaks." + uuid, 0);
    }

    public void setSlotStreak(UUID uuid, int value) {
        if (value <= 0) {
            data.set("slot-streaks." + uuid, null);
        } else {
            data.set("slot-streaks." + uuid, value);
        }
    }

    public String getLastFreeSpin(UUID uuid) {
        return data.getString("free-spins." + uuid, "");
    }

    public void setLastFreeSpin(UUID uuid, String day) {
        data.set("free-spins." + uuid, day);
    }

    public void addWin(UUID uuid, String name, double amount) {
        String base = "stats.won." + uuid;
        data.set(base, data.getDouble(base, 0.0) + amount);
        data.set("stats.names." + uuid, name);
    }

    public java.util.Map<UUID, Double> getWins() {
        java.util.Map<UUID, Double> result = new java.util.HashMap<>();
        org.bukkit.configuration.ConfigurationSection section = data.getConfigurationSection("stats.won");
        if (section == null) return result;
        for (String key : section.getKeys(false)) {
            try {
                result.put(UUID.fromString(key), section.getDouble(key));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }

    public String getName(UUID uuid) {
        return data.getString("stats.names." + uuid, uuid.toString().substring(0, 8));
    }
}