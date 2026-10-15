package de.winniepat.gamblingPlugin.commands;

import de.winniepat.gamblingPlugin.GamblingPlugin;
import de.winniepat.gamblingPlugin.animations.CaseAnimation;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CaseCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
    }
}
