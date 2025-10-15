package nl.openminetopia.modules.misc.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.misc.objects.BootEffectType;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

@CommandAlias("boots")
@CommandPermission("openminetopia.boots")
@Description("Create special boots with different effects.")
public class BootsCommand extends BaseCommand {

    @Default
    @Syntax("<material|hand> <effect_type> <level>")
    @CommandCompletion("leather|chainmail|iron|golden|diamond|hand speed|ice|blub 1|2|3|4|5|6|7|8|9")
    public void onBoots(Player player, String materialArg, String effectTypeArg, int level) {
        // Validate level
        if (level < 1 || level > 9) {
            player.sendMessage(ChatUtils.color("<red>Level must be between 1 and 9!"));
            return;
        }

        // Parse effect type
        BootEffectType effectType = BootEffectType.fromString(effectTypeArg);
        if (effectType == null) {
            player.sendMessage(ChatUtils.color("<red>Invalid effect type! Use: speed, ice, or blub"));
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
        applyBootEffect(boots, effectType, level);

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
     * Apply the specified effect to the boots
     */
    private void applyBootEffect(ItemStack boots, BootEffectType effectType, int level) {
        ItemMeta meta = boots.getItemMeta();
        if (meta == null) {
            return;
        }

        switch (effectType) {
            case SPEED -> addSpeedModifier(meta, level);
            case ICE -> {
                // Cap Frost Walker at level 2 (max vanilla level)
                int iceLevel = Math.min(level, 2);
                boots.addUnsafeEnchantment(Enchantment.FROST_WALKER, iceLevel);
            }
            case BLUB -> {
                // Cap Depth Strider at level 3 (max vanilla level)
                int blubLevel = Math.min(level, 3);
                boots.addUnsafeEnchantment(Enchantment.DEPTH_STRIDER, blubLevel);
            }
        }

        // Set display name if item doesn't have one
        if (!meta.hasDisplayName()) {
            meta.displayName(ChatUtils.color("<aqua>" + effectType.getDisplayName() + " Boots " + level));
        }

        boots.setItemMeta(meta);
    }

    /**
     * Add movement speed attribute modifier to boots
     */
    private void addSpeedModifier(ItemMeta meta, int level) {
        double speedIncrease = level * 0.01; // 0.01 to 0.09
        
        // Create UNIQUE NamespacedKey for this modifier so multiple boots can stack
        // Each boot gets a unique UUID to prevent modifiers from overwriting each other
        NamespacedKey key = new NamespacedKey(OpenMinetopia.getInstance(), "speed_boost_" + UUID.randomUUID());
        
        // Check configuration for whether boots work in hand
        boolean worksInHand = OpenMinetopia.getDefaultConfiguration().isBootsWorkInHand();
        
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
            // Works anywhere (even in hand)
            modifier = new AttributeModifier(
                    key,
                    speedIncrease,
                    AttributeModifier.Operation.ADD_NUMBER
            );
        } else {
            // Only works when worn on feet
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

