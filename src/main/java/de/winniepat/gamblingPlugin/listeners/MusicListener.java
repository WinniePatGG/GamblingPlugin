package de.winniepat.gamblingPlugin.listeners;

import de.winniepat.gamblingPlugin.GamblingPlugin;
import de.winniepat.gamblingPlugin.util.MusicManager;
import de.winniepat.gamblingPlugin.util.Scheduler;
import de.winniepat.gamblingPlugin.util.ViewTitles;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MusicListener implements Listener {

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        MusicManager.onOpenView(player, ViewTitles.title(event.getView()));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Scheduler.runForPlayer(GamblingPlugin.getInstance(), player, 4L, () -> MusicManager.reconcile(player));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        MusicManager.forget(event.getPlayer().getUniqueId());
    }
}
