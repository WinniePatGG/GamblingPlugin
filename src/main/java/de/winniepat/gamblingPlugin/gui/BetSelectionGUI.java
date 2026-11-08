package de.winniepat.gamblingPlugin.gui;

import de.winniepat.gamblingPlugin.util.CasinoItems;
import de.winniepat.gamblingPlugin.util.GameOpener;
import de.winniepat.gamblingPlugin.GamblingPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.util.Arrays;
import java.util.List;

public class BetSelectionGUI implements Listener {
    public static final String TITLE = "§6§lChoose Bet";
    private static final int GAME_TITLE_SLOT = 4;
    private static final int FIRST_BET_SLOT = 10;
    private static final List<Double> AMOUNTS = Arrays.asList(10.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0);

    public static void open(Player player, String game) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        CasinoItems.fillAll(inv, Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < AMOUNTS.size(); i++) {
            double amount = AMOUNTS.get(i);
            inv.setItem(FIRST_BET_SLOT + i, CasinoItems.item(Material.GOLD_NUGGET,
                    "§eBet " + CasinoItems.money(amount),
                    amount <= GamblingPlugin.getInstance().getEconomy().getBalance(player)
                            ? "§7Click to play." : "§cYou cannot afford this bet."));
        }
        inv.setItem(22, CasinoItems.item(Material.BARRIER, "§cCancel"));
        inv.setItem(GAME_TITLE_SLOT, CasinoItems.item(Material.PAPER, "§f" + game + " §7- choose your wager"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!TITLE.equals(de.winniepat.gamblingPlugin.util.ViewTitles.title(event.getView()))) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)
                || event.getClickedInventory() != event.getView().getTopInventory()) return;
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        if (slot == 22) {
            player.closeInventory();
            return;
        }
        int index = slot - FIRST_BET_SLOT;
        if (index < 0 || index >= AMOUNTS.size()) return;
        double amount = AMOUNTS.get(index);
        if (GamblingPlugin.getInstance().getEconomy().getBalance(player) < amount) {
            player.sendMessage("§cYou cannot afford that bet.");
            return;
        }
        String game = "menu";
        if (event.getView().getTopInventory().getItem(GAME_TITLE_SLOT) != null
                && event.getView().getTopInventory().getItem(GAME_TITLE_SLOT).hasItemMeta()) {
            String display = event.getView().getTopInventory().getItem(GAME_TITLE_SLOT).getItemMeta().getDisplayName();
            game = display.substring(2, display.indexOf(" §7"));
        }
        player.closeInventory();
        GameOpener.openGame(player, game, amount);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (TITLE.equals(de.winniepat.gamblingPlugin.util.ViewTitles.title(event.getView()))) {
            event.setCancelled(true);
        }
    }
}
