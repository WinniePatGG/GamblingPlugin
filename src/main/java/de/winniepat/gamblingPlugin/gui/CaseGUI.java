package de.winniepat.gamblingPlugin.gui;

import de.winniepat.gamblingPlugin.GamblingPlugin;
import de.winniepat.gamblingPlugin.animations.CaseAnimation;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class CaseGUI implements Listener {

    private final Player player;
    private final Inventory inv;
    private final Set<UUID> cooldown = new HashSet<>();

    private final double caseCost = 100.0;

    public CaseGUI(Player player) {
        this.player = player;
        this.inv = Bukkit.createInventory(null, 3 * 9, "§8Case");

        setupInventory();
        player.openInventory(inv);
        Bukkit.getPluginManager().registerEvents(this, GamblingPlugin.getInstance());
    }

    private void setupInventory() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName("§7");
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        ItemStack openButton = new ItemStack(Material.ENDER_CHEST);
        ItemMeta openMeta = openButton.getItemMeta();
        if (openMeta != null) {
            openMeta.setDisplayName("§aOpen Case");
            openMeta.setLore(Collections.singletonList("§7Cost: §e" + caseCost));
            openButton.setItemMeta(openMeta);
        }

        inv.setItem(13, openButton);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§8Case")) return;
        if (event.getCurrentItem() == null) return;
        if (event.getSlot() != 13) return;
        if (event.getClick() != ClickType.LEFT) return;
        if (!(event.getWhoClicked() instanceof Player clickedPlayer)) return;

        event.setCancelled(true);

        if (!clickedPlayer.getUniqueId().equals(player.getUniqueId())) return;

        if (cooldown.contains(player.getUniqueId())) {
            player.sendMessage("§cPlease wait before opening another case.");
            return;
        }

        Economy economy = GamblingPlugin.getInstance().getEconomy();
        if (economy == null) {
            player.sendMessage("§cEconomy system not found.");
            return;
        }

        double balance = economy.getBalance(player);
        if (balance < caseCost) {
            player.sendMessage("§cYou don't have enough money! §7(Need §e" + caseCost + "§7)");
            return;
        }

        // Charge the player
        economy.withdrawPlayer(player, caseCost);
        player.sendMessage("§aYou paid §e" + caseCost + "§a to open a case.");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

        cooldown.add(player.getUniqueId());
        Bukkit.getScheduler().runTaskLater(GamblingPlugin.getInstance(), () -> cooldown.remove(player.getUniqueId()), 40L);

        // Run your open animation (💥 this is the new part)
        new CaseAnimation(player).start();
    }
}
