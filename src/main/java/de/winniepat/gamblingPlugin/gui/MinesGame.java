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

import java.util.Random;
import java.util.UUID;

/**
 * Stake-style Mines: a 5x5 grid hides a configurable number of mines. Each safe
 * tile raises the payout multiplier; hitting a mine loses everything. The
 * player can cash out any time after revealing at least one tile.
 */
final class MinesGame {

    static final String TITLE = "§cMines";

    static final int[] GRID = new int[25];
    static final int SLOT_CASHOUT = 4;
    static final int SLOT_MINES_INFO = 1;
    static final int SLOT_NEXT_INFO = 7;
    static final int SLOT_CLOSE = 45;
    static final int SLOT_PLAY_AGAIN = 53;

    static {
        int i = 0;
        for (int row = 1; row <= 5; row++) {
            for (int col = 2; col <= 6; col++) {
                GRID[i++] = row * 9 + col;
            }
        }
    }

    private enum Phase {
        PLAY, ENDED
    }

    private final Player player;
    private final UUID uuid;
    private final Inventory inv;
    private final Random random = new Random();

    private final boolean[] minesAt = new boolean[25];
    private final boolean[] revealedAt = new boolean[25];

    private double bet;
    private int minesCount;
    private int reveals;
    private Phase phase = Phase.PLAY;

    MinesGame(Player player, double bet) {
        this.player = player;
        this.uuid = player.getUniqueId();
        this.bet = bet;
        this.inv = Bukkit.createInventory(null, 54, TITLE);
    }

    static double betCost() {
        return GamblingPlugin.getInstance().getConfig().getDouble("mines.bet-cost", 100.0);
    }

    void start() {
        ActiveGames.join(uuid, "mines");
        newRound();
    }

    Inventory inventory() {
        return inv;
    }

    boolean isPlaying() {
        return phase == Phase.PLAY;
    }

    /** Starts a fresh round, withdrawing a new bet. */
    private void newRound() {
        phase = Phase.PLAY;
        reveals = 0;
        minesCount = clampMines(GamblingPlugin.getInstance().getConfig().getInt("mines.mines", 3));
        for (int i = 0; i < 25; i++) {
            minesAt[i] = false;
            revealedAt[i] = false;
        }
        int placed = 0;
        while (placed < minesCount) {
            int spot = random.nextInt(25);
            if (!minesAt[spot]) {
                minesAt[spot] = true;
                placed++;
            }
        }

        bet = betCost();
        GamblingPlugin.getInstance().getEconomy().withdrawPlayer(player, bet);
        player.sendMessage("§7Mines bet: §c-" + CasinoItems.money(bet)
                + " §8| §7" + minesCount + " mines hidden. §7Reveal gems and §ecash out §7in time!");
        render();
    }

    private static int clampMines(int config) {
        return Math.max(1, Math.min(24, config));
    }

    /** Total money returned per $1 bet after k revealed tiles (house-edge adjusted). */
    private double payoutMultiplier(int revealedTiles) {
        if (revealedTiles <= 0) return 0.0;
        double edge = GamblingPlugin.getInstance().getConfig().getDouble("mines.house-edge", 0.03);
        double probability = 1.0;
        for (int j = 0; j < revealedTiles; j++) {
            probability *= (25.0 - minesCount - j) / (25.0 - j);
        }
        return (1.0 - edge) / probability;
    }

    void revealTile(int gridIndex) {
        if (phase != Phase.PLAY) return;
        if (revealedAt[gridIndex]) return;

        if (minesAt[gridIndex]) {
            phase = Phase.ENDED;
            ActiveGames.leave(uuid);
            render();
            player.sendMessage("§c§lBOOM! §7You hit a mine and lost §c" + CasinoItems.money(bet));
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
            closeSoon(50L);
            return;
        }

        revealedAt[gridIndex] = true;
        reveals++;
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.4f + reveals * 0.05f);
        render();

        if (reveals >= 25 - minesCount) {
            endRound(true);
        }
    }

    void cashOut() {
        endRound(false);
    }

    private void endRound(boolean fullClear) {
        if (phase != Phase.PLAY || reveals <= 0) return;
        phase = Phase.ENDED;

        double payout = bet * payoutMultiplier(reveals);
        deposit(payout);
        double profit = payout - bet;
        if (profit > 0) {
            GamblingPlugin.getInstance().getPlayerData().addWin(uuid, player.getName(), profit);
            GamblingPlugin.getInstance().getPlayerData().save();
        }

        if (fullClear) {
            player.sendMessage("§6§lFULL CLEAR! §aEvery safe tile revealed — you won §e" + CasinoItems.money(profit));
        } else {
            player.sendMessage("§aCashed out after " + reveals + " tiles! §7You won §e" + CasinoItems.money(profit)
                    + " §7(§f" + CasinoItems.multiplier(payoutMultiplier(reveals)) + "§7)");
        }
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        ActiveGames.leave(uuid);
        render();
        closeSoon(40L);
    }

    void playAgain() {
        if (phase == Phase.PLAY) return;
        double cost = betCost();
        if (GamblingPlugin.getInstance().getEconomy().getBalance(player) < cost) {
            player.sendMessage("§cYou don't have enough money for another round §7(§f" + CasinoItems.money(cost) + "§7)");
            return;
        }
        newRound();
    }

    private void deposit(double amount) {
        OfflinePlayer target = player.isOnline() ? player : Bukkit.getOfflinePlayer(uuid);
        GamblingPlugin.getInstance().getEconomy().depositPlayer(target, amount);
    }

    private void closeSoon(long ticks) {
        final Inventory snapshot = inv;
        final Player closer = player;
        Bukkit.getScheduler().runTaskLater(GamblingPlugin.getInstance(), () -> {
            if (phase == Phase.PLAY) {
                return; // a new round already started before the scheduled close
            }
            if (closer.isOnline() && closer.getOpenInventory().getTopInventory() == snapshot) {
                closer.closeInventory();
            }
        }, ticks);
    }

    /** Auto-resolves when the GUI is closed mid-round. */
    void forceResolveOnClose() {
        MinesGUI.forget(uuid);
        if (phase != Phase.PLAY) {
            ActiveGames.leave(uuid);
            return;
        }
        ActiveGames.leave(uuid);
        if (reveals > 0) {
            double payout = bet * payoutMultiplier(reveals);
            deposit(payout);
            double profit = payout - bet;
            if (profit > 0) {
                GamblingPlugin.getInstance().getPlayerData().addWin(uuid, player.getName(), profit);
                GamblingPlugin.getInstance().getPlayerData().save();
            }
            if (player.isOnline()) {
                player.sendMessage("§eYou left the board — auto cash out: §a+" + CasinoItems.money(profit));
            }
        } else if (player.isOnline()) {
            player.sendMessage("§cYou left the board before revealing a tile. §7Your bet was lost.");
        }
    }

    private void render() {
        CasinoItems.fillAll(inv, Material.BLACK_STAINED_GLASS_PANE);

        for (int i = 0; i < 25; i++) {
            int slot = GRID[i];
            if (revealedAt[i]) {
                inv.setItem(slot, CasinoItems.item(Material.EMERALD, "§a§lGEM!"));
            } else if (minesAt[i]) {
                inv.setItem(slot, phase == Phase.PLAY
                        ?CasinoItems.item(Material.SMOOTH_STONE, "§f?", "§7Click to reveal.")
                        : CasinoItems.item(Material.TNT, "§cBOOM!"));
            } else {
                inv.setItem(slot, phase == Phase.PLAY
                        ? CasinoItems.item(Material.SMOOTH_STONE, "§f?", "§7Click to reveal.")
                        : CasinoItems.pane(Material.GRAY_STAINED_GLASS_PANE, " "));
            }
        }

        inv.setItem(SLOT_MINES_INFO, CasinoItems.item(Material.TNT,
                "§c§lMines: §f" + minesCount, "§7Tiles revealed: §f" + reveals));

        if (phase == Phase.PLAY) {
            double current = bet * payoutMultiplier(reveals);
            double nextMultiplier = payoutMultiplier(reveals + 1);
            inv.setItem(SLOT_NEXT_INFO, CasinoItems.item(Material.PAPER,
                    "§eNext gem: §l" + CasinoItems.multiplier(nextMultiplier),
                    "§7Cash out after it: §a" + CasinoItems.money(bet * nextMultiplier - bet)));

            inv.setItem(SLOT_CASHOUT, reveals > 0
                    ? CasinoItems.item(Material.GOLD_BLOCK, "§6§lCASH OUT",
                            "§7Current win: §a" + CasinoItems.money(bet * payoutMultiplier(reveals) - bet))
                    : CasinoItems.item(Material.GOLD_NUGGET, "§7Cash out", "§7Reveal a tile first."));
        } else {
            inv.setItem(SLOT_CASHOUT, CasinoItems.pane(Material.RED_STAINED_GLASS_PANE, " "));
        }

        inv.setItem(SLOT_CLOSE, CasinoItems.item(Material.BARRIER, "§cClose"));
        inv.setItem(SLOT_PLAY_AGAIN, phase == Phase.PLAY
                ? CasinoItems.pane(Material.BLACK_STAINED_GLASS_PANE, " ")
                : CasinoItems.item(Material.LIME_WOOL, "§a§lPLAY AGAIN §e(" + CasinoItems.money(betCost()) + ")"));
    }
}
