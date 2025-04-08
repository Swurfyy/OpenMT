package nl.openminetopia.modules.player.utils;

import lombok.experimental.UtilityClass;
import nl.openminetopia.configuration.MessageConfiguration;

@UtilityClass
public class PlaytimeUtil {

    public static String formatPlaytime(long playtimeInMillis) {
        long days = playtimeInMillis / 86400;
        long hours = (playtimeInMillis % 86400) / 3600;
        long minutes = ((playtimeInMillis % 86400) % 3600) / 60;
        long seconds = ((playtimeInMillis % 86400) % 3600) % 60;
        return MessageConfiguration.message("player_time_format")
                .replace("<days>", days + "")
                .replace("<hours>", hours + "")
                .replace("<minutes>", minutes + "")
                .replace("<seconds>", seconds + "");
    }
}