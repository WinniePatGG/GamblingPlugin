package de.winniepat.gamblingPlugin.gui;

import de.winniepat.gamblingPlugin.GamblingPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class DoubleGUI implements Listener {

    private static final Map<UUID, Integer> playerStage = new HashMap<>();
    private static final List<Integer> amounts = Arrays.asList(1, 2, 4, 8, 16, 32, 64, 128);
    private static final Set<UUID> openingNow = new HashSet<>();

    public static void open(Player player) {
        UUID uuid = player.getUniqueId();

        if (openingNow.contains(uuid)) return;
        openingNow.add(uuid);
        Bukkit.getScheduler().runTaskLater(GamblingPlugin.getInstance(), () -> openingNow.remove(uuid), 10L);

        if (GamblingPlugin.getInstance().getEconomy().getBalance(player) < 20) {
            player.sendMessage("§cYou need at least $20 to play Double or Nothing.");
            return;
        }

        GamblingPlugin.getInstance().getEconomy().withdrawPlayer(player, 20);
        player.sendMessage("§7You paid §c$20 §7to play.");

        playerStage.put(uuid, 0);
        openGui(player);
    }

    private static void openGui(Player player) {
        UUID uuid = player.getUniqueId();
        int stage = playerStage.getOrDefault(uuid, 0);
        int amount = amounts.get(stage);

        Inventory gui = Bukkit.createInventory(null, 9, "§aDouble or Nothing");

        ItemStack green = new ItemStack(Material.LIME_WOOL);
        ItemMeta greenMeta = green.getItemMeta();
        greenMeta.setDisplayName("§aDouble to $" + (stage < amounts.size() - 1 ? amounts.get(stage + 1) : "MAX"));
        green.setItemMeta(greenMeta);
        gui.setItem(2, green);

        ItemStack center = new ItemStack(Material.PAPER);
        ItemMeta centerMeta = center.getItemMeta();
        centerMeta.setDisplayName("§fCurrent: $" + amount);
        center.setItemMeta(centerMeta);
        gui.setItem(4, center);

        ItemStack red = new ItemStack(Material.RED_WOOL);
        ItemMeta redMeta = red.getItemMeta();
        redMeta.setDisplayName("§cTake $" + amount);
        red.setItemMeta(redMeta);
        gui.setItem(6, red);

        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals("§aDouble or Nothing")) return;
        e.setCancelled(true);

        Player player = (Player) e.getWhoClicked();
        UUID uuid = player.getUniqueId();
        int stage = playerStage.getOrDefault(uuid, 0);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = clicked.getItemMeta().getDisplayName();

        if (name.contains("Double")) {
            if (new Random().nextBoolean()) {
                if (stage >= amounts.size() - 1) {
                    int finalAmount = amounts.get(stage);
                    GamblingPlugin.getInstance().getEconomy().depositPlayer(player, finalAmount);
                    player.sendMessage("§aYou reached max and won $" + finalAmount + "!");
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                    player.closeInventory();
                    playerStage.remove(uuid);
                } else {
                    playerStage.put(uuid, stage + 1);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
                    openGui(player);
                }
            } else {
                player.sendMessage("§cYou lost everything!");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.4f);
                player.closeInventory();
                playerStage.remove(uuid);
            }
        }

        if (name.contains("Take")) {
            int winnings = amounts.get(stage);
            GamblingPlugin.getInstance().getEconomy().depositPlayer(player, winnings);
            player.sendMessage("§aYou took $" + winnings + " and walked away!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
            player.closeInventory();
            playerStage.remove(uuid);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("§aDouble or Nothing")) {
            e.setCancelled(true);
        }
    }
}
