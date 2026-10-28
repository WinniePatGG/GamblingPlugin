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

/** Event wiring for {@link CoinflipGame}. */
public class CoinflipGUI implements Listener {

    private static final Map<UUID, CoinflipGame> games = new HashMap<>();

    public static void open(Player player) {
        CoinflipGame existing = games.get(player.getUniqueId());
        if (existing != null) {
            if (existing.isFlipping()) {
                player.sendMessage("§cThe coin is still flipping!");
                return;
            }
            player.openInventory(existing.inventory());
            return;
        }

        CoinflipGame game = new CoinflipGame(player);
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
        if (!de.winniepat.gamblingPlugin.util.ViewTitles.title(event.getView()).equals(CoinflipGame.TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        Player player = (Player) event.getWhoClicked();
        CoinflipGame game = games.get(player.getUniqueId());
        if (game == null) return;

        int slot = event.getSlot();
        if (slot == CoinflipGame.SLOT_HEADS) {
            game.pick(true);
        } else if (slot == CoinflipGame.SLOT_TAILS) {
            game.pick(false);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (de.winniepat.gamblingPlugin.util.ViewTitles.title(event.getView()).equals(CoinflipGame.TITLE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        if (!de.winniepat.gamblingPlugin.util.ViewTitles.title(event.getView()).equals(CoinflipGame.TITLE)) return;
        Player player = (Player) event.getPlayer();
        CoinflipGame game = games.get(player.getUniqueId());
        if (game != null) {
            game.forceResolveOnClose();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        CoinflipGame game = games.remove(player.getUniqueId());
        ActiveGames.leave(player.getUniqueId());
        if (game != null) {
            game.forceResolveOnClose();
        }
    }
}
