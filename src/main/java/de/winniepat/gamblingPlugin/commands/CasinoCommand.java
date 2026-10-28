package de.winniepat.gamblingPlugin.commands;

import de.winniepat.gamblingPlugin.GamblingPlugin;
import de.winniepat.gamblingPlugin.util.GameOpener;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CasinoCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Collections.unmodifiableList(Arrays.asList(
            "menu", "slots", "roulette", "blackjack", "mines", "crash", "wheel", "coinflip",
            "double", "case", "daily", "leaderboard", "hologram", "update", "reload"));

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean hasArgs = args != null && args.length > 0;
        String key = hasArgs ? args[0].toLowerCase() : "menu";

        switch (key) {
            case "menu":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Only players can use this command.");
                    return true;
                }
                GameOpener.openGame((Player) sender, "menu");
                return true;
            case "slots":
            case "roulette":
            case "blackjack":
            case "mines":
            case "crash":
            case "wheel":
            case "coinflip":
            case "double": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Only players can use this command.");
                    return true;
                }
                GameOpener.openGame((Player) sender, key);
                return true;
            }
            case "case": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Only players can use this command.");
                    return true;
                }
                GameOpener.openCase((Player) sender);
                return true;
            }
            case "daily":
                return new DailyCommand().onCommand(sender, command, label,
                        Arrays.copyOfRange(args, hasArgs ? 1 : 0, args.length));
            case "update":
            case "casinoupdate":
                return new UpdateCommand().onCommand(sender, command, label,
                        Arrays.copyOfRange(args, hasArgs ? 1 : 0, args.length));
            case "reload": {
                if (sender instanceof Player && !sender.isOp()) {
                    sender.sendMessage("§cYou need operator permission to use this command.");
                    return true;
                }
                GamblingPlugin.getInstance().reloadPluginConfig();
                sender.sendMessage("§aGamblingPlugin configuration reloaded.");
                return true;
            }
            case "leaderboard":
            case "top":
            case "lb":
                sendLeaderboard(sender);
                return true;
            case "hologram": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Only players can use this command.");
                    return true;
                }
                Player p = (Player) sender;
                Location base = p.getLocation();
                List<Map.Entry<UUID, Double>> topEntries = new ArrayList<>(
                        GamblingPlugin.getInstance().getPlayerData().getWins().entrySet());
                topEntries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

                spawnHolo(p, base.clone(), "§6§lCasino Top Winners");
                int r = 1;
                for (Map.Entry<UUID, Double> entry : topEntries) {
                    if (r > 5) break;
                    spawnHolo(p, base.clone().add(0, -(r * 0.3), 0),
                            "§e" + r + ". §f" + GamblingPlugin.getInstance().getPlayerData().getName(entry.getKey())
                                    + " §a$" + (int) entry.getValue().doubleValue());
                    r++;
                }
                p.sendMessage("§aHologram spawned.");
                return true;
            }
            default:
                sender.sendMessage("§eUsage: /casino <menu|slots|blackjack|mines|crash|wheel|coinflip|roulette|double|case|daily|leaderboard|hologram|update|reload>");
                return true;
        }
    }

    /** Shared leaderboard output, also used by the lobby menu button. */
    public static void sendLeaderboard(CommandSender sender) {
        sender.sendMessage("§6=== Casino Leaderboard ===");
        List<Map.Entry<UUID, Double>> entries = new ArrayList<>(
                GamblingPlugin.getInstance().getPlayerData().getWins().entrySet());
        entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        int rank = 1;
        for (Map.Entry<UUID, Double> entry : entries) {
            if (rank > 10) break;
            sender.sendMessage("§e" + rank + ". §f" + GamblingPlugin.getInstance().getPlayerData().getName(entry.getKey())
                    + " §7- §a$" + (int) entry.getValue().doubleValue());
            rank++;
        }
        if (entries.isEmpty()) sender.sendMessage("§7No wins recorded yet.");
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args == null || args.length != 1) {
            return Collections.emptyList();
        }
        String partial = args[0].toLowerCase();
        List<String> result = new ArrayList<>();
        for (String sub : SUBCOMMANDS) {
            if (sub.startsWith(partial)) {
                result.add(sub);
            }
        }
        return result;
    }
}
