package nl.openminetopia.modules.police.listeners;

import nl.openminetopia.modules.police.utils.BalaclavaUtils;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class BalaclavaInteractListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack itemInHand = event.getItem();
        if (itemInHand == null || !BalaclavaUtils.isBalaclavaItem(itemInHand)) {
            return;
        }

        event.setCancelled(true);

        // Create a copy with amount 1 for the helmet
        ItemStack itemToWear = itemInHand.clone();
        itemToWear.setAmount(1);

        // Handle old helmet - add it back to inventory or drop it
        if (event.getPlayer().getInventory().getHelmet() != null) {
            ItemStack oldHelmet = event.getPlayer().getInventory().getHelmet();
            HashMap<Integer, ItemStack> overflow = event.getPlayer().getInventory().addItem(oldHelmet);
            // Drop any items that couldn't fit in inventory
            for (ItemStack overflowItem : overflow.values()) {
                event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), overflowItem);
            }
        }

        // Set the helmet (only 1 item)
        event.getPlayer().getInventory().setHelmet(itemToWear);

        // Remove 1 item from the hand
        ItemStack remainingItem = itemInHand.clone();
        remainingItem.setAmount(itemInHand.getAmount() - 1);

        if (remainingItem.getAmount() > 0) {
            event.getPlayer().getInventory().setItemInMainHand(remainingItem);
        } else {
            event.getPlayer().getInventory().setItemInMainHand(null);
        }

        event.getPlayer().sendMessage(ChatUtils.color("<dark_aqua>Je hebt een bivakmuts op je hoofd gezet."));
    }
}
