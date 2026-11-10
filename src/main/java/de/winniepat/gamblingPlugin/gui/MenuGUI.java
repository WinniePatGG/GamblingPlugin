package de.winniepat.gamblingPlugin.gui;

import de.winniepat.gamblingPlugin.GamblingPlugin;
import de.winniepat.gamblingPlugin.commands.CasinoCommand;
import de.winniepat.gamblingPlugin.gui.customui.MenuHolder;
import de.winniepat.gamblingPlugin.util.CasinoItems;
import de.winniepat.gamblingPlugin.util.GameOpener;
import de.winniepat.gamblingPlugin.util.ViewTitles;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class MenuGUI implements Listener {

    public static final String TITLE = "§6§lGambling Menu";

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

    public MenuGUI(){}

    public static void open(Player player) {
        MenuHolder holder = new MenuHolder();

        Inventory inv = Bukkit.createInventory(holder, 45, TITLE);

        holder.setInventory(inv);

        double slotsCost = GamblingPlugin.getInstance().getConfig().getDouble("slots.bet-cost", 100.0);
        double rouletteCost = GamblingPlugin.getInstance().getConfig().getDouble("roulette.bet-cost", 50.0);
        double blackjackCost = GamblingPlugin.getInstance().getConfig().getDouble("blackjack.bet-cost", 100.0);
        double minesCost = GamblingPlugin.getInstance().getConfig().getDouble("mines.bet-cost", 100.0);
        double crashCost = GamblingPlugin.getInstance().getConfig().getDouble("crash.bet-cost", 100.0);
        double wheelCost = GamblingPlugin.getInstance().getConfig().getDouble("wheel.bet-cost", 100.0);
        double coinflipCost = GamblingPlugin.getInstance().getConfig().getDouble("coinflip.bet-cost", 100.0);
        double doubleCost = GamblingPlugin.getInstance().getConfig().getDouble("double.entry-cost", 20.0);
        double caseCost = GamblingPlugin.getInstance().getConfig().getDouble("cases.cost", 50.0);

        // row 1
        inv.setItem(SLOT_SLOTS, CasinoItems.model(Material.PAPER, 1001, "§6§lSlots",
                "§7Three reels, five symbols.", "§7Bet: §e" + CasinoItems.money(slotsCost)));
        inv.setItem(SLOT_ROULETTE, CasinoItems.model(Material.PAPER, 1002, "§c§lRoulette",
                "§7Bet on red, green or black.", "§7Bet: §e" + CasinoItems.money(rouletteCost)));
        inv.setItem(SLOT_BLACKJACK, CasinoItems.model(Material.PAPER, 1003, "§2§lBlackjack",
                "§7Beat the dealer without busting.", "§7Bet: §e" + CasinoItems.money(blackjackCost)));
        inv.setItem(SLOT_MINES, CasinoItems.model(Material.PAPER, 1004, "§c§lMines",
                "§7Reveal gems, dodge the mines.", "§7Bet: §e" + CasinoItems.money(minesCost)));
        inv.setItem(SLOT_CRASH, CasinoItems.model(Material.PAPER, 1005, "§6§lCrash",
                "§7Ride the multiplier...", "§7Bet: §e" + CasinoItems.money(crashCost)));
        inv.setItem(SLOT_WHEEL, CasinoItems.model(Material.PAPER, 1006, "§5§lWheel of Fortune",
                "§7Land on the big multipliers.", "§7Bet: §e" + CasinoItems.money(wheelCost)));
        inv.setItem(SLOT_COINFLIP, CasinoItems.model(Material.PAPER, 1007, "§e§lCoinflip",
                "§7Heads or tails?", "§7Bet: §e" + CasinoItems.money(coinflipCost)));

        // row 2
        inv.setItem(SLOT_DOUBLE, CasinoItems.model(Material.PAPER, 1008, "§a§lDouble or Nothing",
                "§7Keep doubling or walk away.", "§7Entry: §e" + CasinoItems.money(doubleCost)));
        inv.setItem(SLOT_CASE, CasinoItems.model(Material.PAPER, 1009, "§b§lCase Opening",
                "§7CS:GO-style cases. Pays real money.", "§7Case: §e" + CasinoItems.money(caseCost)));
        inv.setItem(SLOT_DAILY, CasinoItems.item(Material.CLOCK, "§e§lDaily Reward",
                "§7Claim your free coins once a day."));

        inv.setItem(SLOT_LEADERBOARD, CasinoItems.item(Material.EMERALD, "§a§lTop Winners",
                "§7View the all-time leaderboard."));
        inv.setItem(SLOT_BALANCE, CasinoItems.item(Material.PAPER, "§fBalance: §a"
                + CasinoItems.money(GamblingPlugin.getInstance().getEconomy().getBalance(player)),
                "§7Click to refresh."));
        inv.setItem(SLOT_CLOSE, CasinoItems.model(Material.PAPER, 1012, "§c§lClose"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!ViewTitles.title(event.getView()).equals(TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        int slot = event.getSlot();
        switch (slot) {
            case SLOT_SLOTS:
                GameOpener.openGame(player, "slots");
                break;
            case SLOT_ROULETTE:
                GameOpener.openGame(player, "roulette");
                break;
            case SLOT_BLACKJACK:
                GameOpener.openGame(player, "blackjack");
                break;
            case SLOT_MINES:
                GameOpener.openGame(player, "mines");
                break;
            case SLOT_CRASH:
                GameOpener.openGame(player, "crash");
                break;
            case SLOT_WHEEL:
                GameOpener.openGame(player, "wheel");
                break;
            case SLOT_COINFLIP:
                GameOpener.openGame(player, "coinflip");
                break;
            case SLOT_DOUBLE:
                GameOpener.openGame(player, "double");
                break;
            case SLOT_CASE:
                GameOpener.openGame(player, "case");
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
        if (ViewTitles.title(event.getView()).equals(TITLE)) {
            event.setCancelled(true);
        }
    }
}
