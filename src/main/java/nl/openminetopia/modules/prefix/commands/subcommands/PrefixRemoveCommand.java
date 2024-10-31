package nl.openminetopia.modules.prefix.commands.subcommands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.prefix.objects.Prefix;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@CommandAlias("prefix")
public class PrefixRemoveCommand extends BaseCommand {

    @Subcommand("remove")
    @Syntax("<player> <prefix>")
    @CommandPermission("openminetopia.prefix.remove")
    @CommandCompletion("@players @playerPrefixes")
    @Description("Remove a prefix from a player.")
    public void removePrefix(Player player, OfflinePlayer offlinePlayer, String prefixName) {
        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getMinetopiaPlayer(player);
        if (minetopiaPlayer == null) return;

        if (offlinePlayer.getPlayer() == null) {
            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("player_not_found"));
            return;
        }

        MinetopiaPlayer targetMinetopiaPlayer = PlayerManager.getInstance().getMinetopiaPlayer(offlinePlayer);
        if (targetMinetopiaPlayer == null) return;

        for (Prefix prefix : targetMinetopiaPlayer.getPrefixes()) {
            if (prefix.getPrefix().equalsIgnoreCase(prefixName)) {
                ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("prefix_removed")
                        .replace("<player>", (offlinePlayer.getName() == null ? "null" : offlinePlayer.getName()))
                        .replace("<prefix>", prefix.getPrefix()));
                targetMinetopiaPlayer.removePrefix(prefix);
                return;
            }
        }
        ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("prefix_not_found")
                .replace("<player>", (offlinePlayer.getName() == null ? "null" : offlinePlayer.getName()))
                .replace("<prefix>", prefixName));
    }
}
