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
}