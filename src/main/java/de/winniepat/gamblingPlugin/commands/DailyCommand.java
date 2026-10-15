package de.winniepat.gamblingPlugin.commands;

import de.winniepat.gamblingPlugin.GamblingPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.UUID;

public class DailyCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        String today = LocalDate.now().toString();

        if (today.equals(GamblingPlugin.getInstance().getPlayerData().getLastDailyClaim(uuid))) {
            player.sendMessage("§cYou already claimed your daily reward today.");
            return true;
        }

        double amount = GamblingPlugin.getInstance().getConfig().getDouble("daily-reward.amount", 200.0);
        GamblingPlugin.getInstance().getEconomy().depositPlayer(player, amount);
        GamblingPlugin.getInstance().getPlayerData().setLastDailyClaim(uuid, today);
        GamblingPlugin.getInstance().getPlayerData().save();

        player.sendMessage("§aYou claimed your daily reward: §6$" + (int) amount);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        return true;
    }
}