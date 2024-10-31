package nl.openminetopia.modules.player.utils;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.utils.ChatUtils;

@UtilityClass
public class PlaytimeUtil {

    public static String formatPlaytime(int playtimeInSeconds) {
        int days = playtimeInSeconds / 86400;
        int hours = (playtimeInSeconds % 86400) / 3600;
        int minutes = ((playtimeInSeconds % 86400) % 3600) / 60;
        int seconds = ((playtimeInSeconds % 86400) % 3600) % 60;
        return MessageConfiguration.message("player_time_format")
                .replace("<days>", days + "")
                .replace("<hours>", hours + "")
                .replace("<minutes>", minutes + "")
                .replace("<seconds>", seconds + "");
    }
}
