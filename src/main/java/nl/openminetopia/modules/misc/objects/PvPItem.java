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
    private final SlownessConfig slownessConfig;

    public String attackerMessage() {
        if (attackerMessage == null || attackerMessage.isEmpty()) {
            return "";
        }
        return attackerMessage.replace("<item>", item.getType().name());
    }

    public String victimMessage() {
        if (victimMessage == null || victimMessage.isEmpty()) {
            return "";
        }
        return victimMessage.replace("<item>", item.getType().name());
    }

    public boolean isSimilar(ItemStack itemToCheck) {
        if (item == null || itemToCheck == null) return false;
        ItemStack item = this.item.clone();
        itemToCheck = itemToCheck.clone();
        if (item.getType() != itemToCheck.getType()) return false;

        // Remove damage from both items for comparison
        if (item.hasItemMeta()) {
            Damageable damageable = (Damageable) item.getItemMeta();
            if (damageable.hasDamage()) {
                damageable.setDamage(0);
                item.setItemMeta(damageable);
            }
        }
        if (itemToCheck.hasItemMeta()) {
            Damageable checkDamageable = (Damageable) itemToCheck.getItemMeta();
            if (checkDamageable.hasDamage()) {
                checkDamageable.setDamage(0);
                itemToCheck.setItemMeta(checkDamageable);
            }
        }

        var meta = item.hasItemMeta() ? item.getItemMeta() : null;
        var checkMeta = itemToCheck.hasItemMeta() ? itemToCheck.getItemMeta() : null;

        // Check custom model data - only if the configured item explicitly sets it
        if (meta != null && meta.hasCustomModelData()) {
            if (checkMeta == null || !checkMeta.hasCustomModelData()) return false;
            if (meta.getCustomModelData() != checkMeta.getCustomModelData()) return false;
        } else if (checkMeta != null && checkMeta.hasCustomModelData()) {
            // If the config doesn't set custom model data but the item has it, check if config uses -1 (wildcard)
            // This allows renamed items to match config items with custom-model-data: -1
            if (meta != null && meta.hasCustomModelData()) return false;
        }

        // Check item model - only if the configured item explicitly sets it
        if (VersionUtil.isCompatible("1.21.4")) {
            if (meta != null && meta.hasItemModel()) {
                if (checkMeta == null || !checkMeta.hasItemModel()) return false;
                NamespacedKey modelA = meta.getItemModel();
                NamespacedKey modelB = checkMeta.getItemModel();
                
                if (modelA != null || modelB != null) {
                    if (modelA == null || modelB == null) return false;
                    if (!modelA.equals(modelB)) return false;
                }
            }
        }

        return true;
    }
}
