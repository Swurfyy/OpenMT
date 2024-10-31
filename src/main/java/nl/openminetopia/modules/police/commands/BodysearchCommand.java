package nl.openminetopia.modules.police.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.entity.Player;

@CommandAlias("bodysearch|fouilleer")
public class BodysearchCommand extends BaseCommand {

    @Default
    @Syntax("<player>")
    @Description("Fouilleer een speler.")
    @CommandPermission("openminetopia.bodysearch")
    public void bodysearch(Player player, OnlinePlayer onlineTarget) {
        Player target = onlineTarget.getPlayer();
        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getMinetopiaPlayer(player);
        if (player == target) {
            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("police_bodysearch_self"));
            return;
        }

        MinetopiaPlayer minetopiaTarget = PlayerManager.getInstance().getMinetopiaPlayer(target);

        if (minetopiaTarget == null) {
            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("player_not_found"));
            return;
        }

        if (player.getLocation().distance(target.getLocation()) > OpenMinetopia.getDefaultConfiguration().getBodysearchRange()) {
            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("police_bodysearch_too_far")
                    .replace("<player>", target.getName()));
            return;
        }

        if (!MessageConfiguration.message("police_bodysearch_executor").isEmpty())
            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("police_bodysearch_executor")
                    .replace("<player>", target.getName()));

        if (!MessageConfiguration.message("police_bodysearch_target").isEmpty())
            ChatUtils.sendFormattedMessage(minetopiaTarget, MessageConfiguration.message("police_bodysearch_target")
                    .replace("<player>", player.getName()));

        player.openInventory(target.getInventory());
    }
}
