package nl.openminetopia.modules.lock.listeners;

import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.modules.lock.utils.LockUtil;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class LockInteractListener implements Listener {

    @EventHandler
    public void lockInteract(final PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (!LockUtil.isLocked(event.getClickedBlock())) return;
        Player player = event.getPlayer();
        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getOnlineMinetopiaPlayer(player);
        if (!LockUtil.canOpen(event.getClickedBlock(), player)) {
            event.setCancelled(true);
            ChatUtils.sendFormattedMessage(minetopiaPlayer,"<red>Dit slot is <dark_red>vergrendeld<red>!");
            return;
        }
        UUID ownerUuid = LockUtil.getLockOwner(event.getClickedBlock());
        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUuid);
        String ownerName = owner.getName() == null  ? "onbekend" : owner.getName();
        ChatUtils.sendFormattedMessage(minetopiaPlayer, "<gold>Je opent een slot van <yellow>" + ownerName + "<gold>.");
    }
}
