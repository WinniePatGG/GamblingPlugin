package de.winniepat.gamblingPlugin.gui;

import de.winniepat.gamblingPlugin.GamblingPlugin;
import de.winniepat.gamblingPlugin.commands.CasinoCommand;
import de.winniepat.gamblingPlugin.util.CasinoItems;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/**
 * The casino lobby: /casino (or /casino menu) opens this clickable hub with
 * every game, plus daily reward, leaderboard, balance and a close button.
 */
public class MenuGUI implements Listener {

    public static final String TITLE = "§6§lCasino Lobby";

    public static final int SLOT_SLOTS = 10;
    public static final int SLOT_ROULETTE = 11;
    public static final int SLOT_BLACKJACK = 12;
    public static final int SLOT_MINES = 13;
    public static final int SLOT_CRASH = 14;
    public static final int SLOT_WHEEL = 15;
    public static final int SLOT_COINFLIP = 16;
    public static final int SLOT_DOUBLE = 20;
    public static final int SLOT_CASE = 22;
    public static final int SLOT_DAILY = 24;
    public static final int SLOT_LEADERBOARD = 38;
    public static final int SLOT_CLOSE = 40;
    public static final int SLOT_BALANCE = 42;

    public MenuGUI() {
    }

    public static void open(Player player) {
        Inventory inv = org.bukkit.Bukkit.createInventory(null, 45, TITLE);
        CasinoItems.fillAll(inv, Material.GRAY_STAINED_GLASS_PANE);

        double slotsCost = GamblingPlugin.getInstance().getConfig().getDouble("slots.bet-cost", 100.0);
        double rouletteCost = GamblingPlugin.getInstance().getConfig().getDouble("roulette.bet-cost", 50.0);
        double blackjackCost = GamblingPlugin.getInstance().getConfig().getDouble("blackjack.bet-cost", 100.0);
        double minesCost = GamblingPlugin.getInstance().getConfig().getDouble("mines.bet-cost", 100.0);
        double crashCost = GamblingPlugin.getInstance().getConfig().getDouble("crash.bet-cost", 100.0);
        double wheelCost = GamblingPlugin.getInstance().getConfig().getDouble("wheel.bet-cost", 100.0);
        double coinflipCost = GamblingPlugin.getInstance().getConfig().getDouble("coinflip.bet-cost", 100.0);
        double doubleCost = GamblingPlugin.getInstance().getConfig().getDouble("double.entry-cost", 20.0);
        double caseCost = GamblingPlugin.getInstance().getConfig().getDouble("cases.cost", 50.0);

        inv.setItem(SLOT_SLOTS, CasinoItems.item(Material.DISPENSER, "§6§lSlots",
                "§7Three reels, five symbols.", "§7Bet: §e" + CasinoItems.money(slotsCost)));
        inv.setItem(SLOT_ROULETTE, CasinoItems.item(Material.COMPASS, "§c§lRoulette",
                "§7Bet on red, green or black.", "§7Bet: §e" + CasinoItems.money(rouletteCost)));
        inv.setItem(SLOT_BLACKJACK, CasinoItems.item(Material.WRITABLE_BOOK, "§2§lBlackjack",
                "§7Beat the dealer without busting.", "§7Bet: §e" + CasinoItems.money(blackjackCost)));
        inv.setItem(SLOT_MINES, CasinoItems.item(Material.TNT, "§c§lMines",
                "§7Reveal gems, dodge the mines.", "§7Bet: §e" + CasinoItems.money(minesCost)));
        inv.setItem(SLOT_CRASH, CasinoItems.item(Material.FIREWORK_ROCKET, "§6§lCrash",
                "§7Ride the multiplier...", "§7Bet: §e" + CasinoItems.money(crashCost)));
        inv.setItem(SLOT_WHEEL, CasinoItems.item(Material.MUSIC_DISC_CAT, "§5§lWheel of Fortune",
                "§7Land on the big multipliers.", "§7Bet: §e" + CasinoItems.money(wheelCost)));
        inv.setItem(SLOT_COINFLIP, CasinoItems.item(Material.GOLD_NUGGET, "§e§lCoinflip",
                "§7Heads or tails?", "§7Bet: §e" + CasinoItems.money(coinflipCost)));

        inv.setItem(SLOT_DOUBLE, CasinoItems.item(Material.REDSTONE_TORCH, "§a§lDouble or Nothing",
                "§7Keep doubling or walk away.", "§7Entry: §e" + CasinoItems.money(doubleCost)));
        inv.setItem(SLOT_CASE, CasinoItems.item(Material.CHEST, "§b§lCase Opening",
                "§7CS:GO-style item cases.", "§7Case: §e" + CasinoItems.money(caseCost)));
        inv.setItem(SLOT_DAILY, CasinoItems.item(Material.CLOCK, "§e§lDaily Reward",
                "§7Claim your free coins once a day."));

        inv.setItem(SLOT_LEADERBOARD, CasinoItems.item(Material.EMERALD, "§a§lTop Winners",
                "§7View the all-time leaderboard."));
        inv.setItem(SLOT_BALANCE, CasinoItems.item(Material.PAPER, "§fBalance: §a"
                + CasinoItems.money(GamblingPlugin.getInstance().getEconomy().getBalance(player)),
                "§7Click to refresh."));
        inv.setItem(SLOT_CLOSE, CasinoItems.item(Material.BARRIER, "§c§lClose"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!de.winniepat.gamblingPlugin.util.ViewTitles.title(event.getView()).equals(TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        switch (slot) {
            case SLOT_SLOTS:
                de.winniepat.gamblingPlugin.util.GameOpener.openGame(player, "slots");
                break;
            case SLOT_ROULETTE:
                de.winniepat.gamblingPlugin.util.GameOpener.openGame(player, "roulette");
                break;
            case SLOT_BLACKJACK:
                de.winniepat.gamblingPlugin.util.GameOpener.openGame(player, "blackjack");
                break;
            case SLOT_MINES:
                de.winniepat.gamblingPlugin.util.GameOpener.openGame(player, "mines");
                break;
            case SLOT_CRASH:
                de.winniepat.gamblingPlugin.util.GameOpener.openGame(player, "crash");
                break;
            case SLOT_WHEEL:
                de.winniepat.gamblingPlugin.util.GameOpener.openGame(player, "wheel");
                break;
            case SLOT_COINFLIP:
                de.winniepat.gamblingPlugin.util.GameOpener.openGame(player, "coinflip");
                break;
            case SLOT_DOUBLE:
                de.winniepat.gamblingPlugin.util.GameOpener.openGame(player, "double");
                break;
            case SLOT_CASE:
                de.winniepat.gamblingPlugin.util.GameOpener.openGame(player, "case");
                break;
            case SLOT_DAILY:
                org.bukkit.Bukkit.dispatchCommand(player, "casino daily");
                break;
            case SLOT_LEADERBOARD:
                CasinoCommand.sendLeaderboard(player);
                break;
            case SLOT_BALANCE:
                open(player);
                break;
            case SLOT_CLOSE:
                player.closeInventory();
                break;
            default:
                break;
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (de.winniepat.gamblingPlugin.util.ViewTitles.title(event.getView()).equals(TITLE)) {
            event.setCancelled(true);
        }
    }
}
