package de.winniepat.gamblingPlugin.animations;

import de.winniepat.gamblingPlugin.GamblingPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CaseAnimation {

    private static final Set<Inventory> OPEN_INVENTORIES = new HashSet<>();
    private static final Set<UUID> OPEN_PLAYERS = new HashSet<>();

    public static boolean isCaseInventory(Inventory inventory) {
        return inventory != null && OPEN_INVENTORIES.contains(inventory);
    }

    public static boolean isCasePlayer(Player player) {
        return player != null && OPEN_PLAYERS.contains(player.getUniqueId());
    }

    public static void onCaseOpened(Player player, Inventory inventory) {
        if (player != null) {
            OPEN_PLAYERS.add(player.getUniqueId());
        }
        if (inventory != null) {
            OPEN_INVENTORIES.add(inventory);
        }
    }

    public static void onCaseClosed(Player player, Inventory inventory) {
        if (player != null) {
            OPEN_PLAYERS.remove(player.getUniqueId());
        }
        if (inventory != null) {
            OPEN_INVENTORIES.remove(inventory);
        }
    }

    private final Player player;
    private final Inventory inv;
    private final List<ItemStack> possibleRewards;
    private final Random random = new Random();

    private final int[] slots = {9, 10, 11, 12, 13, 14, 15, 16, 17};
    private final List<ItemStack> currentScroll = new ArrayList<>();
    private int shiftCount = 0;
    private boolean rewardGiven = false;

    public CaseAnimation(Player player) {
        this.player = player;
        this.inv = Bukkit.createInventory(null, 3 * 9, "§bOpening Case...");
        this.possibleRewards = getPossibleRewards();
        onCaseOpened(player, inv);
    }

    public void start() {
        player.openInventory(inv);

        inv.setItem(4, createReward(Material.GRAY_STAINED_GLASS_PANE, "§a▼"));
        inv.setItem(22, createReward(Material.GRAY_STAINED_GLASS_PANE, "§a▲"));

        for (int i = 0; i < slots.length + 10; i++) {
            currentScroll.add(getRandomReward());
        }

        runAnimation(0);
    }

    private void runAnimation(int shift) {
        if (shift >= currentScroll.size() - slots.length) {
            if (!rewardGiven) {
                ItemStack reward = inv.getItem(13);
                if (reward == null) {
                    player.sendMessage("§cError: could not retrieve the case reward.");
                } else {
                    inv.clear(13);
                    Map<Integer, ItemStack> leftover = player.getInventory().addItem(reward);
                    if (!leftover.isEmpty()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover.get(0));
                        player.sendMessage("§cYour inventory was full! The reward was dropped on the ground.");
                    }
                    player.sendMessage("§aYou won: §e" + reward.getItemMeta().getDisplayName());
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.2f, 1.0f);
                }
                for (int slot : slots) {
                    inv.clear(slot);
                }
                inv.clear(4);
                inv.clear(22);
                rewardGiven = true;
                Bukkit.getScheduler().runTaskLater(GamblingPlugin.getInstance(), () -> {
                    if (player.getOpenInventory().getTopInventory() == inv) {
                        player.closeInventory();
                    }
                }, 30L);
            }
            return;
        }

        updateScroll(shift);

        long delay = Math.min(10 + shift * 2, 60);

        new BukkitRunnable() {
            @Override
            public void run() {
                runAnimation(shift + 1);
            }
        }.runTaskLater(GamblingPlugin.getInstance(), delay / 2);
    }

    private void updateScroll(int shift) {
        for (int i = 0; i < slots.length; i++) {
            int scrollIndex = shift + i;
            if (scrollIndex < currentScroll.size()) {
                inv.setItem(slots[i], currentScroll.get(scrollIndex));
            }
        }
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.5f);
    }


    private void updateScroll() {
        for (int i = 0; i < slots.length; i++) {
            int scrollIndex = shiftCount + i;
            if (scrollIndex < currentScroll.size()) {
                inv.setItem(slots[i], currentScroll.get(scrollIndex));
            }
        }

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.5f);
    }

    private ItemStack getRandomReward() {
        return possibleRewards.get(random.nextInt(possibleRewards.size())).clone();
    }

    private List<ItemStack> getPossibleRewards() {
        List<ItemStack> list = new ArrayList<>();

        list.add(createReward(Material.DIAMOND, "§bDiamond"));
        list.add(createReward(Material.EMERALD, "§aEmerald"));
        list.add(createReward(Material.NETHERITE_SCRAP, "§8Netherite Scrap"));
        list.add(createReward(Material.GOLD_INGOT, "§6Gold Ingot"));
        list.add(createReward(Material.IRON_INGOT, "§fIron Ingot"));
        list.add(createReward(Material.TOTEM_OF_UNDYING, "§eTotem of Undying"));
        list.add(createReward(Material.ENCHANTED_GOLDEN_APPLE, "§dEnchanted Apple"));

        return list;
    }

    private ItemStack createReward(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
