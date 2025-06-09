package de.winniepat.gamblingPlugin.listeners;

import de.winniepat.gamblingPlugin.GamblingPlugin;
import de.winniepat.gamblingPlugin.gui.CaseGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;

public class CaseListener implements Listener {

    private final GamblingPlugin plugin;

    public CaseListener(GamblingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;

        if (event.getClickedBlock().getType() == Material.EMERALD_BLOCK) {
            Player player = event.getPlayer();
            new CaseGUI(event.getPlayer());
            event.setCancelled(true);
        }
    }
}
