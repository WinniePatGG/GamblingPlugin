package de.winniepat.gamblingPlugin.listeners;

import de.winniepat.gamblingPlugin.gui.SlotGUI;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class SlotListener implements Listener {

    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Block block = event.getClickedBlock();
        Player player = event.getPlayer();

        if (block.getType() == Material.JIGSAW) {
            if (player.isSneaking()) {
                if (player.isOp()) {
                    block.breakNaturally(player.getInventory().getItemInMainHand());
                } else {
                    event.setCancelled(true);
                    player.sendMessage("§cOnly Staff can break slot machines!");
                }
            } else {
                event.setCancelled(true);
                de.winniepat.gamblingPlugin.gui.BetSelectionGUI.open(event.getPlayer(), "slots");
            }
        }
    }
}
