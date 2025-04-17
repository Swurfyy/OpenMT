package nl.openminetopia.utils.item;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

@UtilityClass
public class ItemUtils {

    public void applyEffects(Player target, List<PotionEffect> effects, int duration) {
        for (PotionEffect effect : effects) {
            PotionEffect potionEffect = new PotionEffect(effect.getType(), duration, effect.getAmplifier());
            target.addPotionEffect(potionEffect);
        }
    }

    public void clearEffects(Player target, List<PotionEffect> effects) {
        for (PotionEffect effect : effects) {
            PotionEffectType potionEffectType = effect.getType();
            target.getActivePotionEffects().forEach(activeEffect -> {
                if (activeEffect.getType().equals(potionEffectType)) {
                    target.removePotionEffect(potionEffectType);
                }
            });
        }
    }

    public boolean isSimilarToAny(ItemStack itemToCheck, List<ItemStack> validItems) {
        if (itemToCheck == null || validItems == null || validItems.isEmpty()) return false;

        for (ItemStack item : validItems) {
            if (item == null) continue;
            if (item.getType() != itemToCheck.getType()) continue;

            if (item.hasItemMeta() != itemToCheck.hasItemMeta()) continue;

            if (!item.hasItemMeta()) return true; // Both have no meta, match

            var meta = item.getItemMeta();
            var checkMeta = itemToCheck.getItemMeta();
            if (meta == null || checkMeta == null) continue;

            if (meta.hasCustomModelData() != checkMeta.hasCustomModelData()) continue;
            if (meta.hasCustomModelData() && checkMeta.getCustomModelData() != meta.getCustomModelData()) continue;

            if (meta instanceof org.bukkit.inventory.meta.Damageable damageMeta
                    && checkMeta instanceof org.bukkit.inventory.meta.Damageable checkDamageMeta) {
                if (damageMeta.getDamage() != checkDamageMeta.getDamage()) continue;
            }

            if (Bukkit.getVersion().contains("1.21.4") && meta.hasItemModel() && checkMeta.hasItemModel()) {
                if (meta.getItemModel() != checkMeta.getItemModel()) continue;
            }

            return true; // Found a match
        }

        return false;
    }
}
