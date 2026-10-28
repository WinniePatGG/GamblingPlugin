package de.winniepat.gamblingPlugin.gui;

import de.winniepat.gamblingPlugin.GamblingPlugin;
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

/** Event wiring for {@link BlackjackGame}. */
public class BlackjackGUI implements Listener {

    private static final Map<UUID, BlackjackGame> games = new HashMap<>();

    public static void open(Player player) {
        BlackjackGame existing = games.get(player.getUniqueId());
        if (existing != null) {
            // Re-show the table instead of starting a duplicate round.
            player.openInventory(existing.inventory());
            return;
        }

        double bet = BlackjackGame.betCost();
        if (GamblingPlugin.getInstance().getEconomy().getBalance(player) < bet) {
            player.sendMessage("§cYou need at least §e" + de.winniepat.gamblingPlugin.util.CasinoItems.money(bet)
                    + " §cto play blackjack.");
            return;
        }
        GamblingPlugin.getInstance().getEconomy().withdrawPlayer(player, bet);

        BlackjackGame game = new BlackjackGame(player, bet);
        games.put(player.getUniqueId(), game);
        game.start();
    }

    static void forget(UUID uuid) {
        games.remove(uuid);
    }

    public static void clearState() {
        games.clear();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!de.winniepat.gamblingPlugin.util.ViewTitles.title(event.getView()).equals(BlackjackGame.TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        Player player = (Player) event.getWhoClicked();
        BlackjackGame game = games.get(player.getUniqueId());
        if (game == null || game.isResolved()) return;

        int slot = event.getSlot();
        if (slot == BlackjackGame.SLOT_HIT) {
            game.hit();
        } else if (slot == BlackjackGame.SLOT_DOUBLE) {
            game.doubleDown();
        } else if (slot == BlackjackGame.SLOT_STAND) {
            game.stand();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (de.winniepat.gamblingPlugin.util.ViewTitles.title(event.getView()).equals(BlackjackGame.TITLE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        if (!de.winniepat.gamblingPlugin.util.ViewTitles.title(event.getView()).equals(BlackjackGame.TITLE)) return;
        Player player = (Player) event.getPlayer();
        BlackjackGame game = games.get(player.getUniqueId());
        if (game != null) {
            game.forceStandOnClose();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        BlackjackGame game = games.remove(player.getUniqueId());
        ActiveGames.leave(player.getUniqueId());
        if (game != null) {
            game.forceStandOnClose();
        }
    }
}
