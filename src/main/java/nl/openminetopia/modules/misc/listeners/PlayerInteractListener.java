package nl.openminetopia.modules.misc.listeners;

import com.jazzkuh.inventorylib.objects.Menu;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerInteractListener implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;
        Player player = event.getPlayer();

        if (!OpenMinetopia.getDefaultConfiguration().getTrashcanBlocks().contains(block.getType())) return;
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && player.getGameMode() == GameMode.CREATIVE) return;

        event.setCancelled(true);

        if (!OpenMinetopia.getDefaultConfiguration().isTrashcanUseDropperInventory()) {
            new TrashcanMenu(InventoryType.CHEST).open(event.getPlayer());
        } else {
            new TrashcanMenu(InventoryType.DROPPER).open(event.getPlayer());
        }

        PlayerManager.getInstance().getMinetopiaPlayer(player).whenComplete(((minetopiaPlayer, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                return;
            }
            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("trashcan_message"));
        }));
    }

    private static class TrashcanMenu extends Menu {

        public TrashcanMenu(InventoryType type) {
            super(ChatUtils.color(MessageConfiguration.message("trashcan_title")), 3, type, false);
        }
    }
}
