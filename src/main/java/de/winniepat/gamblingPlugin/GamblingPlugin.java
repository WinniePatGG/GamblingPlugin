package de.winniepat.gamblingPlugin;

import de.winniepat.gamblingPlugin.animations.CaseAnimation;
import de.winniepat.gamblingPlugin.commands.*;
import de.winniepat.gamblingPlugin.gui.*;
import de.winniepat.gamblingPlugin.listeners.*;
import de.winniepat.gamblingPlugin.util.ActiveGames;
import de.winniepat.gamblingPlugin.util.BetAmounts;
import de.winniepat.gamblingPlugin.util.MusicManager;
import de.winniepat.gamblingPlugin.util.PlayerData;
import de.winniepat.gamblingPlugin.util.Scheduler;
import de.winniepat.gamblingPlugin.util.ServerVersion;
import de.winniepat.modrinthupdatechecker.ModrinthUpdateChecker;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class GamblingPlugin extends JavaPlugin {

    private static GamblingPlugin instance;
    private Economy economy;
    private PlayerData playerData;
    private List<Double> betAmounts = BetAmounts.DEFAULT;

    ModrinthUpdateChecker checker = ModrinthUpdateChecker.builder()
            .projectId("VPVNKnOS")
            .currentVersion(getDescription().getVersion())
            .build();

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        loadBetAmounts();

        playerData = new PlayerData(this);

        String serverVersion = ServerVersion.current();
        if (ServerVersion.isSupported(serverVersion)) {
            getLogger().info("GamblingPlugin enabled on Minecraft " + serverVersion
                    + " (" + (Scheduler.isFolia() ? "Folia" : "Paper/Spigot") + ").");
        } else {
            getLogger().warning("Running on unsupported Minecraft version " + serverVersion
                    + "! Supported range: " + ServerVersion.MIN_SUPPORTED + " - " + ServerVersion.MAX_SUPPORTED
                    + ". The plugin may not work correctly.");
        }

        if (!setupEconomy()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        registerCommands();
        registerEvents();
        registerListeners();

        checker.checkForUpdates().thenAccept(result -> {
            if (result.isUpdateAvailable()) {
                getLogger().warning("A new version of GamblingPlugin is available: " + result.latestVersion());
                result.downloadUrl().ifPresent(url -> getLogger().warning("Download it here: " + url));
            }
        });
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault not found! Disabling plugin.");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().severe("No Economy provider found! Disabling plugin.");
            return false;
        }
        economy = rsp.getProvider();
        getLogger().info("Economy setup successful with provider: " + economy.getName());
        return true;
    }

    public static GamblingPlugin getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }

    public PlayerData getPlayerData() {
        return playerData;
    }

    public List<Double> getBetAmounts() {
        return betAmounts;
    }

    public void reloadPluginConfig() {
        reloadConfig();
        loadBetAmounts();
        if (playerData != null) {
            playerData.load();
        }
        MusicManager.handleConfigReload();
    }

    private void loadBetAmounts() {
        betAmounts = BetAmounts.load(getConfig(), message -> getLogger().warning(message));
    }

    @Override
    public void onDisable() {
        if (playerData != null) {
            playerData.save();
        }
        SlotGUI.clearState();
        DoubleGUI.clearState();
        RouletteGUI.clearState();
        CaseAnimation.clearState();
        BlackjackGUI.clearState();
        MinesGUI.clearState();
        CrashGUI.clearState();
        CoinflipGUI.clearState();
        WheelGUI.clearState();
        ActiveGames.clear();
        MusicManager.clearAll();
        stopHttpServer();
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new SlotGUI(), this);
        getServer().getPluginManager().registerEvents(new DoubleGUI(), this);
        getServer().getPluginManager().registerEvents(new RouletteGUI(), this);
        getServer().getPluginManager().registerEvents(new MenuGUI(), this);
        getServer().getPluginManager().registerEvents(new BlackjackGUI(), this);
        getServer().getPluginManager().registerEvents(new MinesGUI(), this);
        getServer().getPluginManager().registerEvents(new CrashGUI(), this);
        getServer().getPluginManager().registerEvents(new CoinflipGUI(), this);
        getServer().getPluginManager().registerEvents(new WheelGUI(), this);
        getServer().getPluginManager().registerEvents(new BetSelectionGUI(), this);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new SlotListener(), this);
        getServer().getPluginManager().registerEvents(new DoubleInteractListener(), this);
        getServer().getPluginManager().registerEvents(new CaseListener(), this);
        getServer().getPluginManager().registerEvents(new RouletteListener(this), this);
        getServer().getPluginManager().registerEvents(new StationListener(), this);
        getServer().getPluginManager().registerEvents(new MusicListener(), this);
        getServer().getPluginManager().registerEvents(new ResourcePackListener(), this);
    }

    private void registerCommands() {
        CasinoCommand casinoCommand = new CasinoCommand();
        Objects.requireNonNull(getCommand("casino")).setExecutor(casinoCommand);
        Objects.requireNonNull(getCommand("casino")).setTabCompleter(casinoCommand);
    }

    // ===== Resource Pack Self-Hosting =====

    private static final String RESOURCE_PACK_FILENAME = "gambling-plugin-resource-pack.zip";
    private static final String RESOURCE_PACK_SOURCE_DIR = "resourcepack";
    private static final String RESOURCE_PACK_PATH = "/" + RESOURCE_PACK_FILENAME;

    private File resourcePackFile;
    private String resourcePackHash;
    private String resourcePackUrl;
    private boolean resourcePackRequired;
    private com.sun.net.httpserver.HttpServer httpServer;

    /**
     * Builds the bundled resource pack, hosts it on an embedded HTTP server, and
     * applies it as the server resource pack. No external hosting required.
     *
     * @return true if successful, false otherwise
     */
    public boolean enableResourcePackAuto() {
        try {
            // Stop any previous server first
            stopHttpServer();

            // Build the resource pack zip into the plugin data folder
            if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
                getLogger().severe("Could not create plugin data folder: " + getDataFolder());
                return false;
            }
            resourcePackFile = new File(getDataFolder(), RESOURCE_PACK_FILENAME);

            if (!buildResourcePackZip(resourcePackFile)) {
                getLogger().severe("Failed to build resource pack zip");
                return false;
            }

            // Compute SHA-1 hash so clients can verify the download
            resourcePackHash = computeSHA1(resourcePackFile);
            if (resourcePackHash == null) {
                getLogger().severe("Failed to compute SHA-1 hash");
                return false;
            }

            // Read host/port settings from config
            String host = getConfig().getString("resourcepack.host", "auto");
            int port = getConfig().getInt("resourcepack.port", 8123);
            if (port < 1 || port > 65535) {
                getLogger().warning("Invalid resourcepack.port " + port + ", falling back to 8123");
                port = 8123;
            }

            // Start the embedded HTTP server
            if (!startHttpServer(port)) {
                return false;
            }

            // Determine the address clients should use to reach this server
            String publicHost = resolvePublicHost(host);
            resourcePackUrl = "http://" + publicHost + ":" + port + RESOURCE_PACK_PATH;

            // Apply the resource pack to the server
            boolean required = getConfig().getBoolean("resourcepack.required", false);
            resourcePackRequired = required;
            String prompt = getConfig().getString("resourcepack.prompt", "This server uses a custom resource pack!");

            boolean success = setServerResourcePack(resourcePackUrl, resourcePackHash, required, prompt);
            if (success) {
                getLogger().info("Resource pack is now self-hosted at " + resourcePackUrl);
                getLogger().info("Resource pack SHA-1: " + resourcePackHash);
                if ("auto".equalsIgnoreCase(host)) {
                    getLogger().info("Set resourcepack.host in config.yml if players cannot reach this address.");
                }
            } else {
                stopHttpServer();
            }
            return success;

        } catch (Exception e) {
            getLogger().severe("Failed to enable resource pack auto-hosting: " + e.getMessage());
            stopHttpServer();
            return false;
        }
    }

    /**
     * Stops hosting and clears the server resource pack.
     */
    public void disableResourcePack() {
        // Clear the resource pack from all online players (newer Paper API)
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                java.lang.reflect.Method m = player.getClass().getMethod("removeResourcePacks");
                m.invoke(player);
            } catch (Exception ignored) {}
        }

        // Older API: Server.setResourcePack("") clears it
        try {
            java.lang.reflect.Method m = getServer().getClass().getMethod("setResourcePack", String.class);
            m.invoke(getServer(), "");
        } catch (Exception ignored) {}

        stopHttpServer();
        resourcePackFile = null;
        resourcePackHash = null;
        resourcePackUrl = null;
        resourcePackRequired = false;
        getLogger().info("Resource pack hosting disabled");
    }

    /**
     * Starts the embedded HTTP server serving the resource pack file.
     */
    private boolean startHttpServer(int port) {
        try {
            httpServer = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(port), 0);
            httpServer.createContext(RESOURCE_PACK_PATH, exchange -> {
                try {
                    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        exchange.sendResponseHeaders(405, -1);
                        exchange.close();
                        return;
                    }
                    byte[] data = Files.readAllBytes(resourcePackFile.toPath());
                    exchange.getResponseHeaders().set("Content-Type", "application/zip");
                    exchange.getResponseHeaders().set("Content-Length", String.valueOf(data.length));
                    exchange.sendResponseHeaders(200, data.length);
                    try (java.io.OutputStream os = exchange.getResponseBody()) {
                        os.write(data);
                    }
                } catch (Exception e) {
                    getLogger().warning("Error serving resource pack: " + e.getMessage());
                    try {
                        exchange.sendResponseHeaders(500, -1);
                    } catch (IOException ignored) {}
                } finally {
                    exchange.close();
                }
            });
            httpServer.setExecutor(java.util.concurrent.Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "GamblingPlugin-ResourcePack-HTTP");
                t.setDaemon(true);
                return t;
            }));
            httpServer.start();
            getLogger().info("Resource pack HTTP server listening on port " + port);
            return true;
        } catch (IOException e) {
            getLogger().severe("Could not start resource pack HTTP server on port " + port + ": " + e.getMessage());
            httpServer = null;
            return false;
        }
    }

    private void stopHttpServer() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }
    }

    /**
     * Resolves the host clients use to reach the HTTP server.
     */
    private String resolvePublicHost(String configuredHost) {
        if (configuredHost != null && !configuredHost.isBlank() && !"auto".equalsIgnoreCase(configuredHost)) {
            return configuredHost;
        }

        // Fall back to the address the game server is bound to, then the local host
        try {
            java.lang.reflect.Method m = getServer().getClass().getMethod("getAddress");
            Object address = m.invoke(getServer());
            if (address instanceof java.net.InetSocketAddress) {
                String host = ((java.net.InetSocketAddress) address).getHostString();
                if (host != null && !host.isBlank() && !"0.0.0.0".equals(host)) {
                    return host;
                }
            }
        } catch (Exception ignored) {}

        return "localhost";
    }

    /**
     * Builds the resource pack zip from the plugin's embedded resources.
     */
    private boolean buildResourcePackZip(File outputFile) {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile))) {
            java.net.URL resourceUrl = getClass().getClassLoader().getResource(RESOURCE_PACK_SOURCE_DIR);
            if (resourceUrl == null) {
                getLogger().severe("Resource pack source directory not found: " + RESOURCE_PACK_SOURCE_DIR);
                return false;
            }

            if ("jar".equals(resourceUrl.getProtocol())) {
                return buildResourcePackFromJar(zos);
            } else if ("file".equals(resourceUrl.getProtocol())) {
                return buildResourcePackFromFiles(zos, new File(resourceUrl.toURI()));
            }
            getLogger().severe("Unsupported resource URL protocol: " + resourceUrl.getProtocol());
            return false;
        } catch (Exception e) {
            getLogger().severe("Error building resource pack zip: " + e.getMessage());
            return false;
        }
    }

    private boolean buildResourcePackFromJar(ZipOutputStream zos) throws IOException, java.net.URISyntaxException {
        java.net.URL classUrl = getClass().getProtectionDomain().getCodeSource().getLocation();
        if (!"file".equals(classUrl.getProtocol())) {
            getLogger().severe("Cannot access plugin JAR: " + classUrl);
            return false;
        }

        File jarFile = new File(classUrl.toURI());
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
            java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                java.util.jar.JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith(RESOURCE_PACK_SOURCE_DIR + "/") && !entry.isDirectory()) {
                    String entryName = name.substring((RESOURCE_PACK_SOURCE_DIR + "/").length());
                    try (InputStream is = jar.getInputStream(entry)) {
                        zos.putNextEntry(new ZipEntry(entryName));
                        is.transferTo(zos);
                        zos.closeEntry();
                    }
                }
            }
        }
        return true;
    }

    private boolean buildResourcePackFromFiles(ZipOutputStream zos, File sourceDir) throws IOException {
        try (java.util.stream.Stream<java.nio.file.Path> paths = Files.walk(sourceDir.toPath())) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                try {
                    String relativePath = sourceDir.toPath().relativize(file).toString().replace('\\', '/');
                    zos.putNextEntry(new ZipEntry(relativePath));
                    Files.copy(file, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    getLogger().warning("Error adding file to zip: " + file + " - " + e.getMessage());
                }
            });
        }
        return true;
    }

    private String computeSHA1(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream is = Files.newInputStream(file.toPath())) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            getLogger().severe("Error computing SHA-1: " + e.getMessage());
            return null;
        }
    }

    /**
     * Applies the resource pack. Older Bukkit versions expose setResourcePack on
     * Server; newer Paper versions expose it per Player. We support both.
     */
    private boolean setServerResourcePack(String url, String hash, boolean required, String prompt) {
        // Older API: Server.setResourcePack(...)
        try {
            java.lang.reflect.Method m = getServer().getClass().getMethod("setResourcePack", String.class, byte[].class, boolean.class, String.class);
            byte[] hashBytes = hashToBytes(hash);
            m.invoke(getServer(), url, hashBytes, required, prompt);
            return true;
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            getLogger().severe("Error setting server resource pack: " + e.getMessage());
            return false;
        }

        // Newer API: Player.setResourcePack(...) - apply to everyone currently online.
        // Joining players are handled by ResourcePackListener.
        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                applyResourcePackToPlayer(player);
            }
            return true;
        } catch (Exception e) {
            getLogger().severe("Error applying resource pack to players: " + e.getMessage());
            return false;
        }
    }

    /**
     * Applies the active resource pack to a single player (newer Paper API).
     */
    private void applyResourcePackToPlayer(Player player) {
        if (resourcePackUrl == null || player == null) return;
        try {
            java.lang.reflect.Method m = player.getClass().getMethod("setResourcePack", String.class, String.class, boolean.class);
            m.invoke(player, resourcePackUrl, resourcePackHash, resourcePackRequired);
            return;
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            getLogger().warning("Could not apply resource pack to " + player.getName() + ": " + e.getMessage());
            return;
        }

        try {
            java.lang.reflect.Method m = player.getClass().getMethod("setResourcePack", String.class, byte[].class);
            m.invoke(player, resourcePackUrl, hashToBytes(resourcePackHash));
            return;
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            getLogger().warning("Could not apply resource pack to " + player.getName() + ": " + e.getMessage());
            return;
        }

        try {
            java.lang.reflect.Method m = player.getClass().getMethod("setResourcePack", String.class);
            m.invoke(player, resourcePackUrl);
        } catch (Exception e) {
            getLogger().warning("Could not apply resource pack to " + player.getName() + ": " + e.getMessage());
        }
    }

    private byte[] hashToBytes(String hash) {
        if (hash == null || hash.length() != 40) return null;
        byte[] bytes = new byte[20];
        try {
            for (int i = 0; i < 20; i++) {
                bytes[i] = (byte) Integer.parseInt(hash.substring(i * 2, i * 2 + 2), 16);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return bytes;
    }

    private class ResourcePackListener implements Listener {
        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            if (resourcePackUrl != null) {
                applyResourcePackToPlayer(event.getPlayer());
            }
        }
    }

    public String getResourcePackUrl() {
        return resourcePackUrl;
    }

    public String getResourcePackHash() {
        return resourcePackHash;
    }

    public File getResourcePackFile() {
        return resourcePackFile;
    }

    public boolean isResourcePackEnabled() {
        return resourcePackUrl != null;
    }
}
