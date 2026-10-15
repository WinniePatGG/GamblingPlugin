package de.winniepat.gamblingPlugin.commands;

import de.winniepat.gamblingPlugin.GamblingPlugin;
import de.winniepat.gamblingPlugin.util.UpdateChecker;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class UpdateCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof org.bukkit.entity.Player && !sender.isOp()) {
            sender.sendMessage("§cYou need operator permission to use this command.");
            return true;
        }

        GamblingPlugin plugin = GamblingPlugin.getInstance();
        sender.sendMessage("§eChecking for updates...");

        UpdateChecker.check(plugin, true, () -> {
            if (UpdateChecker.isUpdateAvailable()) {
                sender.sendMessage("§aUpdate available: §6" + UpdateChecker.getLatestVersion()
                        + "§a. Downloading now...");
                UpdateChecker.download(plugin,
                        () -> sender.sendMessage("§aUpdate downloaded to plugins/update. Restart the server to apply it."),
                        () -> sender.sendMessage("§cUpdate download failed. Check the console for details."));
            } else {
                sender.sendMessage("§aGamblingPlugin is already up to date.");
            }
        });

        return true;
    }
}