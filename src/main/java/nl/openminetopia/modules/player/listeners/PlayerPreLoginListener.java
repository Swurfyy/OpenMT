package nl.openminetopia.modules.player.listeners;

import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.player.PlayerModule;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class PlayerPreLoginListener implements Listener {

    @EventHandler
    public void playerPreLogin(final AsyncPlayerPreLoginEvent event) {
        PlayerManager.getInstance().getOnlinePlayers().remove(event.getUniqueId());

        try {
            OfflinePlayer player = Bukkit.getOfflinePlayer(event.getUniqueId());
            PlayerManager.getInstance().getMinetopiaPlayer(player).whenComplete((minetopiaPlayer, throwable) -> {
                if (throwable != null) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, MessageConfiguration.component("player_data_not_loaded"));
                    OpenMinetopia.getInstance().getLogger().warning("Error loading player model: " + throwable.getMessage());
                    return;
                }

                if (minetopiaPlayer == null) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, MessageConfiguration.component("player_data_not_loaded"));
                    OpenMinetopia.getInstance().getLogger().warning("Error loading player model: Player not found");
                    return;
                }

                OpenMinetopia.getInstance().getLogger().info("Loaded MinetopiaPlayer " + player.getName());
            });
        } catch (Exception e) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, MessageConfiguration.component("player_data_not_loaded"));
            OpenMinetopia.getInstance().getLogger().warning("Error loading player model: " + e.getMessage());
        }
    }
}
