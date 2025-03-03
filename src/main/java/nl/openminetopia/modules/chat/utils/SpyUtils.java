package nl.openminetopia.modules.chat.utils;

import lombok.experimental.UtilityClass;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

@UtilityClass
public class SpyUtils {

    public void chatSpy(Player player, String message, List<Player> ignore) {
        String spiedMessage = MessageConfiguration.message("chat_chatspy_format")
                .replace("<player>", player.getName())
                .replace("<message>", message);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getUniqueId().equals(player.getUniqueId())) continue;
            if (ignore.contains(onlinePlayer)) continue;

            MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getOnlineMinetopiaPlayer(player);
            if (minetopiaPlayer == null) return;

            if (minetopiaPlayer.isChatSpyEnabled()) ChatUtils.sendFormattedMessage(minetopiaPlayer, spiedMessage);
        }
    }

    public void commandSpy(Player player, String command) {
        String spiedMessage = MessageConfiguration.message("chat_commandspy_format")
                .replace("<player>", player.getName())
                .replace("<command>", command);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getUniqueId().equals(player.getUniqueId())) continue;

            MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getOnlineMinetopiaPlayer(player);
            if (minetopiaPlayer == null) return;

            if (minetopiaPlayer.isCommandSpyEnabled()) ChatUtils.sendFormattedMessage(minetopiaPlayer, spiedMessage);
        }
    }
}
