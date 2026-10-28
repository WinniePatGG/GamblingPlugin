package de.winniepat.gamblingPlugin.util;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Small helpers for building consistent casino GUI items. */
public final class CasinoItems {

    private CasinoItems() {
    }

    public static ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack pane(Material material, String name) {
        return item(material, name == null || name.isEmpty() ? " " : name);
    }

    public static void fillAll(Inventory inv, Material material) {
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, pane(material, " "));
        }
    }

    public static void fill(Inventory inv, int[] slots, Material material) {
        for (int slot : slots) {
            inv.setItem(slot, pane(material, " "));
        }
    }

    /** Formats an amount like $1,234.50, $12.3k or $1.2M for GUI items. */
    public static String money(double amount) {
        if (Math.abs(amount) >= 1000000) {
            return "$" + String.format(Locale.US, "%.2fM", amount / 1000000.0);
        }
        if (Math.abs(amount) >= 10000) {
            return "$" + String.format(Locale.US, "%.1fk", amount / 1000.0);
        }
        return "$" + String.format(Locale.US, "%.2f", amount);
    }

    /** Formats a multiplier like 1.25x. */
    public static String multiplier(double multiplier) {
        return String.format(Locale.US, "%.2f", multiplier) + "x";
    }

    public static List<String> lore(String... lines) {
        return Arrays.asList(lines);
    }
}
