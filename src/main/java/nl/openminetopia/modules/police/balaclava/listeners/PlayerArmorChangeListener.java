package nl.openminetopia.modules.police.balaclava.listeners;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import nl.openminetopia.modules.police.balaclava.utils.BalaclavaUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PlayerArmorChangeListener implements Listener {

    @EventHandler
    public void armorChange(PlayerArmorChangeEvent event) {
        if (event.getSlotType() != PlayerArmorChangeEvent.SlotType.HEAD) return;

        if (BalaclavaUtils.isBalaclavaItem(event.getNewItem()) && !BalaclavaUtils.isBalaclavaItem(event.getOldItem())) {
            BalaclavaUtils.obfuscate(event.getPlayer(), true);
            return;
        }

        if (BalaclavaUtils.isBalaclavaItem(event.getOldItem()) && !BalaclavaUtils.isBalaclavaItem(event.getNewItem())) {
            BalaclavaUtils.obfuscate(event.getPlayer(), false);
        }
    }
}
