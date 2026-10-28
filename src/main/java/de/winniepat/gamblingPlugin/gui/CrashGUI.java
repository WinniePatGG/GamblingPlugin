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

/** Event wiring for {@link CrashGame}. */
public class CrashGUI implements Listener {

    private static final Map<UUID, CrashGame> games = new HashMap<>();

    public static void open(Player player) {
        CrashGame existing = games.get(player.getUniqueId());
        if (existing != null) {
            player.openInventory(existing.inventory());
            return;
        }

        double bet = CrashGame.betCost();
        if (de.winniepat.gamblingPlugin.GamblingPlugin.getInstance().getEconomy().getBalance(player) < bet) {
            player.sendMessage("§cYou need at least §e"
                    + de.winniepat.gamblingPlugin.util.CasinoItems.money(bet) + " §cto play crash.");
            return;
        }

        CrashGame game = new CrashGame(player, bet);
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
        if (!de.winniepat.gamblingPlugin.util.ViewTitles.title(event.getView()).equals(CrashGame.TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        Player player = (Player) event.getWhoClicked();
        CrashGame game = games.get(player.getUniqueId());
        if (game != null && event.getClickedInventory() != null
                && event.getSlot() == CrashGame.SLOT_CASHOUT) {
            game.cashOut();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (de.winniepat.gamblingPlugin.util.ViewTitles.title(event.getView()).equals(CrashGame.TITLE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        if (!de.winniepat.gamblingPlugin.util.ViewTitles.title(event.getView()).equals(CrashGame.TITLE)) return;
        Player player = (Player) event.getPlayer();
        CrashGame game = games.get(player.getUniqueId());
        if (game != null) {
            game.forceResolveOnClose();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        CrashGame game = games.remove(player.getUniqueId());
        ActiveGames.leave(player.getUniqueId());
        if (game != null) {
            game.forceResolveOnClose();
        }
    }
}
