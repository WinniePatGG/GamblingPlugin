package de.winniepat.gamblingPlugin.util;

import de.winniepat.gamblingPlugin.GamblingPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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

    public synchronized void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException ignored) {
            }
        }
        data = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save playerdata.yml: " + e.getMessage());
        }
    }

    public synchronized String getLastDailyClaim(UUID uuid) {
        return data.getString("daily." + uuid, "");
    }

    public synchronized void setLastDailyClaim(UUID uuid, String day) {
        data.set("daily." + uuid, day);
    }

    public synchronized int getSlotStreak(UUID uuid) {
        return data.getInt("slot-streaks." + uuid, 0);
    }

    public synchronized void setSlotStreak(UUID uuid, int value) {
        if (value <= 0) {
            data.set("slot-streaks." + uuid, null);
        } else {
            data.set("slot-streaks." + uuid, value);
        }
    }

    public synchronized String getLastFreeSpin(UUID uuid) {
        return data.getString("free-spins." + uuid, "");
    }

    public synchronized void setLastFreeSpin(UUID uuid, String day) {
        data.set("free-spins." + uuid, day);
    }

    public synchronized void addWin(UUID uuid, String name, double amount) {
        String base = "stats.won." + uuid;
        data.set(base, data.getDouble(base, 0.0) + amount);
        data.set("stats.names." + uuid, name);
    }

    public synchronized void recordRound(UUID uuid, String name, double wagered, double returned) {
        String base = "stats.casino." + uuid;
        double netProfit = returned - wagered;
        data.set(base + ".games-played", data.getInt(base + ".games-played", 0) + 1);
        data.set(base + ".total-wagered", data.getDouble(base + ".total-wagered", 0.0) + wagered);
        data.set(base + ".total-returned", data.getDouble(base + ".total-returned", 0.0) + returned);
        data.set(base + ".net-profit", data.getDouble(base + ".net-profit", 0.0) + netProfit);
        data.set(base + ".largest-win", Math.max(data.getDouble(base + ".largest-win", 0.0), netProfit));
        data.set("stats.names." + uuid, name);
        save();
    }

    public synchronized CasinoStats getCasinoStats(UUID uuid) {
        String base = "stats.casino." + uuid;
        return new CasinoStats(
                data.getInt(base + ".games-played", 0),
                data.getDouble(base + ".total-wagered", 0.0),
                data.getDouble(base + ".total-returned", 0.0),
                data.getDouble(base + ".net-profit", 0.0),
                data.getDouble(base + ".largest-win", 0.0));
    }

    public synchronized Map<UUID, Double> getWins() {
        Map<UUID, Double> result = new HashMap<>();
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

    public synchronized String getName(UUID uuid) {
        return data.getString("stats.names." + uuid, uuid.toString().substring(0, 8));
    }

    public static final class CasinoStats {
        private final int gamesPlayed;
        private final double totalWagered;
        private final double totalReturned;
        private final double netProfit;
        private final double largestWin;

        public CasinoStats(int gamesPlayed, double totalWagered, double totalReturned,
                           double netProfit, double largestWin) {
            this.gamesPlayed = gamesPlayed;
            this.totalWagered = totalWagered;
            this.totalReturned = totalReturned;
            this.netProfit = netProfit;
            this.largestWin = largestWin;
        }

        public int getGamesPlayed() {
            return gamesPlayed;
        }

        public double getTotalWagered() {
            return totalWagered;
        }

        public double getTotalReturned() {
            return totalReturned;
        }

        public double getNetProfit() {
            return netProfit;
        }

        public double getLargestWin() {
            return largestWin;
        }
    }
}