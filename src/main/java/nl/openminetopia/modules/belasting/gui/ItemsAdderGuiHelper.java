package nl.openminetopia.modules.belasting.gui;

import nl.openminetopia.OpenMinetopia;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Applies an ItemsAdder texture overlay to the player's currently open inventory.
 * Use after opening a Bukkit/TriumphGUI inventory: open first, then next tick call this.
 * Uses TexturedInventoryWrapper.setPlayerInventoryTexture(Player, FontImageWrapper, String, int, int).
 */
public final class ItemsAdderGuiHelper {

    private static final String LOG_PREFIX = "[Belasting ItemsAdder GUI] ";

    private ItemsAdderGuiHelper() {
    }

    private static Logger log() {
        return OpenMinetopia.getInstance().getLogger();
    }

    /**
     * Checks if ItemsAdder is loaded and enabled.
     */
    public static boolean isItemsAdderAvailable() {
        Plugin plugin = OpenMinetopia.getInstance().getServer().getPluginManager().getPlugin("ItemsAdder");
        return plugin != null && plugin.isEnabled();
    }

    /**
     * Applies the ItemsAdder texture overlay to the player's current inventory at the given offsets.
     * Call this one tick after opening the inventory (e.g. after gui.open(player)).
     * Use titleOffset=16 and textureOffset=-16 for default generic_54 (6-row chest) base position.
     *
     * @param player         the player whose open inventory should get the texture
     * @param textureKey     ItemsAdder font_image id (e.g. "minecraft:generic_54_tax")
     * @param titleOffset    title offset (default 16 for generic_54)
     * @param textureOffset  texture/inventory offset (default -16 for generic_54)
     * @return true if the texture was applied, false if ItemsAdder unavailable or font image missing
     */
    public static boolean applyTextureToCurrentInventory(Player player, String textureKey, int titleOffset, int textureOffset) {
        Logger log = log();
        Plugin plugin = OpenMinetopia.getInstance().getServer().getPluginManager().getPlugin("ItemsAdder");
        if (plugin == null || !plugin.isEnabled()) {
            return false;
        }

        String namespacedKey = textureKey.contains(":") ? textureKey : "minecraft:" + textureKey;

        try {
            ClassLoader iaLoader = plugin.getClass().getClassLoader();

            // FontImageWrapper(String namespacedGuiName)
            Class<?> fontImageClass = Class.forName("dev.lone.itemsadder.api.FontImages.FontImageWrapper", true, iaLoader);
            Object fontImage;
            try {
                fontImage = fontImageClass.getConstructor(String.class).newInstance(namespacedKey);
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                log.warning(LOG_PREFIX + "FontImage '" + namespacedKey + "' does not exist in ItemsAdder. Define it as font_image (type: gui) in your pack. " + cause.getMessage());
                return false;
            }

            // TexturedInventoryWrapper.setPlayerInventoryTexture(Player, FontImageWrapper, String title, int titleOffset, int textureOffset)
            Class<?> wrapperClass = Class.forName("dev.lone.itemsadder.api.FontImages.TexturedInventoryWrapper", true, iaLoader);
            java.lang.reflect.Method setTextureMethod = wrapperClass.getMethod(
                    "setPlayerInventoryTexture",
                    Player.class,
                    fontImageClass,
                    String.class,
                    int.class,
                    int.class
            );

            setTextureMethod.invoke(null, player, fontImage, "", titleOffset, textureOffset);
            return true;
        } catch (Exception e) {
            log.warning(LOG_PREFIX + "Failed to apply texture: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            if (e.getCause() != null) {
                log.warning(LOG_PREFIX + "  Cause: " + e.getCause().getMessage());
            }
            log.log(Level.WARNING, LOG_PREFIX + "Stack trace", e);
            return false;
        }
    }
}
