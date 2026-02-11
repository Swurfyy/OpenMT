package nl.openminetopia.modules.player.listeners;

import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PlayerJoinListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void playerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Data should already be loaded from PlayerPreLoginEvent
        // Try to get it synchronously with timeout to avoid blocking the server thread
        try {
            MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getMinetopiaPlayer(player)
                    .get(1, TimeUnit.SECONDS); // 1 second timeout to prevent server freeze
            
            if (minetopiaPlayer == null) {
                OpenMinetopia.getInstance().getLogger().severe("MinetopiaPlayer is null for " + player.getName() + " during join!");
                // Fallback to async loading
                handleAsync(player);
                return;
            }
            
            // ALWAYS add to onlinePlayers map when player joins
            // This ensures getOnlineMinetopiaPlayer() works correctly immediately after join
            PlayerManager.getInstance().getOnlinePlayers().put(player.getUniqueId(), minetopiaPlayer);
            OpenMinetopia.getInstance().getLogger().info("Added " + player.getName() + " to onlinePlayers map during join");
        } catch (TimeoutException e) {
            // Timeout occurred - fallback to async loading
            OpenMinetopia.getInstance().getLogger().warning("Timeout loading player data for " + player.getName() + " during join, falling back to async");
            handleAsync(player);
        } catch (Exception e) {
            OpenMinetopia.getInstance().getLogger().severe("Failed to load player data for " + player.getName() + " during join: " + e.getMessage());
            e.printStackTrace();
            // Fallback to async loading
            handleAsync(player);
        }
    }
    
    private void handleAsync(Player player) {
        // Fallback: load asynchronously and add to map when ready
        PlayerManager.getInstance().getMinetopiaPlayer(player).whenComplete((minetopiaPlayer, throwable) -> {
            if (throwable != null) {
                OpenMinetopia.getInstance().getLogger().severe("Failed to load player data asynchronously for " + player.getName() + ": " + throwable.getMessage());
                return;
            }
            
            if (minetopiaPlayer == null) {
                OpenMinetopia.getInstance().getLogger().severe("MinetopiaPlayer is null for " + player.getName() + " during async join!");
                return;
            }
            
            // Run on main thread to safely modify the map
            Bukkit.getScheduler().runTask(OpenMinetopia.getInstance(), () -> {
                PlayerManager.getInstance().getOnlinePlayers().put(player.getUniqueId(), minetopiaPlayer);
                OpenMinetopia.getInstance().getLogger().info("Added " + player.getName() + " to onlinePlayers map during async join");
            });
        });
    }
}
