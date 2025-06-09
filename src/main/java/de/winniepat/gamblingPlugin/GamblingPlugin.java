package de.winniepat.gamblingPlugin;

import de.winniepat.gamblingPlugin.commands.*;
import de.winniepat.gamblingPlugin.gui.*;
import de.winniepat.gamblingPlugin.listeners.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class GamblingPlugin extends JavaPlugin {

    private static GamblingPlugin instance;
    private Economy economy;

    @Override
    public void onEnable() {
        instance = this;

        if (!setupEconomy()) {
            getLogger().info("Vault not found or no economy plugin hooked! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Objects.requireNonNull(getCommand("slots")).setExecutor(new SlotCommand());
        Objects.requireNonNull(getCommand("double")).setExecutor(new DoubleCommand());
        Objects.requireNonNull(getCommand("roulette")).setExecutor(new RouletteCommand());
        Objects.requireNonNull(getCommand("case")).setExecutor(new CaseCommand(this));

        getServer().getPluginManager().registerEvents(new SlotGUI(), this);
        getServer().getPluginManager().registerEvents(new DoubleGUI(), this);

        getServer().getPluginManager().registerEvents(new SlotListener(), this);
        getServer().getPluginManager().registerEvents(new DoubleInteractListener(), this);
        getServer().getPluginManager().registerEvents(new CaseListener(this), this);
        Bukkit.getPluginManager().registerEvents(new RouletteListener(this), this);


        getLogger().info("GamblingPlugin enabled.");
        setupEconomy();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public static GamblingPlugin getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }
}
