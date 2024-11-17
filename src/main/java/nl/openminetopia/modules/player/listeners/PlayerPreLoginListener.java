package nl.openminetopia.modules.player.listeners;

import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.player.PlayerModule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class PlayerPreLoginListener implements Listener {

    @EventHandler
    public void playerPreLogin(final AsyncPlayerPreLoginEvent event) {
        PlayerManager.getInstance().getOnlinePlayers().remove(event.getUniqueId());

        PlayerModule playerModule = OpenMinetopia.getModuleManager().getModule(PlayerModule.class);

        try {
            playerModule.loadPlayer(event.getUniqueId());
        } catch (Exception e) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, MessageConfiguration.component("player_data_not_loaded"));
            OpenMinetopia.getInstance().getLogger().warning("Error loading player model: " + e.getMessage());
        }
    }
}
