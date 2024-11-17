package nl.openminetopia.modules.police.listeners;

import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.modules.police.utils.PeppersprayUtils;
import nl.openminetopia.utils.ChatUtils;
import nl.openminetopia.utils.PersistentDataUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class PlayerPeppersprayListener implements Listener {

    @EventHandler
    public void playerInteract(final PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player target)) return;

        Player source = event.getPlayer();

        if (!PeppersprayUtils.isPeppersprayItem(source.getInventory().getItemInMainHand())) return;

        event.setCancelled(true);

        ItemStack itemStack = source.getInventory().getItemInMainHand();

        if (OpenMinetopia.getDefaultConfiguration().isPeppersprayUsagesEnabled()) {
            if (PersistentDataUtil.getInteger(itemStack, "openmt.usages") == null) {
                ItemStack finalItemStack = PersistentDataUtil.set(itemStack,
                        OpenMinetopia.getDefaultConfiguration().getPeppersprayMaxUsages(), "openmt.usages");
                source.getInventory().setItemInMainHand(finalItemStack);
            }

            Integer currentUsages = PersistentDataUtil.getInteger(itemStack, "openmt.usages");
            if (currentUsages == null || currentUsages <= 0) {
                source.sendMessage(ChatUtils.color("<red>Jouw pepperspray is <dark_red>leeg<red>!"));
                return;
            }

            ItemStack finalItemStack = PersistentDataUtil.set(itemStack, currentUsages - 1, "openmt.usages");
            source.getInventory().setItemInMainHand(finalItemStack);
        }

        source.sendMessage(ChatUtils.color("<red>Je hebt <dark_red>" + target.getName() + " <red>gepeppersprayed."));


        PlayerManager.getInstance().getMinetopiaPlayerAsync(source, sourceMinetopiaPlayer -> {
            if (sourceMinetopiaPlayer == null) return;

            target.sendMessage(ChatUtils.format(sourceMinetopiaPlayer, "<red>Je bent gepeppersprayed!"));
            PeppersprayUtils.applyPeppersprayEffects(target);
        }, Throwable::printStackTrace);
    }
}
