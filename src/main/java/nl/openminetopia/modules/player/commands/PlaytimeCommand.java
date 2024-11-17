package nl.openminetopia.modules.player.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.player.utils.PlaytimeUtil;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@CommandAlias("playtime")
public class PlaytimeCommand extends BaseCommand {

    @Default
    @CommandCompletion("@players")
    @Description("Get your or another player's playtime.")
    public void playtime(Player player, @Optional OfflinePlayer target) {
        // Retrieve the primary player's MinetopiaPlayer asynchronously
        PlayerManager.getInstance().getMinetopiaPlayerAsync(player, minetopiaPlayer -> {
            if (minetopiaPlayer == null) {
                ChatUtils.sendMessage(player, MessageConfiguration.message("database_read_error"));
                return;
            }

            // If no target is specified or the player lacks permission, display the primary player's playtime
            if (target == null || !player.hasPermission("openminetopia.playtime.others")) {
                ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("player_time_self")
                        .replace("<playtime>", PlaytimeUtil.formatPlaytime(minetopiaPlayer.getPlaytime())));
                return;
            }

            // Retrieve the target player's MinetopiaPlayer asynchronously
            PlayerManager.getInstance().getMinetopiaPlayerAsync(target, targetMinetopiaPlayer -> {
                if (targetMinetopiaPlayer == null) {
                    ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("player_not_found"));
                    return;
                }

                // Display the target player's playtime
                ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("player_time_other_player")
                        .replace("<player>", target.getName() == null ? "null" : target.getName())
                        .replace("<playtime>", PlaytimeUtil.formatPlaytime(targetMinetopiaPlayer.getPlaytime())));
            }, throwable -> {
                throwable.printStackTrace();
                ChatUtils.sendMessage(player, MessageConfiguration.message("database_read_error"));
            });
        }, throwable -> {
            throwable.printStackTrace();
            ChatUtils.sendMessage(player, MessageConfiguration.message("database_read_error"));
        });
    }
}
