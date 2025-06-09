package de.winniepat.gamblingPlugin.listeners;

import de.winniepat.gamblingPlugin.GamblingPlugin;
import de.winniepat.gamblingPlugin.gui.RouletteGUI;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class RouletteListener implements Listener {

    private final GamblingPlugin plugin;
    private final Random random = new Random();
    private final double betCost = 50.0;

    public RouletteListener(GamblingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null || event.getCurrentItem() == null) return;

        String title = event.getView().getTitle();
        ItemStack clicked = event.getCurrentItem();
        if (event.getView().getTitle().startsWith("Roulette") && event.getClickedInventory() == event.getView().getTopInventory()) {
            event.setCancelled(true);
        }

        if (title.equals("Choose a Color to Bet")) {
            Material chosen = clicked.getType();

            if (GamblingPlugin.getInstance().getEconomy().getBalance(player) < betCost) {
                player.sendMessage("§cYou don't have enough money to bet! §7(Required: $" + betCost + ")");
                return;
            }

            GamblingPlugin.getInstance().getEconomy().withdrawPlayer(player, betCost);
            player.sendMessage("§7Bet placed: §6$" + betCost);

            player.openInventory(RouletteGUI.createGameGUI(chosen));
            spinRoulette(player, chosen);
        }
    }

    private void spinRoulette(Player player, Material betColor) {
        Inventory inv = player.getOpenInventory().getTopInventory();

        ItemStack[] originalItems = new ItemStack[RouletteGUI.EDGE_SLOTS.length];
        for (int i = 0; i < RouletteGUI.EDGE_SLOTS.length; i++) {
            originalItems[i] = inv.getItem(RouletteGUI.EDGE_SLOTS[i]);
        }

        final int totalSpins = 40 + random.nextInt(30); // random spin length between 40–69

        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (index >= totalSpins) {
                    cancel();
                    int finalIndex = (index - 1) % RouletteGUI.EDGE_SLOTS.length;
                    int finalSlot = RouletteGUI.EDGE_SLOTS[finalIndex];

                    inv.setItem(finalSlot, originalItems[finalIndex]);
                    Material result = originalItems[finalIndex].getType();

                    if (result == betColor) {
                        int multiplier = switch (result) {
                            case GREEN_WOOL -> 14;
                            case RED_WOOL, BLACK_WOOL -> 2;
                            default -> 0;
                        };
                        double winnings = betCost * multiplier;
                        GamblingPlugin.getInstance().getEconomy().depositPlayer(player, winnings);

                        player.sendMessage("§aYou won! Multiplier: " + multiplier + "x | §a$" + winnings);
                    } else {
                        player.sendMessage("§cYou lost. It landed on " + result.name());
                    }

                    Sound loseSound = (result == betColor)
                            ? Sound.ENTITY_EXPERIENCE_ORB_PICKUP
                            : Sound.BLOCK_NOTE_BLOCK_BASS;
                    player.playSound(player.getLocation(), loseSound, 1f, result == betColor ? 1f : 0.5f);
                    return;
                }

                int currentIndex = index % RouletteGUI.EDGE_SLOTS.length;
                int currentSlot = RouletteGUI.EDGE_SLOTS[currentIndex];

                inv.setItem(currentSlot, RouletteGUI.createItem(Material.YELLOW_STAINED_GLASS_PANE, "§eSpinning..."));

                if (index > 0) {
                    int prevIndex = (index - 1) % RouletteGUI.EDGE_SLOTS.length;
                    int prevSlot = RouletteGUI.EDGE_SLOTS[prevIndex];
                    inv.setItem(prevSlot, originalItems[prevIndex]);
                }

                index++;

                // Play tick sound
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 2f);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
    @EventHandler
        public void onBlockInteract(PlayerInteractEvent event) {
            if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
            if (event.getClickedBlock() == null) return;

            Block block = event.getClickedBlock();
            Player player = event.getPlayer();

            if (block.getType() == Material.STRUCTURE_BLOCK) {
                if (player.isSneaking()) {
                    if (player.isOp()) {
                        block.breakNaturally(player.getInventory().getItemInMainHand());
                    } else {
                        event.setCancelled(true);
                        player.sendMessage("§cOnly Staff can break Roulette Tables!");
                    }
                } else {
                    event.setCancelled(true);
                    player.openInventory(RouletteGUI.createBettingGUI());
                }
            }
        }
}