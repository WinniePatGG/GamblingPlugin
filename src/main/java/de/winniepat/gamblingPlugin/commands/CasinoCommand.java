package de.winniepat.gamblingPlugin.commands;

import de.winniepat.gamblingPlugin.GamblingPlugin;
import de.winniepat.gamblingPlugin.animations.CaseAnimation;
import de.winniepat.gamblingPlugin.gui.DoubleGUI;
import de.winniepat.gamblingPlugin.gui.RouletteGUI;
import de.winniepat.gamblingPlugin.gui.SlotGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CasinoCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String key = (args != null && args.length > 0) ? args[0].toLowerCase() : command.getName().toLowerCase();

        switch (key) {
            case "slots":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Only players can use this command.");
                    return true;
                }
                SlotGUI.openSlotGUI((Player) sender);
                return true;
            case "double":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Only players can use this command.");
                    return true;
                }
                DoubleGUI.open((Player) sender);
                return true;
            case "roulette":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Only players can use this command.");
                    return true;
                }
                ((Player) sender).openInventory(RouletteGUI.createBettingGUI());
                return true;
            case "case":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Only players can use this command.");
                    return true;
                }
                Player player = (Player) sender;
                double cost = GamblingPlugin.getInstance().getConfig().getDouble("cases.cost", 50.0);
                if (GamblingPlugin.getInstance().getEconomy().getBalance(player) < cost) {
                    player.sendMessage("§cYou don't have enough money to open a case! §7(Required: $" + (int) cost + ")");
                    return true;
                }
                GamblingPlugin.getInstance().getEconomy().withdrawPlayer(player, cost);
                player.sendMessage("§7Case opened: §6-$" + (int) cost);
                new CaseAnimation(player).start();
                return true;
            case "daily":
                return new DailyCommand().onCommand(sender, command, label, java.util.Arrays.copyOfRange(args, (args.length > 0 ? 1 : 0), args.length));
            case "update":
            case "casinoupdate":
                return new UpdateCommand().onCommand(sender, command, label, java.util.Arrays.copyOfRange(args, (args.length > 0 ? 1 : 0), args.length));
            default:
                sender.sendMessage("§eUsage: /casino <slots|double|roulette|case|daily|update>");
                return true;
        }
    }
}