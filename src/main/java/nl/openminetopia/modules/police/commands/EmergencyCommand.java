package nl.openminetopia.modules.police.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.configuration.MessageConfiguration;
import nl.openminetopia.modules.police.PoliceModule;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandAlias("112|911")
public class EmergencyCommand extends BaseCommand {

    @Default
    public void emergency(Player player, String message) {
        if (hasCooldown(player)) {
            PlayerManager.getInstance().getMinetopiaPlayerAsync(player, minetopiaPlayer -> {
                long cooldown = OpenMinetopia.getModuleManager().getModule(PoliceModule.class).getEmergencyCooldowns().get(player.getUniqueId());
                ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("emergency_too_soon"));
                ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("emergency_cooldown")
                        .replace("<time>", cooldownToTime(cooldown)));
            }, Throwable::printStackTrace);
            return;
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.hasPermission("openminetopia.police")) return;

            PlayerManager.getInstance().getMinetopiaPlayerAsync(onlinePlayer, minetopiaPlayer -> {
                ChatUtils.sendFormattedMessage(minetopiaPlayer, MessageConfiguration.message("emergency_format")
                        .replace("<sender>", player.getName())
                        .replace("<message>", message));
            }, Throwable::printStackTrace);
        }

        OpenMinetopia.getModuleManager().getModule(PoliceModule.class).getEmergencyCooldowns().put(player.getUniqueId(), System.currentTimeMillis());
    }


    private boolean hasCooldown(Player player) {
        PoliceModule policeModule = OpenMinetopia.getModuleManager().getModule(PoliceModule.class);

        if (!policeModule.getEmergencyCooldowns().containsKey(player.getUniqueId())) return false;

        long cooldown = policeModule.getEmergencyCooldowns().get(player.getUniqueId());
        // check if cooldown of 5 minutes has passed and remove from map if so
        if (System.currentTimeMillis() - cooldown >= (OpenMinetopia.getDefaultConfiguration().getEmergencyCooldown() * 1000L)) {
            policeModule.getEmergencyCooldowns().remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    private String cooldownToTime(long cooldown) {
        long millis = (OpenMinetopia.getDefaultConfiguration().getEmergencyCooldown() * 1000L) - (System.currentTimeMillis() - cooldown);
        long seconds = millisToSeconds(millis);
        long minutes = millisToMinutes(millis);
        return minutes + " minuten en " + seconds + " seconden";
    }

    private long millisToSeconds(long millis) {
        return millis / 1000;
    }

    private long millisToMinutes(long millis) {
        return millis / 60000;
    }
}
