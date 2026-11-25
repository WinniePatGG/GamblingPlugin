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
import org.bukkit.Bukkit;
import org.bukkit.Server;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CasinoCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Collections.unmodifiableList(Arrays.asList(
            "menu", "slots", "roulette", "blackjack", "mines", "crash", "wheel", "coinflip",
            "double", "case", "daily", "stats", "leaderboard", "hologram", "update", "reload",
            "enableresourcepack", "disableresourcepack"));

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
            case "enableresourcepack": {
                if (sender instanceof Player && !sender.isOp()) {
                    sender.sendMessage("§cYou need operator permission to use this command.");
                    return true;
                }
                // Check for "auto" subcommand
                if (args.length >= 2 && "auto".equalsIgnoreCase(args[1])) {
                    return handleEnableResourcePackAuto(sender);
                }
                return handleEnableResourcePack(sender, args);
            }
            case "disableresourcepack": {
                if (sender instanceof Player && !sender.isOp()) {
                    sender.sendMessage("§cYou need operator permission to use this command.");
                    return true;
                }
                return handleDisableResourcePack(sender);
            }
            case "leaderboard":
            case "top":
            case "lb":
                sendLeaderboard(sender);
                return true;
            case "stats":
                sendStats(sender, args.length > 1 ? args[1] : null);
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
                sender.sendMessage("§eUsage: /casino <menu|stats [player]|slots|blackjack|mines|crash|wheel|coinflip|roulette|double|case|daily|leaderboard|hologram|update|reload|enableresourcepack [auto|<url> [hash]]|disableresourcepack>");
                return true;
        }
    }

    private static boolean handleEnableResourcePack(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /casino enableresourcepack <url> [sha1-hash]");
            sender.sendMessage("§7The SHA-1 hash is optional but recommended for verification.");
            return true;
        }

        String url = args[1];
        String hash = null;

        if (args.length >= 3) {
            String hashStr = args[2];
            if (hashStr.length() != 40 || !hashStr.matches("[0-9a-fA-F]+")) {
                sender.sendMessage("§cInvalid SHA-1 hash. Must be 40 hexadecimal characters.");
                return true;
            }
            hash = hashStr.toLowerCase();
        }

        GamblingPlugin plugin = GamblingPlugin.getInstance();
        boolean required = plugin.getConfig().getBoolean("resourcepack.required", false);
        String prompt = plugin.getConfig().getString("resourcepack.prompt", "This server uses a custom resource pack!");

        try {
            Server server = plugin.getServer();
            Class<?> serverClass = server.getClass();

            // Try to find setResourcePack method with various signatures
            // Newer API: setResourcePack(String url, String hash, boolean required, String prompt)
            // Older API: setResourcePack(String url, byte[] hash) or setResourcePack(String url)
            boolean success = false;

            // Try 4-arg version (String, String, boolean, String) - modern Paper
            if (!success && hash != null) {
                try {
                    Method method = serverClass.getMethod("setResourcePack", String.class, String.class, boolean.class, String.class);
                    method.invoke(server, url, hash, required, prompt);
                    success = true;
                } catch (NoSuchMethodException ignored) {}
            }
            if (!success) {
                try {
                    Method method = serverClass.getMethod("setResourcePack", String.class, String.class, boolean.class, String.class);
                    method.invoke(server, url, null, required, prompt);
                    success = true;
                } catch (NoSuchMethodException ignored) {}
            }

            // Try 4-arg version (String, byte[], boolean, String) - intermediate versions
            if (!success && hash != null) {
                try {
                    byte[] hashBytes = new byte[20];
                    for (int i = 0; i < 20; i++) {
                        hashBytes[i] = (byte) Integer.parseInt(hash.substring(i * 2, i * 2 + 2), 16);
                    }
                    Method method = serverClass.getMethod("setResourcePack", String.class, byte[].class, boolean.class, String.class);
                    method.invoke(server, url, hashBytes, required, prompt);
                    success = true;
                } catch (NoSuchMethodException ignored) {}
            }
            if (!success) {
                try {
                    Method method = serverClass.getMethod("setResourcePack", String.class, byte[].class, boolean.class, String.class);
                    method.invoke(server, url, null, required, prompt);
                    success = true;
                } catch (NoSuchMethodException ignored) {}
            }

            // Try 2-arg version (String, String) - newer but not latest
            if (!success && hash != null) {
                try {
                    Method method = serverClass.getMethod("setResourcePack", String.class, String.class);
                    method.invoke(server, url, hash);
                    success = true;
                } catch (NoSuchMethodException ignored) {}
            }
            if (!success) {
                try {
                    Method method = serverClass.getMethod("setResourcePack", String.class, String.class);
                    method.invoke(server, url, (String) null);
                    success = true;
                } catch (NoSuchMethodException ignored) {}
            }

            // Try 2-arg version (String, byte[]) - older API
            if (!success && hash != null) {
                try {
                    byte[] hashBytes = new byte[20];
                    for (int i = 0; i < 20; i++) {
                        hashBytes[i] = (byte) Integer.parseInt(hash.substring(i * 2, i * 2 + 2), 16);
                    }
                    Method method = serverClass.getMethod("setResourcePack", String.class, byte[].class);
                    method.invoke(server, url, hashBytes);
                    success = true;
                } catch (NoSuchMethodException ignored) {}
            }
            if (!success) {
                try {
                    Method method = serverClass.getMethod("setResourcePack", String.class, byte[].class);
                    method.invoke(server, url, (byte[]) null);
                    success = true;
                } catch (NoSuchMethodException ignored) {}
            }

            // Try 1-arg version (String) - oldest API
            if (!success) {
                try {
                    Method method = serverClass.getMethod("setResourcePack", String.class);
                    method.invoke(server, url);
                    success = true;
                } catch (NoSuchMethodException ignored) {}
            }

            if (!success) {
                throw new NoSuchMethodException("No compatible setResourcePack method found");
            }

            sender.sendMessage("§aResource pack enabled successfully!");
            sender.sendMessage("§7URL: §f" + url);
            if (hash != null) {
                sender.sendMessage("§7SHA-1: §f" + hash);
            }
            sender.sendMessage("§7Required: §f" + required);
            sender.sendMessage("§7Prompt: §f" + prompt);
        } catch (Exception e) {
            sender.sendMessage("§cFailed to set resource pack: §f" + e.getMessage());
            plugin.getLogger().severe("Failed to set resource pack: " + e.getMessage());
        }
        return true;
    }

    private static boolean handleEnableResourcePackAuto(CommandSender sender) {
        sender.sendMessage("§eBuilding and hosting the built-in resource pack...");
        GamblingPlugin plugin = GamblingPlugin.getInstance();

        boolean success = plugin.enableResourcePackAuto();
        if (success) {
            sender.sendMessage("§aResource pack is now self-hosted by the server!");
            sender.sendMessage("§7URL: §f" + plugin.getResourcePackUrl());
            sender.sendMessage("§7SHA-1: §f" + plugin.getResourcePackHash());
            sender.sendMessage("§7File: §f" + plugin.getResourcePackFile().getAbsolutePath());
            sender.sendMessage("§ePlayers will be prompted to download it on join.");
        } else {
            sender.sendMessage("§cFailed to self-host the resource pack. Check the console for details.");
        }
        return true;
    }

    private static boolean handleDisableResourcePack(CommandSender sender) {
        GamblingPlugin plugin = GamblingPlugin.getInstance();
        if (!plugin.isResourcePackEnabled()) {
            sender.sendMessage("§eNo self-hosted resource pack is currently active.");
            return true;
        }
        plugin.disableResourcePack();
        sender.sendMessage("§aResource pack disabled.");
        return true;
    }

    private static void sendStats(CommandSender sender, String targetName) {
        UUID uuid;
        if (targetName == null && sender instanceof Player) {
            uuid = ((Player) sender).getUniqueId();
        } else if (targetName != null) {
            Player onlineTarget = Bukkit.getPlayerExact(targetName);
            uuid = onlineTarget != null ? onlineTarget.getUniqueId() : Bukkit.getOfflinePlayer(targetName).getUniqueId();
        } else {
            sender.sendMessage("§cUsage: /casino stats <player>.");
            return;
        }

        GamblingPlugin plugin = GamblingPlugin.getInstance();
        de.winniepat.gamblingPlugin.util.PlayerData.CasinoStats stats =
                plugin.getPlayerData().getCasinoStats(uuid);
        sender.sendMessage("§6=== Casino Stats: " + plugin.getPlayerData().getName(uuid) + " ===");
        sender.sendMessage("§7Games played: §f" + stats.getGamesPlayed());
        sender.sendMessage("§7Total wagered: §e" + de.winniepat.gamblingPlugin.util.CasinoItems.money(stats.getTotalWagered()));
        sender.sendMessage("§7Total returned: §a" + de.winniepat.gamblingPlugin.util.CasinoItems.money(stats.getTotalReturned()));
        String netColor = stats.getNetProfit() >= 0 ? "§a" : "§c";
        sender.sendMessage("§7Net profit/loss: " + netColor
                + de.winniepat.gamblingPlugin.util.CasinoItems.money(stats.getNetProfit()));
        sender.sendMessage("§7Largest win: §a"
                + de.winniepat.gamblingPlugin.util.CasinoItems.money(stats.getLargestWin()));
    }

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
        if (args == null || args.length == 0 || args.length > 2) {
            return Collections.emptyList();
        }
        if (args.length == 2 && "stats".equalsIgnoreCase(args[0])) {
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
