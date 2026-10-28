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
import org.bukkit.scheduler.BukkitTask;

import java.util.Random;
import java.util.UUID;

/**
 * Coinflip: pick heads or tails, watch the coin flip, double~-style payout on
 * the winning side. The flip animation keeps running even if the GUI closes.
 */
final class CoinflipGame {

    static final String TITLE = "§eCoinflip";

    static final int SLOT_HEADS = 11;
    static final int SLOT_TAILS = 15;
    static final int SLOT_COIN = 13;

    private final Player player;
    private final UUID uuid;
    private final Inventory inv;
    private final Random random = new Random();

    private enum Phase {
        PICK, FLIPPING, ENDED
    }

    private Phase phase = Phase.PICK;
    private boolean pickedHeads;
    private double bet;
    private BukkitTask task;

    CoinflipGame(Player player) {
        this.player = player;
        this.uuid = player.getUniqueId();
        this.inv = Bukkit.createInventory(null, 27, TITLE);
    }

    void start() {
        ActiveGames.join(uuid, "coinflip");
        renderPick();
        player.openInventory(inv);
        player.sendMessage("§7Heads or tails? §7A win pays §e"
                + CasinoItems.multiplier(GamblingPlugin.getInstance().getConfig().getDouble("coinflip.payout", 1.95))
                + " §7of the bet.");
    }

    Inventory inventory() {
        return inv;
    }

    boolean isFlipping() {
        return phase == Phase.FLIPPING;
    }

    void pick(boolean heads) {
        if (phase != Phase.PICK) return;
        double cost = GamblingPlugin.getInstance().getConfig().getDouble("coinflip.bet-cost", 100.0);
        if (GamblingPlugin.getInstance().getEconomy().getBalance(player) < cost) {
            player.sendMessage("§cYou need at least §e" + CasinoItems.money(cost) + " §cto flip the coin.");
            return;
        }
        pickedHeads = heads;
        bet = cost;
        GamblingPlugin.getInstance().getEconomy().withdrawPlayer(player, cost);
        phase = Phase.FLIPPING;

        player.sendMessage("§7You bet " + CasinoItems.money(bet) + " on §e" + (heads ? "Heads" : "Tails") + "§7!");
        inv.setItem(SLOT_HEADS, CasinoItems.pane(Material.GRAY_STAINED_GLASS_PANE, " "));
        inv.setItem(SLOT_TAILS, CasinoItems.pane(Material.GRAY_STAINED_GLASS_PANE, " "));
        inv.setItem(SLOT_COIN, CasinoItems.item(Material.GOLD_BLOCK, "§6§lFlipping..."));

        final boolean headsChoice = heads;
        task = new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 8) {
                    cancel();
                    settle(headsChoice);
                    return;
                }
                boolean face = (ticks % 2 == 0);
                inv.setItem(SLOT_COIN, face
                        ? CasinoItems.item(Material.GOLD_BLOCK, "§6§lHeads...")
                        : CasinoItems.item(Material.IRON_BLOCK, "§f§lTails..."));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, 1.5f);
                ticks++;
            }
        }.runTaskTimer(GamblingPlugin.getInstance(), 3L, 3L);
    }

    private void settle(boolean headsChoice) {
        boolean coinHeads = random.nextBoolean();
        boolean win = coinHeads == headsChoice;
        phase = Phase.ENDED;
        ActiveGames.leave(uuid);
        CoinflipGUI.forget(uuid);

        ItemStack face = coinHeads
                ? CasinoItems.item(Material.GOLD_BLOCK, "§6§lHeads!")
                : CasinoItems.item(Material.IRON_BLOCK, "§f§lTails!");
        inv.setItem(SLOT_COIN, face);

        if (win) {
            double payout = bet * GamblingPlugin.getInstance().getConfig().getDouble("coinflip.payout", 1.95);
            double profit = payout - bet;
            deposit(payout);
            GamblingPlugin.getInstance().getPlayerData().addWin(uuid, player.getName(), profit);
            GamblingPlugin.getInstance().getPlayerData().save();
            if (player.isOnline()) {
                player.sendMessage("§aThe coin landed on §e" + (coinHeads ? "Heads" : "Tails") + "§a! You won §e" + CasinoItems.money(profit));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                inv.setItem(CoinflipGame.SLOT_COIN, CasinoItems.item(Material.GOLD_BLOCK,
                        "§6§lWIN! " + (coinHeads ? "Heads" : "Tails") + "!"));
            }
        } else {
            if (player.isOnline()) {
                player.sendMessage("§cThe coin landed on §e" + (coinHeads ? "Heads" : "Tails") + "§c. You lost " + CasinoItems.money(bet));
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);
                inv.setItem(CoinflipGame.SLOT_COIN, CasinoItems.item(Material.GRAY_DYE,
                        "§c§lLOSE! " + (coinHeads ? "Heads" : "Tails") + "."));
            }
        }

        closeSoon(40L);
    }

    /** No bet was placed yet, so closing during PICK simply leaves the lobby. */
    void forceResolveOnClose() {
        CoinflipGUI.forget(uuid);
        if (phase == Phase.FLIPPING) return; // task keeps going and pays out
        ActiveGames.leave(uuid);
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

    private void renderPick() {
        CasinoItems.fillAll(inv, Material.GRAY_STAINED_GLASS_PANE);
        inv.setItem(SLOT_COIN, CasinoItems.item(Material.GOLD_NUGGET, "§e§l?", "§7Pick a side!"));
        inv.setItem(SLOT_HEADS, CasinoItems.item(Material.GOLD_BLOCK, "§6§lHEADS",
                "§7Heads wins if the coin shows heads."));
        inv.setItem(SLOT_TAILS, CasinoItems.item(Material.IRON_BLOCK, "§f§lTAILS",
                "§7Tails wins if the coin shows tails."));
    }
}
