package de.winniepat.gamblingPlugin.gui.customui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null) {
            return;
        }

        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        holder.execute(event.getRawSlot(), player);
    }
}
