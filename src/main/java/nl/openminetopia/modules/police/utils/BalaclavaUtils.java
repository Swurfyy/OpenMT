package nl.openminetopia.modules.police.utils;

import lombok.experimental.UtilityClass;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.utils.item.ItemUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@UtilityClass
public class BalaclavaUtils {

    public void hideNameTag(Player player, boolean hide) {
        if (player == null) return;

        if (hide) {
            BalaclavaNameTagManager.getInstance().hideNameTag(player);
        } else {
            BalaclavaNameTagManager.getInstance().showNameTag(player);
        }
    }

    public boolean isBalaclavaItem(ItemStack item) {
        if (item == null) return false;

        return ItemUtils.isSimilarToAny(
                item,
                OpenMinetopia.getDefaultConfiguration().getBalaclavaItems()
        );
    }

    public boolean isWearingBalaclava(Player player) {
        if (player == null) return false;

        return isBalaclavaItem(player.getInventory().getHelmet());
    }
}