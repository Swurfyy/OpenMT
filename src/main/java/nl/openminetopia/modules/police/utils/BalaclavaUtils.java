package nl.openminetopia.modules.police.utils;

import lombok.experimental.UtilityClass;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.utils.item.ItemUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@UtilityClass
public class BalaclavaUtils {

    public void hideNameTag(Player player, boolean hide) {
        if (hide) {
            BalaclavaNameTagManager.getInstance().hideNameTag(player);
        } else {
            BalaclavaNameTagManager.getInstance().showNameTag(player);
        }
    }

    public boolean isBalaclavaItem(ItemStack head) {
        return ItemUtils.isSimilarToAny(head, OpenMinetopia.getDefaultConfiguration().getBalaclavaItems());
    }

    public boolean isWearingBalaclava(Player player) {
        return isBalaclavaItem(player.getInventory().getHelmet());
    }
}
