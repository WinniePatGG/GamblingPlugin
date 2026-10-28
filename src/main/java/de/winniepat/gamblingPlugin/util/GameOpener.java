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

/**
 * Single entry point for opening any casino game — used by the /casino command,
 * the lobby menu and the station blocks, so routing and session guards stay in
 * one place.
 */
public final class GameOpener {

    private GameOpener() {
    }

    /** Opens a game by config/command key. Unknown keys open the lobby. */
    public static void openGame(Player player, String key) {
        if (key == null) {
            key = "menu";
        }
        switch (key.toLowerCase()) {
            case "menu":
                MenuGUI.open(player);
                return;
            case "slots":
                if (!guard(player, key)) return;
                SlotGUI.openSlotGUI(player);
                return;
            case "double":
                if (!guard(player, key)) return;
                DoubleGUI.open(player);
                return;
            case "roulette":
                if (!guard(player, key)) return;
                player.openInventory(RouletteGUI.createBettingGUI());
                return;
            case "case":
                if (!guard(player, key)) return;
                openCase(player);
                return;
            case "blackjack":
                if (!guard(player, key)) return;
                BlackjackGUI.open(player);
                return;
            case "mines":
                if (!guard(player, key)) return;
                MinesGUI.open(player);
                return;
            case "crash":
                if (!guard(player, key)) return;
                CrashGUI.open(player);
                return;
            case "coinflip":
                if (!guard(player, key)) return;
                CoinflipGUI.open(player);
                return;
            case "wheel":
                if (!guard(player, key)) return;
                WheelGUI.open(player);
                return;
            default:
                MenuGUI.open(player);
                return;
        }
    }

    /** Blocks opening a new game while another interactive game is running. */
    private static boolean guard(Player player, String key) {
        if (ActiveGames.busyWithOther(player.getUniqueId(), key)) {
            player.sendMessage("§cYou're already playing a game! §7Finish that one first §8(/casino menu)");
            return false;
        }
        return true;
    }

    /** Case opening cost check + withdrawal, shared by command and menu. */
    public static void openCase(Player player) {
        double cost = GamblingPlugin.getInstance().getConfig().getDouble("cases.cost", 50.0);
        if (GamblingPlugin.getInstance().getEconomy().getBalance(player) < cost) {
            player.sendMessage("§cYou don't have enough money to open a case! §7(Required: $" + (int) cost + ")");
            return;
        }
        GamblingPlugin.getInstance().getEconomy().withdrawPlayer(player, cost);
        player.sendMessage("§7Case opened: §6-$" + (int) cost);
        new CaseAnimation(player).start();
    }
}
