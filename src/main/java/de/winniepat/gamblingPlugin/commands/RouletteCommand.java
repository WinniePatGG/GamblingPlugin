package de.winniepat.gamblingPlugin.commands;

import de.winniepat.gamblingPlugin.gui.RouletteGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RouletteCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        player.openInventory(RouletteGUI.createBettingGUI());
        return true;
    }
}
