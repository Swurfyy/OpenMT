package nl.openminetopia.modules.police.nightvision.listeners;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import nl.openminetopia.modules.police.nightvision.utils.NightvisionUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PlayerEquipNightvisionListener implements Listener {

    @EventHandler
    public void armorChange(PlayerArmorChangeEvent event) {
        if (event.getSlotType() != PlayerArmorChangeEvent.SlotType.HEAD) return;

        if (NightvisionUtils.isNightvisonItem(event.getNewItem()) && !NightvisionUtils.isNightvisonItem(event.getOldItem())) {
            NightvisionUtils.applyNightvisonEffects(event.getPlayer());
            return;
        }

        if (NightvisionUtils.isNightvisonItem(event.getOldItem()) && !NightvisionUtils.isNightvisonItem(event.getNewItem())) {
            NightvisionUtils.clearNightvisionEffects(event.getPlayer());
        }
    }
}
