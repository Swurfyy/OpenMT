package nl.openminetopia.modules.belasting.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Holder for the belasting payment GUI when opened via ItemsAdder.
 * Used to identify the inventory in click events and run slot actions.
 */
public class BelastingMenuHolder implements InventoryHolder {

    private final int confirmSlot;
    private final int declineSlot;
    private final Runnable onConfirm;
    private final Runnable onDecline;
    private Inventory inventory;

    public BelastingMenuHolder(int confirmSlot, int declineSlot, Runnable onConfirm, Runnable onDecline) {
        this.confirmSlot = confirmSlot;
        this.declineSlot = declineSlot;
        this.onConfirm = onConfirm;
        this.onDecline = onDecline;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public void runAction(int slot) {
        if (slot == confirmSlot) {
            onConfirm.run();
        } else if (slot == declineSlot) {
            onDecline.run();
        }
    }

    public boolean isActionSlot(int slot) {
        return slot == confirmSlot || slot == declineSlot;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory != null ? inventory : org.bukkit.Bukkit.createInventory(this, 54);
    }
}
