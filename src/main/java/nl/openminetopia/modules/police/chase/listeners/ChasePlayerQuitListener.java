package nl.openminetopia.modules.police.chase.listeners;

import nl.openminetopia.modules.police.chase.ChaseManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class ChasePlayerQuitListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ChaseManager chaseManager = ChaseManager.getInstance();

        // Check if the player who quit is being chased
        if (chaseManager.isBeingChased(player)) {
            // Ban the player for logging out during chase
            chaseManager.banPlayerForLogout(player);
            // Stop the chase
            chaseManager.stopChase(player.getUniqueId());
        }

        // Check if the player who quit is an agent in any chase
        chaseManager.stopChaseByAgent(player);
    }
}
