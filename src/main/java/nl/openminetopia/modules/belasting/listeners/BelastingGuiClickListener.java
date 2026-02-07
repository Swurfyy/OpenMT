package nl.openminetopia.modules.belasting.listeners;

import nl.openminetopia.modules.belasting.gui.BelastingMenuHolder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Handles clicks in the belasting payment GUI when it is opened via ItemsAdder.
 */
public class BelastingGuiClickListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BelastingMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot >= 0 && slot < 54 && holder.isActionSlot(slot)) {
            holder.runAction(slot);
        }
    }
}
