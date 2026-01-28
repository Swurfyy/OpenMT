package nl.openminetopia.modules.misc.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.misc.objects.BootEffectType;
import nl.openminetopia.utils.ChatUtils;
import nl.openminetopia.utils.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

@CommandAlias("boots")
@CommandPermission("openminetopia.boots")
@Description("Create special boots with different effects.")
public class BootsCommand extends BaseCommand {

    @Default
    @Syntax("<material|hand> <effect_type> <level> [modelpath]")
    @CommandCompletion("leather|chainmail|iron|golden|diamond|hand speed|ice|blub 1|2|3|4|5|6|7|8|9 contents/my_armor/textures/armor/politie/layer_1")
    public void onBootsWithModel(Player player, String materialArg, String effectTypeArg, int level, @Optional String modelPath) {
        // Parse effect type
        BootEffectType effectType = BootEffectType.fromString(effectTypeArg);
        if (effectType == null) {
            player.sendMessage(ChatUtils.color("<red>Invalid effect type! Use: speed, ice, or blub"));
            return;
        }

        // Validate level based on boot type
        int maxLevel = getMaxLevel(effectType);
        if (level < 1 || level > maxLevel) {
            player.sendMessage(ChatUtils.color("<red>Level must be between 1 and " + maxLevel + " for " + effectType.name().toLowerCase() + " boots!"));
            return;
        }

        ItemStack boots;

        // Handle "hand" mode
        if (materialArg.equalsIgnoreCase("hand")) {
            ItemStack inHand = player.getInventory().getItemInMainHand();
            if (inHand.getType() == Material.AIR) {
                player.sendMessage(ChatUtils.color("<red>You must hold an item in your hand!"));
                return;
            }
            boots = inHand.clone();
            player.getInventory().remove(inHand);
        } else {
            // Parse boot material
            Material bootMaterial = parseBootMaterial(materialArg);
            if (bootMaterial == null) {
                player.sendMessage(ChatUtils.color("<red>Invalid boot material! Use: leather, chainmail, iron, golden, diamond, or hand"));
                return;
            }
            boots = new ItemStack(bootMaterial);
        }

        // Apply effect to boots
        boots = applyBootEffect(boots, effectType, level, modelPath);

        // Give boots to player
        player.getInventory().addItem(boots);
        player.sendMessage(ChatUtils.color("<green>Created <aqua>" + effectType.getDisplayName() + " <green>boots with level <aqua>" + level + "<green>!"));
    }

    /**
     * Parse boot material from string input
     */
    private Material parseBootMaterial(String input) {
        return switch (input.toLowerCase()) {
            case "leather" -> Material.LEATHER_BOOTS;
            case "chainmail" -> Material.CHAINMAIL_BOOTS;
            case "iron" -> Material.IRON_BOOTS;
            case "golden", "gold" -> Material.GOLDEN_BOOTS;
            case "diamond" -> Material.DIAMOND_BOOTS;
            case "netherite" -> Material.NETHERITE_BOOTS;
            default -> null;
        };
    }

    /**
     * Apply the specified effect to the boots with optional model path
     * @return The updated ItemStack
     */
    private ItemStack applyBootEffect(ItemStack boots, BootEffectType effectType, int level, String modelPath) {
        // Store original display name before applying ItemsAdder model
        String originalDisplayName = null;
        ItemMeta meta = boots.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            originalDisplayName = meta.displayName() != null ? 
                meta.displayName().toString() : null;
        }
        
        // Apply ItemsAdder model first (if provided) to get correct base item
        boolean hasModelPath = modelPath != null && !modelPath.isEmpty();
        if (hasModelPath) {
            boots = applyItemsAdderModel(boots, modelPath);
            // After applying ItemsAdder model, get the meta again
            meta = boots.getItemMeta();
            if (meta == null) {
                return boots;
            }
            
            // If ItemsAdder set a display name, we'll override it with our custom name
            // But if no modelpath was provided, preserve the original name
        }
        
        // Now apply effects to the boots
        if (meta == null) {
            meta = boots.getItemMeta();
            if (meta == null) {
                return boots;
            }
        }

        switch (effectType) {
            case SPEED -> addSpeedModifier(meta, level);
            case ICE -> {
                // Cap Frost Walker at level 2 (max vanilla level)
                int iceLevel = Math.min(level, 2);
                meta.addEnchant(Enchantment.FROST_WALKER, iceLevel, true);
            }
            case BLUB -> {
                // Cap Depth Strider at level 3 (max vanilla level)
                int blubLevel = Math.min(level, 3);
                meta.addEnchant(Enchantment.DEPTH_STRIDER, blubLevel, true);
            }
        }

        // Set display name based on whether modelpath was provided
        if (hasModelPath) {
            // When modelpath is provided, always set our custom display name (overrides ItemsAdder name)
            String customDisplayName;
            if (effectType == BootEffectType.SPEED) {
                String speedValue = String.format("0.0%d", level);
                customDisplayName = "<yellow>Sportschoenenn " + speedValue;
            } else {
                customDisplayName = "<yellow>" + effectType.getDisplayName();
            }
            meta.displayName(ChatUtils.color(customDisplayName));
        } else {
            // When no modelpath is provided, preserve the original display name if it existed
            // Only set a new name if the item didn't have one
            if (originalDisplayName == null || originalDisplayName.isEmpty()) {
                String customDisplayName;
                if (effectType == BootEffectType.SPEED) {
                    String speedValue = String.format("0.0%d", level);
                    customDisplayName = "<yellow>Sportschoenenn " + speedValue;
                } else {
                    customDisplayName = "<yellow>" + effectType.getDisplayName();
                }
                meta.displayName(ChatUtils.color(customDisplayName));
            }
            // If originalDisplayName exists, don't change it - preserve the original name
        }

        boots.setItemMeta(meta);
        return boots;
    }

    /**
     * Apply ItemsAdder model to boots
     * Supports multiple formats:
     * 1. Full path: "contents/my_armor/textures/armor/politie/layer_1" - uses exact path
     * 2. Namespace:key: "my_armor:agent" - automatically resolves to correct boots item
     * 3. Namespace:key with _boots: "my_armor:agent_boots" - direct boots item reference
     * 
     * Preserves ALL ItemsAdder properties and only adds speed effects
     */
    private ItemStack applyItemsAdderModel(ItemStack boots, String modelPath) {
        try {
            String namespace;
            String armorName;
            String bootsItemKey;
            
            // Parse model path to extract namespace and item key
            if (modelPath.contains(":") && !modelPath.contains("/")) {
                // Format: namespace:key (e.g., "my_armor:agent" or "my_armor:agent_boots")
                String[] parts = modelPath.split(":", 2);
                namespace = parts[0];
                String itemKey = parts[1];
                
                // Check if it already ends with _boots
                if (itemKey.endsWith("_boots")) {
                    bootsItemKey = itemKey;
                    armorName = itemKey.substring(0, itemKey.length() - 6); // Remove "_boots"
                } else {
                    // Extract armor name and create boots item key
                    armorName = itemKey;
                    bootsItemKey = armorName + "_boots";
                }
            } else {
                // Full path format: "contents/my_armor/textures/armor/politie/layer_1"
                String[] pathParts = modelPath.split("/");
                
                // Extract namespace (first part)
                namespace = pathParts.length > 0 ? pathParts[0] : "itemsadder";
                
                // Extract armor name from path (part before "layer_X")
                armorName = null;
                for (int i = 0; i < pathParts.length - 1; i++) {
                    if (pathParts[i].equalsIgnoreCase("armor") && i + 1 < pathParts.length) {
                        armorName = pathParts[i + 1];
                        break;
                    }
                }
                
                if (armorName == null && pathParts.length >= 2) {
                    String lastPart = pathParts[pathParts.length - 1];
                    if (lastPart.startsWith("layer_")) {
                        armorName = pathParts[pathParts.length - 2];
                    }
                }
                
                if (armorName == null) {
                    OpenMinetopia.getInstance().getLogger().warning("Could not extract armor name from path: " + modelPath);
                    return boots;
                }
                
                bootsItemKey = armorName + "_boots";
            }
            
            // Try to use ItemsAdder API first - this preserves ALL properties
            // First try with bootsItemKey (e.g., "agent_boots")
            OpenMinetopia.getInstance().getLogger().fine("Attempting to load ItemsAdder item: " + namespace + ":" + bootsItemKey);
            ItemStack itemsAdderItem = tryGetItemsAdderItem(namespace, bootsItemKey);
            
            // If boots item not found, try with just the armor name (e.g., "agent")
            // This handles cases where ItemsAdder has the armor set but not individual pieces
            if ((itemsAdderItem == null || itemsAdderItem.getType().isAir()) && 
                modelPath.contains(":") && !modelPath.contains("/") && 
                !modelPath.split(":", 2)[1].endsWith("_boots")) {
                // Try with just the armor name
                OpenMinetopia.getInstance().getLogger().fine("Boots item not found, trying armor set: " + namespace + ":" + armorName);
                itemsAdderItem = tryGetItemsAdderItem(namespace, armorName);
                
                // If found, we'll use it but need to ensure it's configured as boots
                if (itemsAdderItem != null && !itemsAdderItem.getType().isAir()) {
                    OpenMinetopia.getInstance().getLogger().fine("Found ItemsAdder armor set '" + namespace + ":" + armorName + "', configuring as boots");
                }
            }
            
            if (itemsAdderItem != null && !itemsAdderItem.getType().isAir()) {
                OpenMinetopia.getInstance().getLogger().fine("Successfully loaded ItemsAdder item: " + namespace + ":" + (itemsAdderItem.getItemMeta() != null ? "loaded" : "no meta"));
            } else {
                OpenMinetopia.getInstance().getLogger().fine("ItemsAdder item not found, using manual setup");
            }
            
            if (itemsAdderItem != null && !itemsAdderItem.getType().isAir()) {
                // Use ItemsAdder item as base - it already has ALL correct properties (icon, layer, custom data, etc.)
                // Clone it to preserve everything
                boots = itemsAdderItem.clone();
                
                // Get the meta to preserve all ItemsAdder properties
                ItemMeta itemsAdderMeta = boots.getItemMeta();
                if (itemsAdderMeta == null) {
                    return boots;
                }
                
                // Log original properties for debugging
                NamespacedKey originalItemModel = itemsAdderMeta.getItemModel();
                var originalEquippable = itemsAdderMeta.getEquippable();
                OpenMinetopia.getInstance().getLogger().fine("ItemsAdder item loaded - item_model: " + originalItemModel + 
                    ", equippable: " + (originalEquippable != null ? originalEquippable.getSlot() + "/" + originalEquippable.getModel() : "null"));
                
                // ONLY modify the equippable component to ensure correct slot and asset_id for armor layer
                // DO NOT modify item_model - ItemsAdder already set it correctly!
                var equippable = itemsAdderMeta.getEquippable();
                if (equippable != null) {
                    // Ensure slot is FEET (might already be correct, but ensure it)
                    equippable.setSlot(EquipmentSlot.FEET);
                    // Ensure asset_id is set for armor texture (layer) - use armorName, not bootsItemKey
                    NamespacedKey assetId = new NamespacedKey(namespace, armorName);
                    equippable.setModel(assetId);
                    itemsAdderMeta.setEquippable(equippable);
                } else {
                    // Create equippable component if it doesn't exist (shouldn't happen for armor items)
                    ItemBuilder builder = new ItemBuilder(boots);
                    builder.setEquippableSlot(EquipmentSlot.FEET);
                    NamespacedKey assetId = new NamespacedKey(namespace, armorName);
                    builder.setEquippableModel(assetId);
                    boots = builder.toItemStack();
                    itemsAdderMeta = boots.getItemMeta();
                }
                
                // CRITICAL: Do NOT modify item_model - ItemsAdder already set it correctly!
                // The item_model from ItemsAdder is the correct one and should be preserved as-is
                
                if (itemsAdderMeta != null) {
                    boots.setItemMeta(itemsAdderMeta);
                }
                
                // ItemsAdder item already has correct icon, layer, and all properties
                // Display name will be set by applyBootEffect after this
                OpenMinetopia.getInstance().getLogger().fine("ItemsAdder item configured - preserving all original properties including item_model");
                return boots;
            }
            
            // Fallback: Manual setup if ItemsAdder API not available or item not found
            // This should rarely happen if ItemsAdder is properly configured
            OpenMinetopia.getInstance().getLogger().warning("ItemsAdder API not available or item '" + namespace + ":" + bootsItemKey + "' not found. Using manual setup.");
            
            ItemMeta meta = boots.getItemMeta();
            if (meta == null) return boots;
            
            // Try to set item_model - ItemsAdder typically uses namespace:item_id format
            // But we'll try multiple formats to maximize compatibility
            NamespacedKey iconModel = new NamespacedKey(namespace, bootsItemKey);
            meta.setItemModel(iconModel);
            OpenMinetopia.getInstance().getLogger().fine("Setting item_model (fallback) to: " + iconModel);
            
            // Set equippable component for armor texture (layer)
            var equippable = meta.getEquippable();
            if (equippable == null) {
                ItemBuilder builder = new ItemBuilder(boots);
                builder.setEquippableSlot(EquipmentSlot.FEET);
                boots = builder.toItemStack();
                meta = boots.getItemMeta();
                if (meta == null) return boots;
                equippable = meta.getEquippable();
            }
            
            if (equippable != null) {
                equippable.setSlot(EquipmentSlot.FEET);
                NamespacedKey assetId = new NamespacedKey(namespace, armorName);
                equippable.setModel(assetId);
                meta.setEquippable(equippable);
            }
            
            // Set ItemsAdder custom data
            setItemsAdderCustomData(meta, namespace, bootsItemKey);
            
            boots.setItemMeta(meta);
            return boots;
            
        } catch (Exception e) {
            OpenMinetopia.getInstance().getLogger().warning("Failed to apply ItemsAdder model '" + modelPath + "': " + e.getMessage());
            e.printStackTrace();
            return boots;
        }
    }
    
    /**
     * Try to get ItemsAdder item using API (soft dependency)
     * Tries multiple methods to ensure we get the exact ItemsAdder item with all properties
     */
    private ItemStack tryGetItemsAdderItem(String namespace, String itemId) {
        String fullItemId = namespace + ":" + itemId;
        
        try {
            Plugin itemsAdderPlugin = OpenMinetopia.getInstance().getServer().getPluginManager().getPlugin("ItemsAdder");
            if (itemsAdderPlugin == null || !itemsAdderPlugin.isEnabled()) {
                OpenMinetopia.getInstance().getLogger().fine("ItemsAdder plugin not found or not enabled");
                return null;
            }
            
            // Method 1: Try CustomStack.getInstance().getItemByName() - most common
            try {
                Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
                java.lang.reflect.Method getInstanceMethod = customStackClass.getMethod("getInstance");
                Object customStackInstance = getInstanceMethod.invoke(null);
                
                java.lang.reflect.Method getItemMethod = customStackClass.getMethod("getItemByName", String.class);
                OpenMinetopia.getInstance().getLogger().fine("Looking up ItemsAdder item (method 1): " + fullItemId);
                Object customStack = getItemMethod.invoke(customStackInstance, fullItemId);
                
                if (customStack != null) {
                    java.lang.reflect.Method getItemStackMethod = customStackClass.getMethod("getItemStack");
                    ItemStack item = (ItemStack) getItemStackMethod.invoke(customStack);
                    if (item != null && !item.getType().isAir()) {
                        OpenMinetopia.getInstance().getLogger().fine("Successfully retrieved ItemsAdder item (method 1): " + fullItemId);
                        return item;
                    }
                }
            } catch (Exception e) {
                OpenMinetopia.getInstance().getLogger().fine("Method 1 failed for " + fullItemId + ": " + e.getMessage());
            }
            
            // Method 2: Try static CustomStack.getItemByName() - alternative API
            try {
                Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
                java.lang.reflect.Method getItemMethod = customStackClass.getMethod("getItemByName", String.class);
                OpenMinetopia.getInstance().getLogger().fine("Looking up ItemsAdder item (method 2): " + fullItemId);
                Object customStack = getItemMethod.invoke(null, fullItemId);
                
                if (customStack != null) {
                    java.lang.reflect.Method getItemStackMethod = customStackClass.getMethod("getItemStack");
                    ItemStack item = (ItemStack) getItemStackMethod.invoke(customStack);
                    if (item != null && !item.getType().isAir()) {
                        OpenMinetopia.getInstance().getLogger().fine("Successfully retrieved ItemsAdder item (method 2): " + fullItemId);
                        return item;
                    }
                }
            } catch (Exception e) {
                OpenMinetopia.getInstance().getLogger().fine("Method 2 failed for " + fullItemId + ": " + e.getMessage());
            }
            
            // Method 3: Try ItemsAdder.getInstance().getItem() - older API
            try {
                Class<?> itemsAdderClass = Class.forName("dev.lone.itemsadder.api.ItemsAdder");
                java.lang.reflect.Method getInstanceMethod = itemsAdderClass.getMethod("getInstance");
                Object itemsAdderInstance = getInstanceMethod.invoke(null);
                
                java.lang.reflect.Method getItemMethod = itemsAdderClass.getMethod("getItem", String.class);
                OpenMinetopia.getInstance().getLogger().fine("Looking up ItemsAdder item (method 3): " + fullItemId);
                Object customStack = getItemMethod.invoke(itemsAdderInstance, fullItemId);
                
                if (customStack != null) {
                    Class<?> customStackClass = customStack.getClass();
                    java.lang.reflect.Method getItemStackMethod = customStackClass.getMethod("getItemStack");
                    ItemStack item = (ItemStack) getItemStackMethod.invoke(customStack);
                    if (item != null && !item.getType().isAir()) {
                        OpenMinetopia.getInstance().getLogger().fine("Successfully retrieved ItemsAdder item (method 3): " + fullItemId);
                        return item;
                    }
                }
            } catch (Exception e) {
                OpenMinetopia.getInstance().getLogger().fine("Method 3 failed for " + fullItemId + ": " + e.getMessage());
            }
            
            OpenMinetopia.getInstance().getLogger().fine("ItemsAdder item not found with any method: " + fullItemId);
        } catch (Exception e) {
            // ItemsAdder API not available or failed
            OpenMinetopia.getInstance().getLogger().fine("ItemsAdder API error for " + fullItemId + ": " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Convert layer path to icon path for ItemsAdder
     * Layer: "contents/my_armor/textures/armor/politie/layer_1"
     * Icon: "contents/my_armor/textures/item/politie" (boots.png is implicit)
     * 
     * Icons are always in: {namespace}/textures/item/{armor_name}/boots.png
     */
    private String convertLayerPathToIconPath(String layerPath, String namespace, String armorName) {
        try {
            String[] pathParts = layerPath.split("/");
            
            // Find the "textures" directory and replace "armor" with "item"
            // "contents/my_armor/textures/armor/politie/layer_1" -> "contents/my_armor/textures/item/politie"
            StringBuilder iconPath = new StringBuilder();
            boolean foundTextures = false;
            
            for (int i = 0; i < pathParts.length; i++) {
                String part = pathParts[i];
                
                // Skip namespace (first part) as it's already separate
                if (i == 0) {
                    continue;
                }
                
                // Once we find textures, replace armor with item
                if (part.equalsIgnoreCase("textures")) {
                    foundTextures = true;
                    if (iconPath.length() > 0) iconPath.append("/");
                    iconPath.append(part);
                } else if (foundTextures && part.equalsIgnoreCase("armor")) {
                    // Replace "armor" with "item"
                    if (iconPath.length() > 0) iconPath.append("/");
                    iconPath.append("item");
                } else if (part.startsWith("layer_")) {
                    // Stop at layer_X, don't include it (boots.png is implicit)
                    break;
                } else if (!foundTextures || !part.equalsIgnoreCase("armor")) {
                    // Include all other parts
                    if (iconPath.length() > 0) iconPath.append("/");
                    iconPath.append(part);
                }
            }
            
            // Ensure we have textures/item/{armor_name}
            String result = iconPath.toString();
            if (!result.contains("textures/item")) {
                // Fallback: construct standard path
                return "textures/item/" + armorName;
            }
            
            return result;
        } catch (Exception e) {
            // Fallback to standard icon path
            return "textures/item/" + armorName;
        }
    }
    
    /**
     * Set ItemsAdder custom data on item meta
     * ItemsAdder uses a specific NBT structure for custom items
     */
    private void setItemsAdderCustomData(ItemMeta meta, String namespace, String itemId) {
        try {
            // ItemsAdder stores custom data in PersistentDataContainer
            // Format: itemsadder:namespace and itemsadder:id
            var persistentData = meta.getPersistentDataContainer();
            
            // Primary ItemsAdder keys
            NamespacedKey namespaceKey = new NamespacedKey("itemsadder", "namespace");
            NamespacedKey idKey = new NamespacedKey("itemsadder", "id");
            
            // Set namespace and id directly
            persistentData.set(namespaceKey, org.bukkit.persistence.PersistentDataType.STRING, namespace);
            persistentData.set(idKey, org.bukkit.persistence.PersistentDataType.STRING, itemId);
            
            // Also try to set the compound structure if supported
            // This ensures maximum compatibility with ItemsAdder
            try {
                // Try to create a compound data structure
                var compoundData = persistentData.getAdapterContext().newPersistentDataContainer();
                compoundData.set(namespaceKey, org.bukkit.persistence.PersistentDataType.STRING, namespace);
                compoundData.set(idKey, org.bukkit.persistence.PersistentDataType.STRING, itemId);
                // Note: Setting compound containers may not be directly supported in all versions
            } catch (Exception ignored) {
                // Compound structure not supported, continue with direct keys
            }
        } catch (Exception e) {
            // ItemsAdder custom data setting is optional, log but don't fail
            OpenMinetopia.getInstance().getLogger().fine("Could not set ItemsAdder custom data: " + e.getMessage());
        }
    }

    /**
     * Get the maximum allowed level for a boot effect type
     */
    private int getMaxLevel(BootEffectType effectType) {
        return switch (effectType) {
            case ICE -> 2;
            case BLUB -> 3;
            case SPEED -> 9;
        };
    }

    /**
     * Add movement speed attribute modifier to boots
     */
    private void addSpeedModifier(ItemMeta meta, int level) {
        double speedIncrease = level * 0.01; // 0.01 to 0.09
        
        // Create UNIQUE NamespacedKey for this modifier so multiple boots can stack
        // Each boot gets a unique UUID to prevent modifiers from overwriting each other
        NamespacedKey key = new NamespacedKey(OpenMinetopia.getInstance(), "speed_boost_" + UUID.randomUUID());
        
        // Level 3 and higher boots can ONLY work when worn on feet (not in hand)
        // Level 1-2 boots can work in hand if configuration allows it
        boolean worksInHand = level < 3 && OpenMinetopia.getDefaultConfiguration().isBootsWorkInHand();
        
        // Get movement speed attribute from registry (modern 1.21+ API)
        Attribute movementSpeed = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ATTRIBUTE)
                .get(NamespacedKey.minecraft("movement_speed"));
        
        if (movementSpeed == null) {
            OpenMinetopia.getInstance().getLogger().warning("Could not find movement_speed attribute!");
            return;
        }
        
        AttributeModifier modifier;
        if (worksInHand) {
            // Works anywhere (even in hand) - only for level 1-2 if config allows
            modifier = new AttributeModifier(
                    key,
                    speedIncrease,
                    AttributeModifier.Operation.ADD_NUMBER
            );
        } else {
            // Only works when worn on feet (always for level 3+, or level 1-2 if config disallows)
            modifier = new AttributeModifier(
                    key,
                    speedIncrease,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.FEET
            );
        }
        
        meta.addAttributeModifier(movementSpeed, modifier);
    }
}

