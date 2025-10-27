package nl.openminetopia.modules.misc.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.configuration.DefaultConfiguration;
import nl.openminetopia.modules.misc.utils.MiscUtils;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

@CommandAlias("head")
public class HeadCommand extends BaseCommand {

    @Default
    public void head(Player player) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType() == Material.AIR || !MiscUtils.isValidHeadItem(itemInHand)) {
            player.sendMessage(ChatUtils.color("<red>Je kan dit item niet op je hoofd dragen!"));
            return;
        }

        // Create a copy with amount 1 for the helmet
        ItemStack itemToWear = itemInHand.clone();
        itemToWear.setAmount(1);

        // Handle old helmet - add it back to inventory or drop it
        if (player.getInventory().getHelmet() != null) {
            ItemStack oldHelmet = player.getInventory().getHelmet();
            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(oldHelmet);
            // Drop any items that couldn't fit in inventory
            for (ItemStack overflowItem : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), overflowItem);
            }
        }
        
        // Set the helmet (only 1 item)
        player.getInventory().setHelmet(itemToWear);
        
        // Remove 1 item from the hand
        ItemStack remainingItem = itemInHand.clone();
        remainingItem.setAmount(itemInHand.getAmount() - 1);
        
        if (remainingItem.getAmount() > 0) {
            player.getInventory().setItemInMainHand(remainingItem);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        player.sendMessage(ChatUtils.color("<green>Je hebt het item op je hoofd gezet!"));
    }

    @Subcommand("add")
    @CommandPermission("openminetopia.head.add")
    @Description("Voeg een item toe aan de head whitelist.")
    public void add(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() != Material.AIR && MiscUtils.isValidHeadItem(item)) {
            player.sendMessage(ChatUtils.color("<red>Dit item staat al op de head whitelist!"));
            return;
        }

        DefaultConfiguration configuration = OpenMinetopia.getDefaultConfiguration();
        configuration.addToHeadWhitelist(item);

        player.sendMessage(ChatUtils.color("<green>Je hebt het item toegevoegd aan de head whitelist!"));
    }

    @Subcommand("remove")
    @CommandPermission("openminetopia.head.remove")
    @Description("Verwijder een item van de head whitelist.")
    public void remove(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() != Material.AIR && !MiscUtils.isValidHeadItem(item)) {
            player.sendMessage(ChatUtils.color("<red>Dit item staat niet op de head whitelist!"));
            return;
        }

        DefaultConfiguration configuration = OpenMinetopia.getDefaultConfiguration();
        configuration.removeFromHeadWhitelist(item);

        player.sendMessage(ChatUtils.color("<green>Je hebt het item verwijderd van de head whitelist!"));
    }
}
