package nl.openminetopia.modules.belasting.gui;

import nl.openminetopia.OpenMinetopia;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class ItemsAdderHelper {

    private ItemsAdderHelper() {
    }

    public static ItemStack getItem(String fullId, Material fallback) {
        if (fullId == null || !fullId.contains(":")) return fallbackItem(fallback);
        String[] parts = fullId.split(":", 2);
        ItemStack stack = tryGetItemsAdderItem(parts[0], parts[1]);
        return stack != null && !stack.getType().isAir() ? stack : fallbackItem(fallback);
    }

    private static ItemStack fallbackItem(Material material) {
        return material != null ? new ItemStack(material) : new ItemStack(Material.BARRIER);
    }

    private static ItemStack tryGetItemsAdderItem(String namespace, String itemId) {
        String fullItemId = namespace + ":" + itemId;
        try {
            Plugin plugin = OpenMinetopia.getInstance().getServer().getPluginManager().getPlugin("ItemsAdder");
            if (plugin == null || !plugin.isEnabled()) return null;
            try {
                Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
                Object instance = customStackClass.getMethod("getInstance").invoke(null);
                Object customStack = customStackClass.getMethod("getItemByName", String.class).invoke(instance, fullItemId);
                if (customStack != null) {
                    ItemStack item = (ItemStack) customStackClass.getMethod("getItemStack").invoke(customStack);
                    if (item != null && !item.getType().isAir()) return item;
                }
            } catch (Exception ignored) {
            }
            try {
                Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
                Object customStack = customStackClass.getMethod("getItemByName", String.class).invoke(null, fullItemId);
                if (customStack != null) {
                    ItemStack item = (ItemStack) customStackClass.getMethod("getItemStack").invoke(customStack);
                    if (item != null && !item.getType().isAir()) return item;
                }
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
