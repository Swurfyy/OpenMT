package nl.openminetopia.modules.police.listeners;

import nl.openminetopia.modules.police.utils.BalaclavaNameTagManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class BalaclavaNameTagListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        BalaclavaNameTagManager.getInstance().onPlayerJoin(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        BalaclavaNameTagManager.getInstance().onPlayerQuit(event.getPlayer());
    }
}
