package de.winniepat.gamblingPlugin.gui.customui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MenuHolder implements InventoryHolder {
    private Inventory inventory;

    private final Map<Integer, Consumer<Player>> actions = new HashMap<>();

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public void setAction(int slot, Consumer<Player> action) {
        actions.put(slot, action);
    }

    public void execute(int slot, Player player) {
        Consumer<Player> action = actions.get(slot);

        if (action != null) {
            action.accept(player);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
