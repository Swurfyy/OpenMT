package nl.openminetopia.modules.police.listeners;

import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BodysearchInventoryListener implements Listener {

    // Track players who are currently bodysearching (viewing inventory via /fouilleer)
    private static final Set<UUID> bodysearchingPlayers = new HashSet<>();

    /**
     * Mark a player as currently bodysearching
     */
    public static void startBodysearch(Player player) {
        bodysearchingPlayers.add(player.getUniqueId());
    }

    /**
     * Stop tracking a player as bodysearching
     */
    public static void stopBodysearch(Player player) {
        bodysearchingPlayers.remove(player.getUniqueId());
    }

    /**
     * Check if a player is currently bodysearching
     */
    public static boolean isBodysearching(Player player) {
        return bodysearchingPlayers.contains(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // Only apply restrictions if player is bodysearching (opened via /fouilleer)
        if (!isBodysearching(player)) return;
        
        // Check if item removal is disabled
        if (OpenMinetopia.getDefaultConfiguration().isBodysearchAllowItemRemoval()) return;
        
        // Check if player is viewing someone else's inventory
        if (event.getInventory().getType() != InventoryType.PLAYER) return;
        if (event.getInventory().getHolder() == player) return;
        
        // Check if the clicked slot is in the target's inventory (not the player's own inventory)
        if (event.getSlotType() == InventoryType.SlotType.OUTSIDE) return;
        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory().getHolder() == player) return;
        
        // Cancel the event to prevent item removal
        event.setCancelled(true);
        
        // Send message to player
        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getOnlineMinetopiaPlayer(player);
        if (minetopiaPlayer != null) {
            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("police_bodysearch_item_removal_disabled"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // Only apply restrictions if player is bodysearching (opened via /fouilleer)
        if (!isBodysearching(player)) return;
        
        // Check if item removal is disabled
        if (OpenMinetopia.getDefaultConfiguration().isBodysearchAllowItemRemoval()) return;
        
        // Check if player is viewing someone else's inventory
        if (event.getInventory().getType() != InventoryType.PLAYER) return;
        if (event.getInventory().getHolder() == player) return;
        
        // Cancel the event to prevent item removal
        event.setCancelled(true);
        
        // Send message to player
        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getOnlineMinetopiaPlayer(player);
        if (minetopiaPlayer != null) {
            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("police_bodysearch_item_removal_disabled"));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        // Stop tracking when player closes inventory
        stopBodysearch(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up tracking when player disconnects
        stopBodysearch(event.getPlayer());
    }
}
