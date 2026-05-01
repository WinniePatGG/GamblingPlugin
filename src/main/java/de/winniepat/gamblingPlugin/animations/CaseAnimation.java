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

    private final Player player;
    private final Inventory inv;
    private final List<ItemStack> possibleRewards;
    private final Random random = new Random();

    private final int[] slots = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    private final List<ItemStack> currentScroll = new ArrayList<>();
    private int shiftCount = 0;
    private boolean rewardGiven = false;

    public CaseAnimation(Player player) {
        this.player = player;
        this.inv = Bukkit.createInventory(null, 3 * 9, "§bOpening Case...");
        this.possibleRewards = getPossibleRewards();
    }

    public void start() {
        player.openInventory(inv);

        for (int i = 0; i < slots.length + 10; i++) {
            currentScroll.add(getRandomReward());
        }

        runAnimation(0);
    }

    private void runAnimation(int shift) {
        if (shift >= currentScroll.size() - slots.length) {
            if (!rewardGiven) {
                ItemStack reward = inv.getItem(4);
                player.getInventory().addItem(reward);
                player.sendMessage("§aYou won: §e" + reward.getItemMeta().getDisplayName());
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.2f, 1.0f);
                rewardGiven = true;
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
