package de.winniepat.gamblingPlugin.gui;

import de.winniepat.gamblingPlugin.util.ActiveGames;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Event wiring for {@link MinesGame}. */
public class MinesGUI implements Listener {

    private static final Map<UUID, MinesGame> games = new HashMap<>();

    public static void open(Player player) {
        MinesGame existing = games.get(player.getUniqueId());
        if (existing != null) {
            player.openInventory(existing.inventory());
            return;
        }

        if (de.winniepat.gamblingPlugin.GamblingPlugin.getInstance().getEconomy().getBalance(player)
                < MinesGame.betCost()) {
            player.sendMessage("§cYou need at least §e"
                    + de.winniepat.gamblingPlugin.util.CasinoItems.money(MinesGame.betCost())
                    + " §cto play mines.");
            return;
        }

        MinesGame game = new MinesGame(player, MinesGame.betCost());
        games.put(player.getUniqueId(), game);
        game.start();
        player.openInventory(game.inventory());
    }

    static void forget(UUID uuid) {
        games.remove(uuid);
    }

    public static void clearState() {
        games.clear();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!de.winniepat.gamblingPlugin.util.ViewTitles.title(event.getView()).equals(MinesGame.TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        Player player = (Player) event.getWhoClicked();
        MinesGame game = games.get(player.getUniqueId());
        if (game == null) return;

        int slot = event.getSlot();
        if (slot == MinesGame.SLOT_CASHOUT) {
            game.cashOut();
            return;
        }
        if (slot == MinesGame.SLOT_CLOSE) {
            player.closeInventory();
            return;
        }
        if (slot == MinesGame.SLOT_PLAY_AGAIN) {
            game.playAgain();
            return;
        }
        for (int i = 0; i < MinesGame.GRID.length; i++) {
            if (MinesGame.GRID[i] == slot) {
                game.revealTile(i);
                return;
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (de.winniepat.gamblingPlugin.util.ViewTitles.title(event.getView()).equals(MinesGame.TITLE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        if (!de.winniepat.gamblingPlugin.util.ViewTitles.title(event.getView()).equals(MinesGame.TITLE)) return;
        Player player = (Player) event.getPlayer();
        MinesGame game = games.get(player.getUniqueId());
        if (game != null) {
            game.forceResolveOnClose();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        MinesGame game = games.remove(player.getUniqueId());
        ActiveGames.leave(player.getUniqueId());
        if (game != null) {
            game.forceResolveOnClose();
        }
    }
}
