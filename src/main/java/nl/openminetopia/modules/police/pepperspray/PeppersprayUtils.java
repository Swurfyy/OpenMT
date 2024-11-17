package nl.openminetopia.modules.police.pepperspray;

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
public class PeppersprayUtils {

    public void applyPeppersprayEffects(Player target) {
        DefaultConfiguration configuration = OpenMinetopia.getDefaultConfiguration();

        ItemUtils.applyEffects(target, configuration.getPeppersprayEffects(), configuration.getPeppersprayEffectsDuration() * 20);
    }

    public boolean isPeppersprayItem(ItemStack item) {
        return ItemUtils.isValidItem(item, OpenMinetopia.getDefaultConfiguration().getPeppersprayItems());
    }
}
