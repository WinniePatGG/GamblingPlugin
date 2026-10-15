package de.winniepat.gamblingPlugin.util;

import de.winniepat.gamblingPlugin.GamblingPlugin;
import org.bukkit.Bukkit;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateChecker {

    private static volatile boolean updateAvailable = false;
    private static volatile String latestVersion = null;
    private static volatile String downloadUrl = null;

    private UpdateChecker() {
    }

    public static void check(GamblingPlugin plugin) {
        check(plugin, false, null);
    }

    public static void check(GamblingPlugin plugin, Runnable after) {
        check(plugin, false, after);
    }

    public static void check(GamblingPlugin plugin, boolean force, Runnable after) {
        if (!force && !plugin.getConfig().getBoolean("updates.check-on-startup", true)) return;

        Runnable runAfter = () -> {
            if (after != null) Bukkit.getScheduler().runTask(plugin, after);
        };

        String project = plugin.getConfig().getString("updates.modrinth-project", "VPVNKnOS");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL("https://api.modrinth.com/v2/project/" + project + "/version?limit=1");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "GamblingPlugin/" + plugin.getDescription().getVersion());

                try (InputStream in = conn.getInputStream()) {
                    String json = new String(in.readAllBytes());

                    Matcher vm = Pattern.compile("\"version_number\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
                    if (vm.find()) latestVersion = vm.group(1);

                    Matcher um = Pattern.compile("\"url\"\\s*:\\s*\"(https://[^\"]+\\.jar)\"").matcher(json);
                    if (um.find()) downloadUrl = um.group(1);

                    String current = plugin.getDescription().getVersion();
                    if (latestVersion != null && !latestVersion.equalsIgnoreCase(current)) {
                        updateAvailable = true;
                        plugin.getLogger().warning("A new version of GamblingPlugin is available: " + latestVersion
                                + " (current: " + current + "). Run /casinoupdate to download it.");
                    } else {
                        plugin.getLogger().info("GamblingPlugin is up to date (" + current + ").");
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not check for updates: " + e.getMessage());
            } finally {
                runAfter.run();
            }
        });
    }

    public static boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public static String getLatestVersion() {
        return latestVersion;
    }

    public static String getDownloadUrl() {
        return downloadUrl;
    }

    public static void download(GamblingPlugin plugin, Runnable onSuccess, Runnable onFailure) {
        if (!updateAvailable || downloadUrl == null) {
            if (onFailure != null) Bukkit.getScheduler().runTask(plugin, onFailure);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Path pluginsDir = plugin.getDataFolder().getParentFile().toPath();
                Path updateDir = pluginsDir.resolve("update");
                Files.createDirectories(updateDir);
                Path target = updateDir.resolve("GamblingPlugin.jar");

                URL url = new URL(downloadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(30000);

                try (InputStream in = conn.getInputStream(); OutputStream out = Files.newOutputStream(target)) {
                    in.transferTo(out);
                }

                if (onSuccess != null) Bukkit.getScheduler().runTask(plugin, onSuccess);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to download update: " + e.getMessage());
                if (onFailure != null) Bukkit.getScheduler().runTask(plugin, onFailure);
            }
        });
    }
}