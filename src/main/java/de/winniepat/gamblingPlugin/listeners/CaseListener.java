package de.winniepat.gamblingPlugin.listeners;

import de.winniepat.gamblingPlugin.animations.CaseAnimation;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryView;

public class CaseListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (isCaseView(e.getView(), playerOf(e.getWhoClicked()))) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (isCaseView(e.getView(), playerOf(e.getWhoClicked()))) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        CaseAnimation.onCaseClosed(playerOf(e.getPlayer()), e.getView().getTopInventory());
    }

    private Player playerOf(HumanEntity entity) {
        return entity instanceof Player ? (Player) entity : null;
    }

    private boolean isCaseView(InventoryView view, Player player) {
        return CaseAnimation.isCasePlayer(player)
                || CaseAnimation.isCaseInventory(view.getTopInventory())
                || view.getTitle().equals("§bOpening Case...");
    }
}
