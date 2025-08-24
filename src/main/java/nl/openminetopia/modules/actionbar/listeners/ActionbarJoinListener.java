package nl.openminetopia.modules.actionbar.listeners;

import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.configuration.DefaultConfiguration;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ActionbarJoinListener implements Listener {

    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        PlayerManager.getInstance().getMinetopiaPlayer(player).whenComplete((minetopiaPlayer, throwable) -> {
            if (minetopiaPlayer == null) {
                player.kick(ChatUtils.color("<red>Er is een fout opgetreden bij het laden van je gegevens! Probeer het later opnieuw."));
                return;
            }

            minetopiaPlayer.setActionbarVisible(true);

            Bukkit.getServer().getScheduler().runTaskTimer(OpenMinetopia.getInstance(), task -> {
                DefaultConfiguration configuration = OpenMinetopia.getDefaultConfiguration();
                if (!configuration.isActionbarEnabled()) return;
                if (!player.isOnline()) {
                    task.cancel();
                    return;
                }
                if (!minetopiaPlayer.isActionbarVisible()) return;
                player.sendActionBar(ChatUtils.format(minetopiaPlayer, configuration.getActionbarText()));
            }, 0, 30L);
        });
    }
}