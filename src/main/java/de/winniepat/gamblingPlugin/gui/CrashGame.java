package de.winniepat.gamblingPlugin.gui;

import de.winniepat.gamblingPlugin.GamblingPlugin;
import de.winniepat.gamblingPlugin.util.ActiveGames;
import de.winniepat.gamblingPlugin.util.CasinoItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Locale;
import java.util.Random;
import java.util.UUID;

/**
 * Crash: pay a stake, watch the multiplier climb, cash out before it crashes.
 * The crash point is sampled so that P(crash >= x) = (1 - house-edge) / x.
 */
final class CrashGame {

    static final String TITLE = "§6Crash";

    static final int SLOT_MULTIPLIER = 13;
    static final int SLOT_CASHOUT = 22;
    static final int SLOT_BET = 11;
    static final int SLOT_POTENTIAL = 15;
    static final int[] BAR = {4, 5, 3, 6, 2, 7, 1, 8, 0};

    private final Player player;
    private final UUID uuid;
    private final Inventory inv;
    private final Random random = new Random();

    private double bet;
    private double multiplier = 1.0;
    private double crashPoint;
    private int steps;
    private boolean running;
    private boolean settled;
    private BukkitTask task;

    CrashGame(Player player, double bet) {
        this.player = player;
        this.uuid = player.getUniqueId();
        this.bet = bet;
        this.inv = Bukkit.createInventory(null, 27, TITLE);
    }

    static double betCost() {
        return GamblingPlugin.getInstance().getConfig().getDouble("crash.bet-cost", 100.0);
    }

    Inventory inventory() {
        return inv;
    }

    void start() {
        ActiveGames.join(uuid, "crash");

        double edge = GamblingPlugin.getInstance().getConfig().getDouble("crash.house-edge", 0.01);
        double cap = GamblingPlugin.getInstance().getConfig().getDouble("crash.max-multiplier", 250.0);
        double roll = random.nextDouble();
        crashPoint = (1.0 - edge) / (1.0 - roll);
        if (crashPoint < 1.0) {
            crashPoint = 1.0;
        }
        if (crashPoint > cap) {
            crashPoint = cap;
        }

        renderWaiting("§eStarting in a moment...");
        player.openInventory(inv);
        player.sendMessage("§7Crash bet: §c-" + CasinoItems.money(bet) + " §8| §7Cash out before it §c§lCRASHES§7!");

        Bukkit.getScheduler().runTaskLater(GamblingPlugin.getInstance(), () -> {
            if (settled) return;
            running = true;
            ItemStack star = CasinoItems.item(Material.NETHER_STAR, "§e§l1.00x");
            inv.setItem(SLOT_MULTIPLIER, star);
            task = new BukkitRunnable() {
                @Override
                public void run() {
                    step();
                }
            }.runTaskTimer(GamblingPlugin.getInstance(), 2L, 2L);
        }, 20L);
    }

    private void step() {
        if (settled) {
            cancelTask();
            return;
        }
        double growth = GamblingPlugin.getInstance().getConfig().getDouble("crash.growth", 0.0075);
        double factor = 1.0 + growth * (1.0 + multiplier * 0.02);
        multiplier *= factor;

        if (multiplier >= crashPoint) {
            crash();
            return;
        }

        renderRunning();
        steps++;
        float pitch = Math.min(2.0f, 1.0f + steps * 0.01f);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, pitch);
    }

    void cashOut() {
        if (!running || settled) return;
        settle(multiplier);
    }

    private void crash() {
        if (settled) return;
        settled = true;
        running = false;
        cancelTask();
        ActiveGames.leave(uuid);
        CrashGUI.forget(uuid);
        renderCrashed();
        if (player.isOnline()) {
            player.sendMessage("§c§lCRASHED §7at §f" + fmt(crashPoint) + "§7! §cYou lost " + CasinoItems.money(bet));
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.8f);
        }
        closeSoon(40L);
    }

    private void settle(double at) {
        if (settled) return;
        settled = true;
        running = false;
        cancelTask();
        ActiveGames.leave(uuid);
        CrashGUI.forget(uuid);

        double payout = bet * at;
        double profit = payout - bet;
        deposit(payout);
        if (profit > 0) {
            GamblingPlugin.getInstance().getPlayerData().addWin(uuid, player.getName(), profit);
            GamblingPlugin.getInstance().getPlayerData().save();
        }

        renderCashed(at);
        if (player.isOnline()) {
            player.sendMessage("§aCashed out at §f" + fmt(at) + "§7! §aYou won §e" + CasinoItems.money(profit));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
        closeSoon(40L);
    }

    /** Called when the GUI is closed before the round finished: auto cash out. */
    void forceResolveOnClose() {
        CrashGUI.forget(uuid);
        if (settled) return;
        if (running) {
            player.sendMessage("§7You closed the graph — auto cash out at §f" + fmt(multiplier));
            settle(multiplier);
        } else {
            settled = true;
            ActiveGames.leave(uuid);
            deposit(bet);
            player.sendMessage("§eCrash cancelled — your bet was refunded.");
        }
    }

    private void cancelTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void deposit(double amount) {
        OfflinePlayer target = player.isOnline() ? player : Bukkit.getOfflinePlayer(uuid);
        GamblingPlugin.getInstance().getEconomy().depositPlayer(target, amount);
    }

    private void closeSoon(long ticks) {
        final Inventory snapshot = inv;
        final Player closer = player;
        Bukkit.getScheduler().runTaskLater(GamblingPlugin.getInstance(), () -> {
            if (closer.isOnline() && closer.getOpenInventory().getTopInventory() == snapshot) {
                closer.closeInventory();
            }
        }, ticks);
    }

    private static String fmt(double value) {
        return String.format(Locale.US, "%.2f", value) + "x";
    }

    private void renderWaiting(String label) {
        CasinoItems.fillAll(inv, Material.GRAY_STAINED_GLASS_PANE);
        inv.setItem(SLOT_MULTIPLIER, CasinoItems.item(Material.NETHER_STAR, "§7" + label));
        inv.setItem(SLOT_BET, CasinoItems.item(Material.GOLD_INGOT, "§eBet: " + CasinoItems.money(bet)));
        renderCashoutButton();
    }

    private void renderRunning() {
        inv.setItem(SLOT_MULTIPLIER, CasinoItems.item(Material.NETHER_STAR, "§e§l" + fmt(multiplier)));

        int filled = (int) Math.min(BAR.length, multiplier);
        for (int i = 0; i < BAR.length; i++) {
            inv.setItem(BAR[i], i < filled
                    ? CasinoItems.pane(Material.LIME_STAINED_GLASS_PANE, " ")
                    : CasinoItems.pane(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        inv.setItem(SLOT_POTENTIAL, CasinoItems.item(Material.PAPER,
                "§aCash out now for §l" + CasinoItems.money(bet * multiplier - bet)));
        renderCashoutButton();
    }

    private void renderCashoutButton() {
        inv.setItem(SLOT_CASHOUT, CasinoItems.item(Material.GOLD_BLOCK,
                "§6§lCASH OUT", "§7Click to take your money."));
    }

    private void renderCrashed() {
        CasinoItems.fillAll(inv, Material.RED_STAINED_GLASS_PANE);
        inv.setItem(SLOT_MULTIPLIER, CasinoItems.item(Material.BARRIER, "§c§lCRASHED @ " + fmt(crashPoint)));
        inv.setItem(SLOT_CASHOUT, CasinoItems.item(Material.GRAY_DYE, "§7Too late..."));
    }

    private void renderCashed(double at) {
        CasinoItems.fillAll(inv, Material.LIME_STAINED_GLASS_PANE);
        inv.setItem(SLOT_MULTIPLIER, CasinoItems.item(Material.GOLD_BLOCK,
                "§6§lCASHED OUT @ " + fmt(at),
                "§7Total paid: §a" + CasinoItems.money(bet * at)));
        inv.setItem(SLOT_CASHOUT, CasinoItems.item(Material.GRAY_DYE, "§aGot it!"));
    }
}
