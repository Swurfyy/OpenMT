package nl.openminetopia.modules.player.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.player.utils.PlaytimeUtil;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@CommandAlias("time|playtime")
public class PlaytimeCommand extends BaseCommand {

    @Default
    @CommandCompletion("@players")
    @CommandPermission("openminetopia.playtime")
    @Description("Get your or another player's playtime.")
    public void onPlaytimeCommand(Player player, @Optional OfflinePlayer target) {
        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getMinetopiaPlayer(player);

        if (target != null && player.hasPermission("openminetopia.playtime.others")) {
            MinetopiaPlayer targetMinetopiaPlayer = PlayerManager.getInstance().getMinetopiaPlayer(target);
            if (targetMinetopiaPlayer == null) {
                ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("player_not_found"));
                return;
            }

            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("player_time_other_player")
                    .replace("<player>", (target.getName() == null ? "null" : target.getName()))
                    .replace("<playtime>", PlaytimeUtil.formatPlaytime(targetMinetopiaPlayer.getPlaytime())));
            return;
        }

        if (minetopiaPlayer == null) {
            ChatUtils.sendMessage(player, MessageConfiguration.message("database_read_error"));
            return;
        }

        ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("player_time_self")
                .replace("<playtime>", PlaytimeUtil.formatPlaytime(minetopiaPlayer.getPlaytime())));
    }
}
