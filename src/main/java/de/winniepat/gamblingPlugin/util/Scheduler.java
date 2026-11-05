package de.winniepat.gamblingPlugin.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public final class Scheduler {

    private Scheduler() {
    }

    private static final boolean FOLIA = detectFolia();

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    public static void runForPlayer(Plugin plugin, Player player, long delayTicks, Runnable task) {
        if (FOLIA) {
            player.getScheduler().runDelayed(plugin, t -> task.run(), null, delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public static void runGlobal(Plugin plugin, long delayTicks, Runnable task) {
        if (FOLIA) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> task.run(), Math.max(0, delayTicks));
        } else {
            if (delayTicks <= 0) {
                Bukkit.getScheduler().runTask(plugin, task);
            } else {
                Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
            }
        }
    }

    public static void runAsync(Plugin plugin, Runnable task) {
        if (FOLIA) {
            Bukkit.getAsyncScheduler().runDelayed(plugin, t -> task.run(), 0, TimeUnit.MILLISECONDS);
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }
}
