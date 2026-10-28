package de.winniepat.gamblingPlugin.gui;

import de.winniepat.gamblingPlugin.GamblingPlugin;
import de.winniepat.gamblingPlugin.util.ActiveGames;
import de.winniepat.gamblingPlugin.util.CasinoItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Wheel of Fortune: a weighted multi-tier wheel spread over the roulette-style
 * edge ring. The spin decelerates and lands on a segment; the payout is the
 * segment multiplier applied to the bet (multipliers are total money returned,
 * so 0x loses, 2x doubles). If the GUI is closed mid-spin the round still
 * resolves and pays out.
 */
public class WheelGUI implements Listener {

    static final String TITLE = "§5Wheel of Fortune";

    private static final int SLOT_STATUS = 31;
    private static final int[] LEGEND = {10, 11, 12, 13, 14, 15, 16};

    private static final Random random = new Random();
    private static final Set<UUID> spinning = new HashSet<>();

    /** One configured wheel segment. */
    private static final class Segment {
        final double multiplier;
        final int weight;
        final Material color;

        Segment(double multiplier, int weight, Material color) {
            this.multiplier = multiplier;
            this.weight = weight;
            this.color = color;
        }
    }

    private static final class Ring {
        final ItemStack[] items = new ItemStack[RouletteGUI.EDGE_SLOTS.length];
        final double[] multipliers = new double[RouletteGUI.EDGE_SLOTS.length];
    }

    static double betCost() {
        return GamblingPlugin.getInstance().getConfig().getDouble("wheel.bet-cost", 100.0);
    }

    public static void open(Player player) {
        if (spinning.contains(player.getUniqueId())) {
            player.sendMessage("§cThe wheel is still spinning!");
            return;
        }

        double bet = betCost();
        if (GamblingPlugin.getInstance().getEconomy().getBalance(player) < bet) {
            player.sendMessage("§cYou need at least §e" + CasinoItems.money(bet) + " §cto spin the wheel.");
            return;
        }
        GamblingPlugin.getInstance().getEconomy().withdrawPlayer(player, bet);

        List<Segment> segments = loadSegments();
        Ring ring = buildRing(segments);
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        CasinoItems.fillAll(inv, Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < ring.items.length; i++) {
            inv.setItem(RouletteGUI.EDGE_SLOTS[i], ring.items[i]);
        }
        renderLegend(inv, segments);
        inv.setItem(SLOT_STATUS, CasinoItems.item(Material.CLOCK, "§5§lThe wheel is spinning..."));

        spinning.add(player.getUniqueId());
        ActiveGames.join(player.getUniqueId(), "wheel");
        player.openInventory(inv);
        player.sendMessage("§7Wheel bet: §c-" + CasinoItems.money(bet) + " §8| §7Good luck!");

        final UUID uuid = player.getUniqueId();
        final ItemStack[] ringSnapshot = ring.items.clone();
        final double[] ringMultipliers = ring.multipliers.clone();
        Bukkit.getScheduler().runTaskLater(GamblingPlugin.getInstance(),
                () -> spinStep(player, uuid, bet, ringSnapshot, ringMultipliers, 0, 55 + random.nextInt(25)), 15L);
    }

    private static void spinStep(Player player, UUID uuid, double bet, ItemStack[] ringItems,
                                 double[] multipliers, int step, int total) {
        int ringIndex = step % RouletteGUI.EDGE_SLOTS.length;
        int slot = RouletteGUI.EDGE_SLOTS[ringIndex];

        Inventory inv = null;
        if (player.isOnline()) {
            String title = de.winniepat.gamblingPlugin.util.ViewTitles.title(player.getOpenInventory());
            if (title.equals(TITLE)) {
                inv = player.getOpenInventory().getTopInventory();
                inv.setItem(slot, CasinoItems.pane(Material.YELLOW_STAINED_GLASS_PANE, "§e*"));
                if (step > 0) {
                    int previousSlot = RouletteGUI.EDGE_SLOTS[(step - 1) % RouletteGUI.EDGE_SLOTS.length];
                    inv.setItem(previousSlot, ringItems[(step - 1) % RouletteGUI.EDGE_SLOTS.length]);
                }
            }
            float pitch = Math.max(0.6f, 1.6f - step * 0.012f);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, pitch);
        }

        if (step + 1 >= total) {
            land(player, uuid, bet, multipliers, ringIndex, inv);
            return;
        }

        final int delay = Math.min(14, 2 + (step * step) / 220);
        Bukkit.getScheduler().runTaskLater(GamblingPlugin.getInstance(),
                () -> spinStep(player, uuid, bet, ringItems, multipliers, step + 1, total), delay);
    }

    private static void land(Player player, UUID uuid, double bet, double[] multipliers,
                             int ringIndex, Inventory inv) {
        spinning.remove(uuid);
        ActiveGames.leave(uuid);

        double multiplier = multipliers[ringIndex];
        double payout = bet * multiplier;
        double profit = payout - bet;

        if (payout > 0) {
            OfflinePlayer target = player.isOnline() ? player : Bukkit.getOfflinePlayer(uuid);
            GamblingPlugin.getInstance().getEconomy().depositPlayer(target, payout);
            if (profit > 0) {
                GamblingPlugin.getInstance().getPlayerData().addWin(uuid, player.getName(), profit);
                GamblingPlugin.getInstance().getPlayerData().save();
            }
        }

        String label = CasinoItems.multiplier(multiplier);
        if (player.isOnline()) {
            if (profit > 0) {
                player.sendMessage("§5The wheel landed on §l" + label + " §5— §aYou won §e" + CasinoItems.money(profit));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            } else if (profit == 0) {
                player.sendMessage("§eThe wheel landed on " + label + " — money back.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
            } else {
                player.sendMessage("§cThe wheel landed on " + label + " §7— better luck next time!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            }

            if (inv != null) {
                inv.setItem(SLOT_STATUS, CasinoItems.item(Material.PAPER,
                        "§5§lResult: " + label,
                        profit > 0 ? "§aYou won " + CasinoItems.money(profit) : "§7No win this time."));
            }
        }
    }

    private static List<Segment> loadSegments() {
        List<Segment> segments = new ArrayList<>();
        ConfigurationSection section = GamblingPlugin.getInstance().getConfig().getConfigurationSection("wheel.segments");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection seg = section.getConfigurationSection(key);
                if (seg == null) continue;
                double multiplier = seg.getDouble("multiplier", -1.0);
                int weight = seg.getInt("weight", 0);
                String colorName = seg.getString("color", "GRAY_WOOL");
                if (multiplier < 0 || weight <= 0) continue;
                Material color = Material.matchMaterial(colorName);
                segments.add(new Segment(multiplier, weight, color == null ? Material.GRAY_WOOL : color));
            }
        }
        if (segments.isEmpty()) {
            segments.add(new Segment(0.0, 15, Material.BLACK_WOOL));
            segments.add(new Segment(1.25, 6, Material.GREEN_WOOL));
            segments.add(new Segment(2.0, 4, Material.LIME_WOOL));
            segments.add(new Segment(3.0, 2, Material.RED_WOOL));
            segments.add(new Segment(5.0, 1, Material.ORANGE_WOOL));
        }
        if (segments.size() > RouletteGUI.EDGE_SLOTS.length) {
            return segments.subList(0, RouletteGUI.EDGE_SLOTS.length);
        }
        return segments;
    }

    /** Spreads segment weights across the ring slots (largest-remainder method). */
    private static Ring buildRing(List<Segment> segments) {
        Ring ring = new Ring();
        int slots = RouletteGUI.EDGE_SLOTS.length;

        double totalWeight = 0;
        for (Segment segment : segments) {
            totalWeight += segment.weight;
        }
        if (totalWeight <= 0) {
            return ring;
        }

        double[] quota = new double[segments.size()];
        int[] counts = new int[segments.size()];
        int assigned = 0;
        for (int i = 0; i < segments.size(); i++) {
            quota[i] = segments.get(i).weight / totalWeight * slots;
            counts[i] = (int) Math.floor(quota[i]);
            assigned += counts[i];
        }

        // Hand the leftover slots to the largest fractional parts...
        Integer[] byRemainder = new Integer[segments.size()];
        for (int i = 0; i < segments.size(); i++) {
            byRemainder[i] = i;
        }
        java.util.Arrays.sort(byRemainder, (a, b) -> Double.compare(quota[b] - Math.floor(quota[b]), quota[a] - Math.floor(quota[a])));
        for (int i = 0; assigned < slots && i < byRemainder.length; i++) {
            counts[byRemainder[i]]++;
            assigned++;
        }

        // ...and make sure every segment keeps at least one slot.
        for (int i = 0; i < segments.size(); i++) {
            if (counts[i] == 0) {
                int donor = 0;
                for (int j = 1; j < segments.size(); j++) {
                    if (counts[j] > counts[donor]) donor = j;
                }
                if (donor != i && counts[donor] > 1) {
                    counts[donor]--;
                    counts[i]++;
                }
            }
        }

        int slotPos = 0;
        for (int i = 0; i < segments.size() && slotPos < slots; i++) {
            Segment segment = segments.get(i);
            ItemStack item = CasinoItems.pane(segment.color, "§7Pays §f" + CasinoItems.multiplier(segment.multiplier));
            for (int n = 0; n < counts[i] && slotPos < slots; n++) {
                ring.multipliers[slotPos] = segment.multiplier;
                ring.items[slotPos] = item;
                slotPos++;
            }
        }
        // Any rounding slack lands on the first segment.
        while (slotPos < slots) {
            Segment first = segments.get(0);
            ring.multipliers[slotPos] = first.multiplier;
            ring.items[slotPos] = CasinoItems.pane(first.color, "§7Pays §f" + CasinoItems.multiplier(first.multiplier));
            slotPos++;
        }
        return ring;
    }

    private static void renderLegend(Inventory inv, List<Segment> segments) {
        for (int slot : LEGEND) {
            inv.setItem(slot, CasinoItems.pane(Material.GRAY_STAINED_GLASS_PANE, " "));
        }
        int shown = 0;
        for (Segment segment : segments) {
            if (shown >= LEGEND.length) break;
            inv.setItem(LEGEND[shown], CasinoItems.pane(segment.color,
                    "§f" + CasinoItems.multiplier(segment.multiplier)));
            shown++;
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (de.winniepat.gamblingPlugin.util.ViewTitles.title(event.getView()).equals(TITLE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (de.winniepat.gamblingPlugin.util.ViewTitles.title(event.getView()).equals(TITLE)) {
            event.setCancelled(true);
        }
    }

    public static void clearState() {
        spinning.clear();
    }
}
