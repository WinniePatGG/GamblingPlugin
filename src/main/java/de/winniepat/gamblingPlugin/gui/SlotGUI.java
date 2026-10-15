package de.winniepat.gamblingPlugin.gui;

import de.winniepat.gamblingPlugin.GamblingPlugin;
import de.winniepat.gamblingPlugin.util.Particles;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class SlotGUI implements Listener {

    private static final List<Material> symbols = Arrays.asList(
            Material.DIAMOND,
            Material.GOLD_INGOT,
            Material.EMERALD,
            Material.IRON_INGOT,
            Material.REDSTONE
    );

    private static final Map<Material, Double> multipliers = new HashMap<>();
    static {
        multipliers.put(Material.DIAMOND, 5.0);
        multipliers.put(Material.GOLD_INGOT, 3.0);
        multipliers.put(Material.EMERALD, 2.0);
        multipliers.put(Material.IRON_INGOT, 1.5);
        multipliers.put(Material.REDSTONE, 1.0);
    }

    private static final Random random = new Random();

    private static Sound soundFromConfig(String path, String fallback) {
        String name = GamblingPlugin.getInstance().getConfig().getString(path, fallback);
        try {
            return Sound.valueOf(name.toUpperCase());
        } catch (Exception ignored) {
            try {
                return Sound.valueOf(fallback.toUpperCase());
            } catch (Exception ignored2) {
                return Sound.ENTITY_PLAYER_LEVELUP;
            }
        }
    }

    private static final int[][] SLOT_INDEXES = {
            {3, 12, 21},
            {4, 13, 22},
            {5, 14, 23}
    };

    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final Set<UUID> roulettePlayers = new HashSet<>();
    private static final Map<UUID, Double> slotBaseRewards = new HashMap<>();
    private static final long COOLDOWN_MS = 5000;

    public static void openSlotGUI(Player player) {
        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();

        if (cooldowns.containsKey(uuid) && now - cooldowns.get(uuid) < COOLDOWN_MS) {
            return;
        }

        boolean freeSpinEnabled = GamblingPlugin.getInstance().getConfig().getBoolean("slots.free-spin-once-per-day", true);
        String today = java.time.LocalDate.now().toString();
        boolean useFreeSpin = freeSpinEnabled && !today.equals(GamblingPlugin.getInstance().getPlayerData().getLastFreeSpin(uuid));

        double cost = useFreeSpin ? 0.0 : GamblingPlugin.getInstance().getConfig().getDouble("slots.bet-cost", 100.0);

        if (!useFreeSpin && GamblingPlugin.getInstance().getEconomy().getBalance(player) < cost) {
            player.sendMessage("§cYou don't have enough money to bet! §7(Required: $" + (int) cost + ")");
            return;
        }

        if (useFreeSpin) {
            GamblingPlugin.getInstance().getPlayerData().setLastFreeSpin(uuid, today);
            GamblingPlugin.getInstance().getPlayerData().save();
            player.sendMessage("§aFree daily spin used!");
        }

        slotBaseRewards.put(uuid, (cost > 0) ? cost : GamblingPlugin.getInstance().getConfig().getDouble("slots.bet-cost", 100.0));

        cooldowns.put(uuid, now);
        roulettePlayers.add(uuid);

        if (cost > 0) {
            GamblingPlugin.getInstance().getEconomy().withdrawPlayer(player, cost);
        }
        player.sendMessage("§eSpinning...");

        Inventory inv = Bukkit.createInventory(null, 27, "§6Slot Machine");
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }
        player.openInventory(inv);

        spinColumn(player, inv, 0, 20, Sound.BLOCK_NOTE_BLOCK_HARP);
        spinColumn(player, inv, 1, 30, Sound.BLOCK_NOTE_BLOCK_HARP);
        spinColumn(player, inv, 2, 40, Sound.BLOCK_NOTE_BLOCK_HARP);
    }

    private static void spinColumn(Player player, Inventory inv, int col, int totalTicks, Sound stopSound) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= totalTicks) {
                    cancel();
                    player.playSound(player.getLocation(), stopSound, 1f, 1f);

                    if (col == 2) {
                        Bukkit.getScheduler().runTaskLater(GamblingPlugin.getInstance(), () -> checkWin(player, inv), 10L);
                    }
                    return;
                }

                inv.setItem(SLOT_INDEXES[col][2], inv.getItem(SLOT_INDEXES[col][1]));
                inv.setItem(SLOT_INDEXES[col][1], inv.getItem(SLOT_INDEXES[col][0]));
                inv.setItem(SLOT_INDEXES[col][0], new ItemStack(symbols.get(random.nextInt(symbols.size()))));

                ItemStack yellowGlassLeft = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
                ItemMeta metaDown = yellowGlassLeft.getItemMeta();
                if (metaDown != null) {
                    metaDown.setDisplayName("§e←");
                    yellowGlassLeft.setItemMeta(metaDown);
                }
                inv.setItem(15, yellowGlassLeft);

                ItemStack yellowGlassRight = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
                ItemMeta metaUp = yellowGlassRight.getItemMeta();
                if (metaUp != null) {
                    metaUp.setDisplayName("§e→");
                    yellowGlassRight.setItemMeta(metaUp);
                }
                inv.setItem(11, yellowGlassRight);

                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.4f);
                ticks++;
            }
        }.runTaskTimer(GamblingPlugin.getInstance(), 0L, 6L);
    }

    private static void checkWin(Player player, Inventory inv) {
        ItemStack mid1 = inv.getItem(12);
        ItemStack mid2 = inv.getItem(13);
        ItemStack mid3 = inv.getItem(14);

        UUID uuid = player.getUniqueId();
        roulettePlayers.remove(uuid);

        if (mid1 == null || mid2 == null || mid3 == null) {
            player.sendMessage("§cError: missing items in GUI.");
            return;
        }

        if (mid1.getType() == mid2.getType() && mid2.getType() == mid3.getType()) {
            Material matched = mid1.getType();
            double baseReward = slotBaseRewards.remove(uuid);
            if (baseReward <= 0) baseReward = GamblingPlugin.getInstance().getConfig().getDouble("slots.bet-cost", 100.0);
            double multiplier = GamblingPlugin.getInstance().getConfig().getDouble("slots.multipliers." + matched.name(),
                    multipliers.getOrDefault(matched, 1.0));
            int streak = GamblingPlugin.getInstance().getPlayerData().getSlotStreak(uuid) + 1;
            GamblingPlugin.getInstance().getPlayerData().setSlotStreak(uuid, streak);
            GamblingPlugin.getInstance().getPlayerData().save();
            double bonus = GamblingPlugin.getInstance().getConfig().getDouble("slots.win-streak-bonus", 0.25);
            double cap = GamblingPlugin.getInstance().getConfig().getDouble("slots.win-streak-cap", 2.0);
            double streakMultiplier = Math.min(1.0 + (streak - 1) * bonus, cap);
            double reward = baseReward * multiplier * streakMultiplier;

            GamblingPlugin.getInstance().getEconomy().depositPlayer(player, reward);
            GamblingPlugin.getInstance().getPlayerData().addWin(uuid, player.getName(), reward);
            GamblingPlugin.getInstance().getPlayerData().save();
            player.sendMessage("§aYou won $" + reward + "! (x" + multiplier + ") Streak: " + streak + "x");
            Sound winSound = soundFromConfig("audio.slot-win", "ENTITY_PLAYER_LEVELUP");
            player.playSound(player.getLocation(), winSound, 1f, 1f);
            Particle spark = Particles.byName(GamblingPlugin.getInstance().getConfig().getString("particles.slot-win", "ELECTRIC_SPARK"), "CRIT");
            if (spark != null) {
                player.getWorld().spawnParticle(spark, player.getLocation().add(0, 1.5, 0), 30, 0.3, 0.5, 0.3);
            }
        } else {
            GamblingPlugin.getInstance().getPlayerData().setSlotStreak(uuid, 0);
            GamblingPlugin.getInstance().getPlayerData().save();
            player.sendMessage("§cYou lost! Better luck next time.");
            Sound lossSound = soundFromConfig("audio.slot-loss", "BLOCK_NOTE_BLOCK_BASS");
            player.playSound(player.getLocation(), lossSound, 1f, 0.5f);
            Particle smoke = Particles.byName(GamblingPlugin.getInstance().getConfig().getString("particles.slot-loss", "SMOKE"), "SMOKE_NORMAL");
            if (smoke != null) {
                player.getWorld().spawnParticle(smoke, player.getLocation().add(0, 1.0, 0), 10, 0.2, 0.2, 0.2);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        Player player = (Player) e.getWhoClicked();

        if (title.equals("§6Slot Machine")
                || title.equals("Choose a Color to Bet")
                || title.startsWith("Roulette: ")
                || roulettePlayers.contains(player.getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getView().getTitle().equals("§6Slot Machine")) {
            e.setCancelled(true);
        }
    }


    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!e.getView().getTitle().equals("§6Slot Machine")) return;

        Player player = (Player) e.getPlayer();
        if (roulettePlayers.contains(player.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(GamblingPlugin.getInstance(), () -> {
                player.openInventory(e.getInventory());
                player.sendMessage("§cYou can't close the slot machine while it's spinning!");
            }, 2L);
        }
    }
}
