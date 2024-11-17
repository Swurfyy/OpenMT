package nl.openminetopia.utils.item;

import lombok.experimental.UtilityClass;
import nl.openminetopia.OpenMinetopia;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

@UtilityClass
public class ItemUtils {

    public void applyEffects(Player target, List<String> effects, int duration) {
        for (String effectString : effects) {
            String[] effect = effectString.split(":");
            String effectName = effect[0].toLowerCase();

            PotionEffectType potionEffectType = Registry.EFFECT.get(NamespacedKey.minecraft(effectName));
            if (potionEffectType == null) {
                OpenMinetopia.getInstance().getLogger().warning("Invalid potion effect: " + effectName);
                continue;
            }

            if (effect.length == 1) {
                PotionEffect potionEffect = new PotionEffect(potionEffectType, duration, 0);
                target.addPotionEffect(potionEffect);
                continue;
            }

            int amplifier = Integer.parseInt(effect[1]);
            PotionEffect potionEffect = new PotionEffect(potionEffectType, duration, amplifier);
            target.addPotionEffect(potionEffect);
        }
    }

    public void clearEffects(Player target, List<String> effects) {
        for (String effectString : effects) {
            String[] effect = effectString.split(":");
            String effectName = effect[0].toLowerCase();

            PotionEffectType potionEffectType = Registry.EFFECT.get(NamespacedKey.minecraft(effectName));
            if (potionEffectType == null) {
                OpenMinetopia.getInstance().getLogger().warning("Invalid potion effect: " + effectName);
                continue;
            }
            target.getActivePotionEffects().forEach(activeEffect -> {
                if (activeEffect.getType().equals(potionEffectType)) {
                    target.removePotionEffect(potionEffectType);
                }
            });
        }
    }

    public boolean isValidItem(ItemStack item, List<String> validItems) {
        if (item == null) return false;

        for (String headItemString : validItems) {
            String[] headItem = headItemString.split(":");
            if (item.getType().name().equalsIgnoreCase(headItem[0])) {
                if (headItem.length == 1) {
                    return true;
                }

                if (item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == Integer.parseInt(headItem[1])) {
                    return true;
                }
            }
        }
        return false;
    }
}
