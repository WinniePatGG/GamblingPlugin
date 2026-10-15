package de.winniepat.gamblingPlugin.commands;

import de.winniepat.gamblingPlugin.GamblingPlugin;
import de.winniepat.gamblingPlugin.animations.CaseAnimation;
import de.winniepat.gamblingPlugin.gui.DoubleGUI;
import de.winniepat.gamblingPlugin.gui.RouletteGUI;
import de.winniepat.gamblingPlugin.gui.SlotGUI;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
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
            case "leaderboard":
            case "top":
            case "lb": {
                sender.sendMessage("§6=== Casino Leaderboard ===");
                java.util.List<java.util.Map.Entry<java.util.UUID, Double>> entries = new java.util.ArrayList<>(GamblingPlugin.getInstance().getPlayerData().getWins().entrySet());
                entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
                int rank = 1;
                for (java.util.Map.Entry<java.util.UUID, Double> e : entries) {
                    if (rank > 10) break;
                    sender.sendMessage("§e" + rank + ". §f" + GamblingPlugin.getInstance().getPlayerData().getName(e.getKey()) + " §7- §a$" + (int) e.getValue().doubleValue());
                    rank++;
                }
                if (entries.isEmpty()) sender.sendMessage("§7No wins recorded yet.");
                return true;
            }
            case "hologram": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Only players can use this command.");
                    return true;
                }
                Player p = (Player) sender;
                Location base = p.getLocation();
                java.util.List<java.util.Map.Entry<java.util.UUID, Double>> topEntries = new java.util.ArrayList<>(GamblingPlugin.getInstance().getPlayerData().getWins().entrySet());
                topEntries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

                double y = 0;
                spawnHolo(p, base.clone().add(0, y, 0), "§6§lCasino Top Winners");
                int r = 1;
                for (java.util.Map.Entry<java.util.UUID, Double> e : topEntries) {
                    if (r > 5) break;
                    spawnHolo(p, base.clone().add(0, y - (r * 0.3), 0),
                            "§e" + r + ". §f" + GamblingPlugin.getInstance().getPlayerData().getName(e.getKey()) + " §a$" + (int) e.getValue().doubleValue());
                    r++;
                }
                p.sendMessage("§aHologram spawned.");
                return true;
            }
            default:
                sender.sendMessage("§eUsage: /casino <slots|double|roulette|case|daily|update|leaderboard|hologram>");
                return true;
        }
    }

    private static void spawnHolo(Player player, Location location, String text) {
        player.getWorld().spawn(location, ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setMarker(true);
            stand.setCustomNameVisible(true);
            stand.setCustomName(text);
        });
    }
}