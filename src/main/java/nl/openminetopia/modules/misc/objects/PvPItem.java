package nl.openminetopia.modules.misc.objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;
import nl.openminetopia.utils.VersionUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

@AllArgsConstructor
@Data
@Accessors(fluent = true)
public class PvPItem {
    private final ItemStack item;
    private final String attackerMessage;
    private final String victimMessage;
    private final boolean slowness;

    public String attackerMessage() {
        return attackerMessage.replace("<item>", item.getType().name());
    }

    public String victimMessage() {
        return victimMessage.replace("<item>", item.getType().name());
    }

    public boolean isSimilar(ItemStack itemToCheck) {
        if (item == null || itemToCheck == null) return false;
        ItemStack item = this.item.clone();
        itemToCheck = itemToCheck.clone();
        if (item.getType() != itemToCheck.getType()) return false;

        Damageable damageable = (Damageable) item.getItemMeta();
        if (damageable.hasDamage()) {
            damageable.setDamage(0);
            item.setItemMeta(damageable);
        }
        Damageable checkDamageable = (Damageable) itemToCheck.getItemMeta();
        if (checkDamageable.hasDamage()) {
            checkDamageable.setDamage(0);
            itemToCheck.setItemMeta(checkDamageable);
        }

        // For PvP items, we want to allow items with custom names to match base items
        // So if the configured item has no meta, it should match any item of the same type
        if (!item.hasItemMeta()) return true; // Configured item has no meta, so it matches any item of the same type
        
        // If the configured item has meta but the item to check doesn't, they don't match
        if (!itemToCheck.hasItemMeta()) return false;

        var meta = item.getItemMeta();
        var checkMeta = itemToCheck.getItemMeta();
        if (meta == null || checkMeta == null) return false;

        if (meta.hasCustomModelData() != checkMeta.hasCustomModelData()) return false;
        if (meta.hasCustomModelData() && checkMeta.getCustomModelData() != meta.getCustomModelData()) return false;

        if (VersionUtil.isCompatible("1.21.4") && meta.hasItemModel() && checkMeta.hasItemModel()) {
            NamespacedKey modelA = meta.getItemModel();
            NamespacedKey modelB = checkMeta.getItemModel();

            if (modelA != null || modelB != null) {
                if (modelA == null || modelB == null) return false;
                return modelA.equals(modelB);
            }
        }

        return true;
    }
}
