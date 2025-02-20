package nl.openminetopia.modules.prefix.commands.subcommands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
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
     * @param expiresAt The time in minutes when the prefix expires. (-1 for never)
     */
    @Subcommand("add")
    @Syntax("<speler> <prefix> [<minuten>]")
    @CommandCompletion("@players @range:0-1440")
    @CommandPermission("openminetopia.prefix.add")
    @Description("Voeg een prefix toe aan een speler voor een bepaalde tijd.")
    public void addPrefix(Player player, OfflinePlayer offlinePlayer, String prefix, @Optional Integer expiresAt) {
        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getOnlineMinetopiaPlayer(player);
        if (minetopiaPlayer == null) return;

        if (offlinePlayer == null) {
            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("player_not_found"));
            return;
        }

        if (expiresAt == null) {
            expiresAt = -1;
        }
        int finalExpiresAt = expiresAt;

        PlayerManager.getInstance().getMinetopiaPlayer(offlinePlayer).whenComplete((targetMinetopiaPlayer, throwable1) -> {
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

            long expiresAtMillis = System.currentTimeMillis() + minutesToMillis(finalExpiresAt);
            if (finalExpiresAt == -1) expiresAtMillis = -1;

            Prefix prefix1 = new Prefix(prefix, expiresAtMillis);
            targetMinetopiaPlayer.addPrefix(prefix1);

            ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("prefix_added")
                    .replace("<player>", (offlinePlayer.getName() == null ? "null" : offlinePlayer.getName()))
                    .replace("<prefix>", prefix)
                    .replace("<time>", finalExpiresAt == -1 ? "nooit" : PlaytimeUtil.formatPlaytime(minutesToMillis(finalExpiresAt))));
        });
    }

    private int minutesToMillis(int minutes) {
        return minutes * 60 * 1000;
    }
}