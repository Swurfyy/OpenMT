package nl.openminetopia.modules.belasting.gui;

import nl.openminetopia.OpenMinetopia;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Resolves ItemsAdder custom items for the belasting GUI.
 * Uses fivemopia model: assets/fivemopia/models/item/invisibleitem.json (texture: fivemopia:item/invisible).
 * Tries fivemopia:invisitem, fivemopia:invisibleitem and fivemopia:invisible so config works regardless of ItemsAdder item id.
 */
public final class ItemsAdderHelper {

    /** Item IDs that represent the invisible item (model: invisibleitem.json, texture: invisible.png). */
    private static final String[] INVISIBLE_ITEM_IDS = new String[]{"fivemopia:invisitem", "fivemopia:invisibleitem", "fivemopia:invisible"};

    private ItemsAdderHelper() {
    }

    public static ItemStack getItem(String fullId, Material fallback) {
        if (fullId == null || !fullId.contains(":")) {
            return fallbackItem(fallback);
        }
        String normalized = fullId.trim().toLowerCase();
        if ("fivemopia:invisitem".equals(normalized) || "fivemopia:invisibleitem".equals(normalized) || "fivemopia:invisible".equals(normalized)) {
            return getInvisibleItem(fallback);
        }
        String[] parts = fullId.split(":", 2);
        ItemStack stack = tryGetItemsAdderItem(parts[0], parts[1]);
        if (stack != null && !stack.getType().isAir()) {
            return stack;
        }
        return fallbackItem(fallback);
    }

    /**
     * Resolves the invisible item by trying known IDs (invisibleitem, invisible). Uses first that exists.
     */
    private static ItemStack getInvisibleItem(Material fallback) {
        for (String id : INVISIBLE_ITEM_IDS) {
            String[] parts = id.split(":", 2);
            ItemStack stack = tryGetItemsAdderItem(parts[0], parts[1]);
            if (stack != null && !stack.getType().isAir()) {
                return stack;
            }
        }
        return fallbackItem(fallback);
    }

    private static ItemStack fallbackItem(Material material) {
        return material != null ? new ItemStack(material) : new ItemStack(Material.BARRIER);
    }

    private static ItemStack tryGetItemsAdderItem(String namespace, String itemId) {
        String fullItemId = namespace + ":" + itemId;
        try {
            Plugin plugin = OpenMinetopia.getInstance().getServer().getPluginManager().getPlugin("ItemsAdder");
            if (plugin == null || !plugin.isEnabled()) {
                return null;
            }
            ClassLoader iaLoader = plugin.getClass().getClassLoader();
            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack", true, iaLoader);
            Object customStack = customStackClass.getMethod("getInstance", String.class).invoke(null, fullItemId);
            if (customStack == null) return null;
            ItemStack item = (ItemStack) customStackClass.getMethod("getItemStack").invoke(customStack);
            if (item != null && !item.getType().isAir()) return item;
        } catch (Exception e) {
            // ItemsAdder item not available or reflection failed
        }
        return null;
    }
}
