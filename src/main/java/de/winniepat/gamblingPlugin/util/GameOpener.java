package de.winniepat.gamblingPlugin.util;

import de.winniepat.gamblingPlugin.GamblingPlugin;
import de.winniepat.gamblingPlugin.animations.CaseAnimation;
import de.winniepat.gamblingPlugin.gui.BlackjackGUI;
import de.winniepat.gamblingPlugin.gui.CoinflipGUI;
import de.winniepat.gamblingPlugin.gui.CrashGUI;
import de.winniepat.gamblingPlugin.gui.DoubleGUI;
import de.winniepat.gamblingPlugin.gui.MinesGUI;
import de.winniepat.gamblingPlugin.gui.RouletteGUI;
import de.winniepat.gamblingPlugin.gui.SlotGUI;
import de.winniepat.gamblingPlugin.gui.WheelGUI;
import de.winniepat.gamblingPlugin.gui.MenuGUI;
import org.bukkit.entity.Player;

public final class GameOpener {

    private GameOpener() {
    }

    public static void openGame(Player player, String key) {
        if (!"menu".equalsIgnoreCase(key)) {
            if (ActiveGames.busyWithOther(player.getUniqueId(), key)) {
                player.sendMessage("§cYou're already playing a game! §7Finish that one first §8(/casino menu)");
                return;
            }
            de.winniepat.gamblingPlugin.gui.BetSelectionGUI.open(player, key);
            return;
        }
        openGame(player, key, 0);
    }

    public static void openGame(Player player, String key, double bet) {
        if (key == null) {
            key = "menu";
        }
        switch (key.toLowerCase()) {
            case "menu":
                MenuGUI.open(player);
                return;
            case "slots":
                SlotGUI.openSlotGUI(player, bet);
                return;
            case "double":
                DoubleGUI.open(player, bet);
                return;
            case "roulette":
                RouletteGUI.open(player, bet);
                return;
            case "case":
                openCase(player, bet);
                return;
            case "blackjack":
                BlackjackGUI.open(player, bet);
                return;
            case "mines":
                MinesGUI.open(player, bet);
                return;
            case "crash":
                CrashGUI.open(player, bet);
                return;
            case "coinflip":
                CoinflipGUI.open(player, bet);
                return;
            case "wheel":
                WheelGUI.open(player, bet);
                return;
            default:
                MenuGUI.open(player);
                return;
        }
    }

    private static boolean guard(Player player, String key) {
        if (ActiveGames.busyWithOther(player.getUniqueId(), key)) {
            player.sendMessage("§cYou're already playing a game! §7Finish that one first §8(/casino menu)");
            return false;
        }
        return true;
    }

    public static void openCase(Player player) {
        de.winniepat.gamblingPlugin.gui.BetSelectionGUI.open(player, "case");
    }

    public static void openCase(Player player, double cost) {
        if (GamblingPlugin.getInstance().getEconomy().getBalance(player) < cost) {
            player.sendMessage("§cYou don't have enough money to open a case! §7(Required: $" + (int) cost + ")");
            return;
        }
        GamblingPlugin.getInstance().getEconomy().withdrawPlayer(player, cost);
        player.sendMessage("§7Case opened: §6-$" + (int) cost);
        new CaseAnimation(player, cost).start();
    }
}
