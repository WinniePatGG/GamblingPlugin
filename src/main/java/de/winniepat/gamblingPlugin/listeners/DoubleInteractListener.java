package de.winniepat.gamblingPlugin.listeners;

import de.winniepat.gamblingPlugin.gui.DoubleGUI;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class DoubleInteractListener implements Listener {

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Block block = event.getClickedBlock();
        Player player = event.getPlayer();

        if (block.getType() == Material.DIAMOND_BLOCK) {
            if (player.isSneaking()) {
                if (player.isOp()) {
                    block.breakNaturally(player.getInventory().getItemInMainHand());
                } else {
                    event.setCancelled(true);
                    player.sendMessage("§cOnly Staff can break Double Or Nothing Machines!");
                }
            } else {
                event.setCancelled(true);
                DoubleGUI.open(player);
            }
        }
    }
}
