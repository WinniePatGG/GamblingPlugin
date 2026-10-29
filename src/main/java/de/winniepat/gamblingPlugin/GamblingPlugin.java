package de.winniepat.gamblingPlugin;

import de.winniepat.gamblingPlugin.animations.CaseAnimation;
import de.winniepat.gamblingPlugin.commands.*;
import de.winniepat.gamblingPlugin.gui.*;
import de.winniepat.gamblingPlugin.listeners.*;
import de.winniepat.gamblingPlugin.util.ActiveGames;
import de.winniepat.gamblingPlugin.util.PlayerData;
import de.winniepat.gamblingPlugin.util.ServerVersion;
import de.winniepat.modrinthupdatechecker.ModrinthUpdateChecker;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class GamblingPlugin extends JavaPlugin {

    private static GamblingPlugin instance;
    private Economy economy;
    private PlayerData playerData;

    ModrinthUpdateChecker checker = ModrinthUpdateChecker.builder()
            .projectId("VPVNKnOS")
            .currentVersion(getDescription().getVersion())
            .build();

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        playerData = new PlayerData(this);

        String serverVersion = ServerVersion.current();
        if (ServerVersion.isSupported(serverVersion)) {
            getLogger().info("GamblingPlugin enabled on Minecraft " + serverVersion + ".");
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

    public void reloadPluginConfig() {
        reloadConfig();
        if (playerData != null) {
            playerData.load();
        }
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
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new SlotListener(), this);
        getServer().getPluginManager().registerEvents(new DoubleInteractListener(), this);
        getServer().getPluginManager().registerEvents(new CaseListener(), this);
        getServer().getPluginManager().registerEvents(new RouletteListener(this), this);
        getServer().getPluginManager().registerEvents(new StationListener(), this);
    }

    private void registerCommands() {
        CasinoCommand casinoCommand = new CasinoCommand();
        Objects.requireNonNull(getCommand("casino")).setExecutor(casinoCommand);
        Objects.requireNonNull(getCommand("casino")).setTabCompleter(casinoCommand);
    }
}
