package nl.openminetopia.modules.misc.listeners;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import nl.openminetopia.OpenMinetopia;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Listener to prevent old boots (created before the update) from working in hands.
 * Boots with speed modifiers that don't have EquipmentSlotGroup.FEET restriction
 * will be fixed to only work when worn on feet.
 */
public class BootsInHandListener implements Listener {

    /**
     * Check if an item is a boot type
     */
    private boolean isBoot(Material material) {
        return material == Material.LEATHER_BOOTS ||
               material == Material.CHAINMAIL_BOOTS ||
               material == Material.IRON_BOOTS ||
               material == Material.GOLDEN_BOOTS ||
               material == Material.DIAMOND_BOOTS ||
               material == Material.NETHERITE_BOOTS;
    }

    /**
     * Check if boots have old speed modifiers (without FEET restriction)
     * and fix them if needed
     * @return true if boots were fixed, false otherwise
     */
    private boolean checkAndFixBoots(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        if (!isBoot(item.getType())) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        // Get movement speed attribute
        Attribute movementSpeed = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ATTRIBUTE)
                .get(NamespacedKey.minecraft("movement_speed"));

        if (movementSpeed == null) {
            return false;
        }

        // Check if item has movement speed modifiers
        if (!meta.hasAttributeModifiers() || 
            meta.getAttributeModifiers(movementSpeed) == null ||
            meta.getAttributeModifiers(movementSpeed).isEmpty()) {
            return false;
        }

        // Check for modifiers without FEET restriction (old boots)
        List<AttributeModifier> oldModifiers = new ArrayList<>();
        for (AttributeModifier modifier : meta.getAttributeModifiers(movementSpeed)) {
            // Check if modifier doesn't have EquipmentSlotGroup.FEET restriction
            // In Paper/Spigot API, if getSlot() returns null, it works everywhere
            // We need to check if it's restricted to FEET or not
            EquipmentSlotGroup slotGroup = modifier.slot();
            
            // If slotGroup is null, it works everywhere (old behavior)
            // If slotGroup is not FEET, we also want to fix it
            if (slotGroup == null || slotGroup != EquipmentSlotGroup.FEET) {
                // Calculate level from speed value (level * 0.01 = speed)
                double speedValue = modifier.amount();
                int level = (int) Math.round(speedValue / 0.01);
                
                // Only fix level 3+ boots (0.03+ speed) - these should never work in hand
                if (level >= 3) {
                    oldModifiers.add(modifier);
                }
            }
        }

        // If we found old modifiers, fix them
        if (!oldModifiers.isEmpty()) {
            // Remove old modifiers
            for (AttributeModifier oldModifier : oldModifiers) {
                meta.removeAttributeModifier(movementSpeed, oldModifier);
            }

            // Add new modifiers with FEET restriction
            for (AttributeModifier oldModifier : oldModifiers) {
                double speedValue = oldModifier.amount();
                NamespacedKey newKey = new NamespacedKey(OpenMinetopia.getInstance(), 
                    "speed_boost_" + UUID.randomUUID());
                
                AttributeModifier newModifier = new AttributeModifier(
                    newKey,
                    speedValue,
                    oldModifier.operation(),
                    EquipmentSlotGroup.FEET
                );
                
                meta.addAttributeModifier(movementSpeed, newModifier);
            }

            item.setItemMeta(meta);
            return true;
        }
        
        return false;
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (checkAndFixBoots(player, item)) {
            // Update the item in inventory if it was fixed
            player.getInventory().setItem(event.getNewSlot(), item);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Check main hand
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (checkAndFixBoots(player, mainHand)) {
            player.getInventory().setItemInMainHand(mainHand);
        }
        // Check off hand
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (checkAndFixBoots(player, offHand)) {
            player.getInventory().setItemInOffHand(offHand);
        }
    }
}
