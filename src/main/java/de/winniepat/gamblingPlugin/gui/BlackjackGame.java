package de.winniepat.gamblingPlugin.gui;

import de.winniepat.gamblingPlugin.GamblingPlugin;
import de.winniepat.gamblingPlugin.util.ActiveGames;
import de.winniepat.gamblingPlugin.util.Cards;
import de.winniepat.gamblingPlugin.util.CasinoItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * One blackjack round against the dealer. The GUI shows the dealer's cards on
 * the top row and the player's cards in the middle, with HIT / DOUBLE / STAND
 * buttons underneath. Closing the GUI mid-hand forces the player to stand.
 */
final class BlackjackGame {

    static final String TITLE = "§2Blackjack";

    static final int SLOT_HIT = 38;
    static final int SLOT_DOUBLE = 40;
    static final int SLOT_STAND = 42;

    private final Player player;
    private final UUID uuid;
    private final Inventory inv;
    private final Random random = new Random();

    private final List<String> playerCards = new ArrayList<>();
    private final List<String> dealerCards = new ArrayList<>();

    private double bet;
    private boolean holeRevealed;
    private boolean dealerTurn;
    private boolean resolved;
    private boolean playerHasBlackjack;
    private boolean dealerTaskRunning;
    private BukkitTask dealerTask;

    BlackjackGame(Player player, double bet) {
        this.player = player;
        this.uuid = player.getUniqueId();
        this.bet = bet;
        this.inv = Bukkit.createInventory(null, 45, TITLE);
    }

    static double betCost() {
        return GamblingPlugin.getInstance().getConfig().getDouble("blackjack.bet-cost", 100.0);
    }

    /** Creates the round, withdraws nothing (caller already did) and deals. */
    void start() {
        playerCards.add(Cards.draw(random));
        dealerCards.add(Cards.draw(random));
        playerCards.add(Cards.draw(random));
        dealerCards.add(Cards.draw(random));
        playerHasBlackjack = Cards.isBlackjack(playerCards);

        ActiveGames.join(uuid, "blackjack");
        render();
        player.openInventory(inv);

        player.sendMessage("§7Your bet: §a" + CasinoItems.money(bet)
                + " §8| §7Hit (✓), Double (2x bet) or Stand against the dealer.");

        if (playerHasBlackjack) {
            player.sendMessage("§6§lBLACKJACK!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.6f);
            beginDealerTurn();
        }
    }

    Inventory inventory() {
        return inv;
    }

    boolean isResolved() {
        return resolved;
    }

    void hit() {
        if (dealerTurn || resolved) return;
        playerCards.add(Cards.draw(random));
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.6f);
        int value = Cards.value(playerCards);
        if (value > 21) {
            resolveBust();
            return;
        }
        if (value == 21) {
            stand();
            return;
        }
        render();
    }

    void doubleDown() {
        if (dealerTurn || resolved) return;
        if (playerCards.size() != 2) return;
        if (!playerHasBlackjack && GamblingPlugin.getInstance().getEconomy().getBalance(player) < bet) {
            player.sendMessage("§cYou can't afford to double down (§7needs " + CasinoItems.money(bet) + "§c).");
            return;
        }
        GamblingPlugin.getInstance().getEconomy().withdrawPlayer(player, bet);
        bet *= 2;
        player.sendMessage("§7Doubled down! Bet is now §e" + CasinoItems.money(bet));
        playerCards.add(Cards.draw(random));
        int value = Cards.value(playerCards);
        if (value > 21) {
            resolveBust();
            return;
        }
        stand();
    }

    void stand() {
        if (dealerTurn || resolved) return;
        beginDealerTurn();
    }

    private void beginDealerTurn() {
        dealerTurn = true;
        render();

        dealerTaskRunning = true;
        dealerTask = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                dealerStep();
            }
        }.runTaskTimer(GamblingPlugin.getInstance(), 10L, 10L);
    }

    private void dealerStep() {
        if (resolved) {
            cancelDealerTask();
            return;
        }
        if (!holeRevealed) {
            holeRevealed = true;
            render();
            if (player.isOnline()) {
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 0.7f);
            }
            return;
        }
        if (!playerHasBlackjack && shouldDealerDraw()) {
            dealerCards.add(Cards.draw(random));
            render();
            if (player.isOnline()) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, 1.2f);
            }
            return;
        }
        cancelDealerTask();
        resolveOutcome();
    }

    private void cancelDealerTask() {
        dealerTaskRunning = false;
        if (dealerTask != null) {
            dealerTask.cancel();
            dealerTask = null;
        }
    }

    private boolean shouldDealerDraw() {
        boolean standOnSoft17 = GamblingPlugin.getInstance().getConfig().getBoolean("blackjack.dealer-stands-on-soft-17", true);
        int value = Cards.value(dealerCards);
        if (value < 17) return true;
        return value == 17 && Cards.isSoft(dealerCards) && !standOnSoft17;
    }

    private void resolveBust() {
        if (resolved) return;
        resolved = true;
        holeRevealed = false; // dealer never plays when the player busts
        ActiveGames.leave(uuid);
        BlackjackGUI.forget(uuid);
        render();
        inv.setItem(31, CasinoItems.item(Material.BARRIER, "§c§lBUST! You lost §e" + CasinoItems.money(bet)));
        player.sendMessage("§cBust with " + Cards.value(playerCards) + "! You lost " + CasinoItems.money(bet) + ".");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);
        closeSoon(40L);
    }

    private void resolveOutcome() {
        if (resolved) return;
        resolved = true;

        int playerValue = Cards.value(playerCards);
        int dealerValue = Cards.value(dealerCards);
        boolean dealerBust = dealerValue > 21;
        boolean dealerBlackjack = dealerCards.size() == 2 && dealerValue == 21;

        double returned;
        String result;
        Sound sound;

        if (playerHasBlackjack && dealerBlackjack) {
            returned = bet * payout("push");
            result = "§ePush! Both have blackjack. §7Paid back " + CasinoItems.money(returned);
            sound = Sound.ENTITY_VILLAGER_YES;
        } else if (playerHasBlackjack) {
            returned = bet * payout("blackjack");
            double profit = returned - bet;
            result = "§6§lBLACKJACK! §aYou won " + CasinoItems.money(profit);
            sound = Sound.UI_TOAST_CHALLENGE_COMPLETE;
        } else if (dealerBust || dealerValue < playerValue) {
            returned = bet * payout("win");
            double profit = returned - bet;
            String why = dealerBust ? "Dealer busts with " + dealerValue : "You win " + playerValue + " vs " + dealerValue;
            result = "§a" + why + "! §aYou won " + CasinoItems.money(profit);
            sound = Sound.ENTITY_PLAYER_LEVELUP;
        } else if (dealerValue == playerValue) {
            returned = bet * payout("push");
            result = "§ePush at " + playerValue + ". §7Paid back " + CasinoItems.money(returned);
            sound = Sound.ENTITY_VILLAGER_YES;
        } else {
            returned = 0.0;
            result = "§cDealer wins " + dealerValue + " vs " + playerValue + ". You lost " + CasinoItems.money(bet);
            sound = Sound.ENTITY_ENDERMAN_TELEPORT;
        }

        if (returned > 0) {
            deposit(returned);
        }
        double profit = returned - bet;
        if (profit > 0) {
            GamblingPlugin.getInstance().getPlayerData().addWin(uuid, player.getName(), profit);
            GamblingPlugin.getInstance().getPlayerData().save();
        }

        ActiveGames.leave(uuid);
        BlackjackGUI.forget(uuid);
        render();
        inv.setItem(31, CasinoItems.item(Material.PAPER, result));
        if (player.isOnline()) {
            player.sendMessage(result);
            player.playSound(player.getLocation(), sound, 1f, 1f);
        }
        closeSoon(60L);
    }

    private double payout(String key) {
        return GamblingPlugin.getInstance().getConfig().getDouble("blackjack.payouts." + key, key.equals("blackjack") ? 2.5 : (key.equals("win") ? 2.0 : 1.0));
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

    /** Called when the player closes the GUI mid-hand: they are forced to stand. */
    void forceStandOnClose() {
        if (!resolved && !dealerTurn) {
            player.sendMessage("§cYou closed the table mid-hand! §7You are forced to stand.");
            beginDealerTurn();
        }
    }

    private void render() {
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, CasinoItems.pane(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        List<org.bukkit.inventory.ItemStack> dealerItems = new ArrayList<>();
        for (int i = 0; i < dealerCards.size(); i++) {
            if (i == 1 && !holeRevealed) {
                dealerItems.add(Cards.faceDown());
            } else {
                dealerItems.add(Cards.item(dealerCards.get(i)));
            }
        }
        placeCentered(inv, 0, dealerItems);

        String dealerLabel = holeRevealed ? String.valueOf(Cards.value(dealerCards)) : "?";
        inv.setItem(13, CasinoItems.item(Material.PAPER, "§7Dealer: §l" + dealerLabel));

        List<org.bukkit.inventory.ItemStack> playerItems = new ArrayList<>();
        for (String card : playerCards) {
            playerItems.add(Cards.item(card));
        }
        placeCentered(inv, 2, playerItems);

        if (!resolved) {
            inv.setItem(31, CasinoItems.item(Material.PAPER,
                    "§fYour hand: §l" + Cards.value(playerCards),
                    "§7Total closest to §l21 §7wins."));
        }

        boolean active = !dealerTurn && !resolved;
        inv.setItem(36, CasinoItems.item(Material.GOLD_INGOT, "§eBet: " + CasinoItems.money(bet)));
        inv.setItem(38, active
                ? CasinoItems.item(Material.LIME_WOOL, "§a§lHIT", "§7Take another card.")
                : CasinoItems.pane(Material.GRAY_STAINED_GLASS_PANE, " "));
        inv.setItem(40, active && playerCards.size() == 2 && !playerHasBlackjack
                ? CasinoItems.item(Material.YELLOW_WOOL, "§e§lDOUBLE", "§7Double your bet,", "§7take exactly one more card.")
                : CasinoItems.pane(Material.GRAY_STAINED_GLASS_PANE, " "));
        inv.setItem(42, active
                ? CasinoItems.item(Material.RED_WOOL, "§c§lSTAND", "§7Let the dealer finish.")
                : CasinoItems.pane(Material.GRAY_STAINED_GLASS_PANE, " "));
    }

    private static void placeCentered(Inventory inv, int row, List<org.bukkit.inventory.ItemStack> items) {
        int size = items.size();
        int start = 4 - (size - 1) / 2;
        start = Math.max(0, Math.min(start, 9 - size));
        for (int i = 0; i < size; i++) {
            inv.setItem(row * 9 + start + i, items.get(i));
        }
    }
}
