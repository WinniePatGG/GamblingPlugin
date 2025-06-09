package de.winniepat.gamblingPlugin.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class RouletteGUI {

    public static final int[] EDGE_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            17, 26, 35, 44, 53,
            52, 51, 50, 49, 48, 47, 46, 45,
            36, 27, 18, 9
    };

    public static Inventory createBettingGUI() {
        Inventory gui = Bukkit.createInventory(null, 27, "Choose a Color to Bet");

        gui.setItem(11, createItem(Material.RED_WOOL, "§cBet on Red"));
        gui.setItem(13, createItem(Material.GREEN_WOOL, "§aBet on Green"));
        gui.setItem(15, createItem(Material.BLACK_WOOL, "§8Bet on Black"));

        return gui;
    }

    public static Inventory createGameGUI(Material betColor) {
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
}