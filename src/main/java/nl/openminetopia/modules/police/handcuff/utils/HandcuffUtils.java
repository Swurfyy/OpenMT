package nl.openminetopia.modules.police.handcuff.utils;

import lombok.experimental.UtilityClass;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.configuration.DefaultConfiguration;
import nl.openminetopia.utils.item.ItemUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@UtilityClass
public class HandcuffUtils {

    public void applyHandcuffEffects(Player target) {
        DefaultConfiguration configuration = OpenMinetopia.getDefaultConfiguration();

        ItemUtils.applyEffects(target, configuration.getHandcuffEffects(), PotionEffect.INFINITE_DURATION);
    }

    public void clearHandcuffEffects(Player target) {
        DefaultConfiguration configuration = OpenMinetopia.getDefaultConfiguration();

        ItemUtils.clearEffects(target, configuration.getHandcuffEffects());
    }

    public boolean isHandcuffItem(ItemStack item) {
        return ItemUtils.isValidItem(item, OpenMinetopia.getDefaultConfiguration().getHandcuffItems());
    }
}
