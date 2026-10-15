package de.winniepat.gamblingPlugin;

import de.winniepat.gamblingPlugin.commands.*;
import de.winniepat.gamblingPlugin.gui.*;
import de.winniepat.gamblingPlugin.listeners.*;
import de.winniepat.gamblingPlugin.util.ServerVersion;
import de.winniepat.gamblingPlugin.util.UpdateChecker;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class GamblingPlugin extends JavaPlugin {

    private static GamblingPlugin instance;
    private Economy economy;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

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
        UpdateChecker.check(this);
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

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new SlotGUI(), this);
        getServer().getPluginManager().registerEvents(new DoubleGUI(), this);
        getServer().getPluginManager().registerEvents(new RouletteGUI(), this);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new SlotListener(), this);
        getServer().getPluginManager().registerEvents(new DoubleInteractListener(), this);
        getServer().getPluginManager().registerEvents(new CaseListener(), this);
        Bukkit.getPluginManager().registerEvents(new RouletteListener(this), this);
    }

    private void registerCommands() {
        Objects.requireNonNull(getCommand("slots")).setExecutor(new SlotCommand());
        Objects.requireNonNull(getCommand("double")).setExecutor(new DoubleCommand());
        Objects.requireNonNull(getCommand("roulette")).setExecutor(new RouletteCommand());
        Objects.requireNonNull(getCommand("case")).setExecutor(new CaseCommand());
        Objects.requireNonNull(getCommand("daily")).setExecutor(new DailyCommand());
        Objects.requireNonNull(getCommand("casinoupdate")).setExecutor(new UpdateCommand());
    }
}
