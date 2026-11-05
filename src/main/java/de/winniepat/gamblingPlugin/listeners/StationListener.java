package de.winniepat.gamblingPlugin.listeners;

import de.winniepat.gamblingPlugin.GamblingPlugin;
import de.winniepat.gamblingPlugin.util.GameOpener;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class StationListener implements Listener {

    private String stationGame(Material type) {
        if (type == null) return null;
        ConfigurationSection stations = GamblingPlugin.getInstance().getConfig().getConfigurationSection("stations");
        if (stations == null) {
            return type == Material.STRUCTURE_BLOCK ? "roulette" : null;
        }
        for (String game : stations.getKeys(false)) {
            String value = stations.getString(game, "");
            if (value == null || value.isEmpty()) continue;
            Material mapped = Material.matchMaterial(value);
            if (mapped != null && mapped == type) {
                return game.toLowerCase();
            }
        }
        return null;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();
        String game = stationGame(block.getType());
        if (game == null) return;

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (!player.isOp()) {
                event.setCancelled(true);
                player.sendMessage("§cOnly staff can break casino stations!");
            }
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (player.isSneaking()) return;

        event.setCancelled(true);
        GameOpener.openGame(player, game);
    }
}
