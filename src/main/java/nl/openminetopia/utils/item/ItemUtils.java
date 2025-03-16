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

    public boolean isValidItem(ItemStack item, List<ItemStack> validItems) {
        if (item == null) return false;

        for (ItemStack compare : validItems) {
            if (item.getType() == compare.getType()) {
                if (!compare.hasItemMeta() || !item.hasItemMeta()) return true;
                ItemMeta meta = item.getItemMeta();
                ItemMeta compareMeta = compare.getItemMeta();

                boolean isSame = true;
                if (Bukkit.getVersion().contains("1.21.4") && meta.hasItemModel() && compareMeta.hasItemModel()) {
                    if (meta.getItemModel() != compareMeta.getItemModel()) isSame = false;
                }

                if (meta.hasCustomModelData() && compareMeta.hasCustomModelData()) {
                    if (meta.getCustomModelData() != compareMeta.getCustomModelData()) isSame = false;
                }

                Damageable damageable = (Damageable) meta;
                Damageable compareDamageable = (Damageable) compareMeta;
                if (damageable.getDamage() != compareDamageable.getDamage()) isSame = false;

                return isSame;
            }
        }
        return false;
    }
}
