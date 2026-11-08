package de.winniepat.gamblingPlugin.animations;

import de.winniepat.gamblingPlugin.GamblingPlugin;
import de.winniepat.gamblingPlugin.util.CasinoItems;
import de.winniepat.gamblingPlugin.util.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CaseAnimation {

    private static final Set<Inventory> OPEN_INVENTORIES = ConcurrentHashMap.newKeySet();

    public static void clearState() {
        OPEN_INVENTORIES.clear();
        OPEN_PLAYERS.clear();
    }
    private static final Set<UUID> OPEN_PLAYERS = ConcurrentHashMap.newKeySet();
    private final double bet;

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

    private static final List<CaseReward> DEFAULT_REWARDS = List.of(
            new CaseReward("§fIron Ingot", Material.IRON_INGOT, 40.0, 30),
            new CaseReward("§6Gold Ingot", Material.GOLD_INGOT, 80.0, 22),
            new CaseReward("§aEmerald", Material.EMERALD, 150.0, 14),
            new CaseReward("§bDiamond", Material.DIAMOND, 300.0, 9),
            new CaseReward("§8Netherite Scrap", Material.NETHERITE_SCRAP, 600.0, 5),
            new CaseReward("§eTotem of Undying", Material.TOTEM_OF_UNDYING, 900.0, 3),
            new CaseReward("§dEnchanted Apple", Material.ENCHANTED_GOLDEN_APPLE, 1750.0, 1)
    );

    private static final class CaseReward {
        final String name;
        final Material material;
        final double amount;
        final int weight;

        CaseReward(String name, Material material, double amount, int weight) {
            this.name = name;
            this.material = material;
            this.amount = amount;
            this.weight = weight;
        }

        ItemStack icon() {
            return CasinoItems.item(material, name, "§7Pays §a" + CasinoItems.money(amount));
        }
    }

    private final Player player;
    private final Inventory inv;
    private final List<CaseReward> rewards;
    private final Random random = new Random();

    private final int[] slots = {9, 10, 11, 12, 13, 14, 15, 16, 17};
    private final int centerOffset = 4;
    private final List<ItemStack> currentScroll = new ArrayList<>();
    private CaseReward winner;
    private boolean rewardGiven = false;

    public CaseAnimation(Player player, double bet) {
        this.player = player;
        this.bet = bet;
        this.inv = Bukkit.createInventory(null, 3 * 9, "§bOpening Case...");
        this.rewards = loadRewards();
        onCaseOpened(player, inv);
    }

    public void start() {
        player.openInventory(inv);

        inv.setItem(4, CasinoItems.pane(Material.GRAY_STAINED_GLASS_PANE, "§a▼"));
        inv.setItem(22, CasinoItems.pane(Material.GRAY_STAINED_GLASS_PANE, "§a▲"));

        for (int i = 0; i < slots.length + 10; i++) {
            currentScroll.add(pickReward().icon());
        }
        winner = pickReward();
        int landingIndex = currentScroll.size() - slots.length - 1 + centerOffset;
        currentScroll.set(landingIndex, winner.icon());

        runAnimation(0);
    }

    private void runAnimation(int shift) {
        if (shift >= currentScroll.size() - slots.length) {
            if (!rewardGiven) {
                payout();
                for (int slot : slots) {
                    inv.clear(slot);
                }
                inv.clear(4);
                inv.clear(22);
                rewardGiven = true;
                Scheduler.runForPlayer(GamblingPlugin.getInstance(), player, 30L, () -> {
                    if (player.getOpenInventory().getTopInventory() == inv) {
                        player.closeInventory();
                    }
                });
            }
            return;
        }

        updateScroll(shift);

        long delay = Math.min(10 + shift * 2, 60);

        Scheduler.runForPlayer(GamblingPlugin.getInstance(), player, delay / 2, () -> runAnimation(shift + 1));
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

    private void payout() {
        OfflinePlayer target = player.isOnline() ? player : Bukkit.getOfflinePlayer(player.getUniqueId());
        double payout = winner.amount * bet
                / GamblingPlugin.getInstance().getConfig().getDouble("cases.cost", 50.0);
        GamblingPlugin.getInstance().getEconomy().depositPlayer(target, payout);
        GamblingPlugin.getInstance().getPlayerData().recordRound(player.getUniqueId(), player.getName(), bet, payout);
        GamblingPlugin.getInstance().getPlayerData().addWin(player.getUniqueId(), player.getName(), payout);
        GamblingPlugin.getInstance().getPlayerData().save();

        player.sendMessage("§aYou won: §e" + winner.name + " §7(§a+" + CasinoItems.money(payout) + "§7)");
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.2f, 1.0f);
    }

    private CaseReward pickReward() {
        int total = 0;
        for (CaseReward reward : rewards) {
            total += reward.weight;
        }
        if (total <= 0) {
            return rewards.get(0);
        }
        int roll = random.nextInt(total);
        for (CaseReward reward : rewards) {
            roll -= reward.weight;
            if (roll < 0) {
                return reward;
            }
        }
        return rewards.get(rewards.size() - 1);
    }

    private static List<CaseReward> loadRewards() {
        List<CaseReward> list = new ArrayList<>();
        ConfigurationSection section = GamblingPlugin.getInstance().getConfig().getConfigurationSection("cases.rewards");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection reward = section.getConfigurationSection(key);
                if (reward == null) continue;
                double amount = reward.getDouble("amount", -1.0);
                int weight = reward.getInt("weight", 0);
                Material material = Material.matchMaterial(reward.getString("material", "PAPER"));
                String name = ChatColor.translateAlternateColorCodes('&', reward.getString("name", key));
                if (amount < 0 || weight <= 0) continue;
                list.add(new CaseReward(name, material == null ? Material.PAPER : material, amount, weight));
            }
        }
        if (list.isEmpty()) {
            return new ArrayList<>(DEFAULT_REWARDS);
        }
        return list;
    }
}
