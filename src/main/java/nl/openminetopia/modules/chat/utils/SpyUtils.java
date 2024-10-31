package nl.openminetopia.modules.chat.utils;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.configuration.MessageConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

@UtilityClass
public class SpyUtils {

    public void chatSpy(Player player, String message, List<Player> ignore) {
        // TODO: Replace <player_name> <message> with actual values
        Component spiedMessage = MessageConfiguration.component("chat_chatspy_format");

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getUniqueId().equals(player.getUniqueId())) continue;
            if (ignore.contains(onlinePlayer)) continue;

            Optional<MinetopiaPlayer> optional = obtainPlayer(onlinePlayer);
            if (optional.isEmpty()) continue;

            MinetopiaPlayer mPlayer = optional.get();
            if (mPlayer.isChatSpyEnabled()) onlinePlayer.sendMessage(spiedMessage);
        }
    }

    public void commandSpy(Player player, String command) {
        // TODO: Replace <player_name> <command> with actual values
        Component spiedMessage = MessageConfiguration.component("chat_commandspy_format");

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getUniqueId().equals(player.getUniqueId())) continue;

            Optional<MinetopiaPlayer> optional = obtainPlayer(onlinePlayer);
            if (optional.isEmpty()) continue;

            MinetopiaPlayer mPlayer = optional.get();
            if (mPlayer.isCommandSpyEnabled()) onlinePlayer.sendMessage(spiedMessage);
        }
    }

    public Optional<MinetopiaPlayer> obtainPlayer(Player player) {
        MinetopiaPlayer mPlayer = PlayerManager.getInstance().getMinetopiaPlayer(player);
        return Optional.ofNullable(mPlayer);
    }

}
