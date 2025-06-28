package de.winniepat.gamblingPlugin.gui;

import de.winniepat.gamblingPlugin.GamblingPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RouletteGUI implements Listener {

    public static final int[] EDGE_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            17, 26, 35, 44, 53,
            52, 51, 50, 49, 48, 47, 46, 45,
            36, 27, 18, 9
    };

    public static final Set<UUID> spinningPlayers = new HashSet<>();

    public static Inventory createBettingGUI() {
        Inventory gui = Bukkit.createInventory(null, 27, "Choose a Color to Bet");

        gui.setItem(11, createItem(Material.RED_WOOL, "§cBet on Red"));
        gui.setItem(13, createItem(Material.GREEN_WOOL, "§aBet on Green"));
        gui.setItem(15, createItem(Material.BLACK_WOOL, "§8Bet on Black"));

        return gui;
    }

    public static Inventory createGameGUI(Material betColor, Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "Roulette: " + betColor.name());

        Material[] colors = new Material[] {
                Material.RED_WOOL, Material.BLACK_WOOL,
                Material.RED_WOOL, Material.BLACK_WOOL,
                Material.RED_WOOL, Material.BLACK_WOOL,
                Material.RED_WOOL, Material.BLACK_WOOL,
                Material.GREEN_WOOL
        };

        int colorIndex = 0;
        for (int slot : EDGE_SLOTS) {
            Material color = colors[colorIndex % colors.length];
            gui.setItem(slot, createItem(color, " "));
            colorIndex++;
        }

        spinningPlayers.add(player.getUniqueId());

        return gui;
    }

    public static ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().startsWith("Roulette: ")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Player player = (Player) e.getPlayer();
        String title = e.getView().getTitle();

        if (title.startsWith("Roulette: ") && spinningPlayers.contains(player.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(GamblingPlugin.getInstance(), () -> {
                player.openInventory(e.getInventory());
                player.sendMessage("§cYou can't close the roulette while it's spinning!");
            }, 2L);
        } else {
            spinningPlayers.remove(player.getUniqueId());
        }
    }
}
