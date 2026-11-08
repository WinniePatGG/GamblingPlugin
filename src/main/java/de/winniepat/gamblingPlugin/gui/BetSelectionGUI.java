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

import java.util.List;

public class BetSelectionGUI implements Listener {
    public static final String TITLE = "§6§lChoose Bet";
    private static final int TITLE_SLOT = 4;

    public static void open(Player player, String game) {
        List<Double> amounts = GamblingPlugin.getInstance().getBetAmounts();
        Inventory inv = Bukkit.createInventory(null, inventorySize(amounts.size()), TITLE);
        CasinoItems.fillAll(inv, Material.GRAY_STAINED_GLASS_PANE);
        List<Integer> amountSlots = amountSlots(amounts.size());
        for (int i = 0; i < amounts.size(); i++) {
            double amount = amounts.get(i);
            inv.setItem(amountSlots.get(i), CasinoItems.item(Material.GOLD_NUGGET,
                    "§eBet " + CasinoItems.money(amount),
                    amount <= GamblingPlugin.getInstance().getEconomy().getBalance(player)
                            ? "§7Click to play." : "§cYou cannot afford this bet."));
        }
        inv.setItem(cancelSlot(inv), CasinoItems.item(Material.BARRIER, "§cCancel"));
        inv.setItem(TITLE_SLOT, CasinoItems.item(Material.PAPER, "§f" + game + " §7- choose your wager"));
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
        if (slot == cancelSlot(event.getView().getTopInventory())) {
            player.closeInventory();
            return;
        }
        List<Integer> amountSlots = amountSlotsForInventory(event.getView().getTopInventory().getSize());
        int index = amountSlots.indexOf(slot);
        List<Double> amounts = GamblingPlugin.getInstance().getBetAmounts();
        if (index < 0 || index >= amounts.size()) return;
        double amount = amounts.get(index);
        if (GamblingPlugin.getInstance().getEconomy().getBalance(player) < amount) {
            player.sendMessage("§cYou cannot afford that bet.");
            return;
        }
        String game = "menu";
        if (event.getView().getTopInventory().getItem(TITLE_SLOT) != null
                && event.getView().getTopInventory().getItem(TITLE_SLOT).hasItemMeta()) {
            String display = event.getView().getTopInventory().getItem(TITLE_SLOT).getItemMeta().getDisplayName();
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

    private static int inventorySize(int amountCount) {
        int amountRows = (amountCount + 6) / 7;
        return Math.max(27, (amountRows + 1) * 9);
    }

    private static List<Integer> amountSlots(int amountCount) {
        int amountRows = (amountCount + 6) / 7;
        List<Integer> slots = new java.util.ArrayList<>(amountCount);
        for (int row = 0; row < amountRows; row++) {
            for (int column = 1; column <= 7 && slots.size() < amountCount; column++) {
                slots.add((row + 1) * 9 + column);
            }
        }
        return slots;
    }

    private static List<Integer> amountSlotsForInventory(int inventorySize) {
        int amountRows = inventorySize / 9 - 1;
        return amountSlots(amountRows * 7);
    }

    private static int cancelSlot(Inventory inventory) {
        return inventory.getSize() - 5;
    }
}
