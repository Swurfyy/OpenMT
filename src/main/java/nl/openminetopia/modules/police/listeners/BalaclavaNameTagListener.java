package nl.openminetopia.modules.police.listeners;

import nl.openminetopia.modules.police.utils.BalaclavaUtils;
import nl.openminetopia.modules.police.utils.TabNametagManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class BalaclavaNameTagListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // TAB handles nametag state automatically, but ensure state is correct
        // if player is wearing balaclava when they join
        if (BalaclavaUtils.isWearingBalaclava(event.getPlayer())) {
            TabNametagManager.getInstance().hideNameTag(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // TAB handles cleanup automatically, no manual intervention needed
        TabNametagManager.getInstance().onPlayerQuit(event.getPlayer());
    }
}
