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

        String project = "VPVNKnOS";
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
                    if (latestVersion != null && isNewerVersion(latestVersion, current)) {
                        updateAvailable = true;
                        plugin.getLogger().warning("A new version of GamblingPlugin is available: " + latestVersion
                                + " (current: " + current + "). Run /casinoupdate to download it.");
                    } else if (latestVersion != null && !latestVersion.equalsIgnoreCase(current)) {
                        plugin.getLogger().info("GamblingPlugin " + current
                                + " is newer than the published " + latestVersion + ". Skipping update.");
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

    /** True when the published version is strictly greater than the running one. */
    static boolean isNewerVersion(String remote, String current) {
        int[] remoteParts = parseVersion(remote);
        int[] currentParts = parseVersion(current);
        for (int i = 0; i < 3; i++) {
            if (remoteParts[i] != currentParts[i]) {
                return remoteParts[i] > currentParts[i];
            }
        }
        return false;
    }

    private static int[] parseVersion(String version) {
        int[] parts = new int[3];
        if (version != null) {
            String[] split = version.split("\\.");
            for (int i = 0; i < split.length && i < 3; i++) {
                try {
                    parts[i] = Integer.parseInt(split[i].trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return parts;
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