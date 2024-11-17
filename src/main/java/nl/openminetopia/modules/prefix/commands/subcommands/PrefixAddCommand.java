package nl.openminetopia.modules.prefix.commands.subcommands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.player.utils.PlaytimeUtil;
import nl.openminetopia.modules.prefix.objects.Prefix;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@CommandAlias("prefix")
public class PrefixAddCommand extends BaseCommand {

    /**
     * Add a prefix to a player.
     *
     * @param expiresAt The time in minutes when the prefix expires.
     */
    @Subcommand("add")
    @Syntax("<player> <minutes> <prefix>")
    @CommandCompletion("@players")
    @CommandPermission("openminetopia.prefix.add")
    @Description("Add a prefix to a player.")
    public void addPrefix(Player player, OfflinePlayer offlinePlayer, Integer expiresAt, String prefix) {
        PlayerManager.getInstance().getMinetopiaPlayerAsync(player, minetopiaPlayer -> {
            if (minetopiaPlayer == null) return;

            if (offlinePlayer == null) {
                ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("player_not_found"));
                return;
            }

            PlayerManager.getInstance().getMinetopiaPlayerAsync(offlinePlayer, targetMinetopiaPlayer -> {
                if (targetMinetopiaPlayer == null) {
                    ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("player_not_found"));
                    return;
                }

                for (Prefix prefix1 : targetMinetopiaPlayer.getPrefixes()) {
                    if (prefix1.getPrefix().equalsIgnoreCase(prefix)) {
                        ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("prefix_already_exists")
                                .replace("<player>", (offlinePlayer.getName() == null ? "null" : offlinePlayer.getName()))
                                .replace("<prefix>", prefix));
                        return;
                    }
                }

                long expiresAtMillis = System.currentTimeMillis() + (expiresAt * 60 * 1000);

                if (expiresAt == -1) expiresAtMillis = -1;

                Prefix prefix1 = new Prefix(prefix, expiresAtMillis);
                targetMinetopiaPlayer.addPrefix(prefix1);

                ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("prefix_added")
                        .replace("<player>", (offlinePlayer.getName() == null ? "null" : offlinePlayer.getName()))
                        .replace("<prefix>", prefix)
                        .replace("<time>", expiresAt == -1 ? "nooit" : PlaytimeUtil.formatPlaytime(minutesToMillis(expiresAt))));
            }, Throwable::printStackTrace);
        }, Throwable::printStackTrace);
    }

    private int minutesToMillis(int minutes) {
        return minutes * 60 * 1000;
    }
}