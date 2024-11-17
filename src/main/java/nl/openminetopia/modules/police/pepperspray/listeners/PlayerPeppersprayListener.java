package nl.openminetopia.modules.police.pepperspray.listeners;

import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.modules.police.pepperspray.PeppersprayManager;
import nl.openminetopia.modules.police.pepperspray.PeppersprayUtils;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public class PlayerPeppersprayListener implements Listener {

    @EventHandler
    public void playerInteract(final PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player target)) return;

        Player source = event.getPlayer();

        if (!PeppersprayUtils.isPeppersprayItem(source.getInventory().getItemInMainHand())) return;

        event.setCancelled(true);

        source.sendMessage(ChatUtils.color("<red>Je hebt <dark_red>" + target.getName() + " <red>gepeppersprayed."));

        PlayerManager.getInstance().getMinetopiaPlayerAsync(target, targetMinetopiaPlayer -> {
            if (targetMinetopiaPlayer == null) return;

            PlayerManager.getInstance().getMinetopiaPlayerAsync(source,sourceMinetopiaPlayer -> {
                if (sourceMinetopiaPlayer == null) return;

                PeppersprayManager.getInstance().pepperspray(targetMinetopiaPlayer, sourceMinetopiaPlayer);
            }, Throwable::printStackTrace);
        }, Throwable::printStackTrace);

    }
}
