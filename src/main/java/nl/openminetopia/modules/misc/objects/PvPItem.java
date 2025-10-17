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

        if (item.hasItemMeta() != itemToCheck.hasItemMeta()) return false;

        if (!item.hasItemMeta()) return true; // Both have no meta, so they match at this point

        var meta = item.getItemMeta();
        var checkMeta = itemToCheck.getItemMeta();
        if (meta == null || checkMeta == null) return false;

        // Only check custom model data if both items actually have it
        if (meta.hasCustomModelData() != checkMeta.hasCustomModelData()) return false;
        if (meta.hasCustomModelData() && meta.getCustomModelData() != checkMeta.getCustomModelData()) return false;

        // Only check item model if both items actually have it
        if (VersionUtil.isCompatible("1.21.4")) {
            if (meta.hasItemModel() != checkMeta.hasItemModel()) return false;
            if (meta.hasItemModel() && checkMeta.hasItemModel()) {
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
