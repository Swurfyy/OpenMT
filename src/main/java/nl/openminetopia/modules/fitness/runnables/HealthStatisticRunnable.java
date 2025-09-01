package nl.openminetopia.modules.fitness.runnables;

import com.jazzkuh.modulemanager.spigot.handlers.tasks.TaskInfo;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.modules.fitness.utils.FitnessUtils;
import nl.openminetopia.modules.player.utils.PlaytimeUtil;
import org.bukkit.scheduler.BukkitRunnable;

@TaskInfo(repeating = true, period = 20L * 25, async = true)
public class HealthStatisticRunnable extends BukkitRunnable {

    private final PlayerManager playerManager = PlayerManager.getInstance();

    @Override
    public void run() {
        for (MinetopiaPlayer minetopiaPlayer : playerManager.getOnlinePlayers().values()){
            if (minetopiaPlayer == null || !minetopiaPlayer.getBukkit().isOnline()) continue;
            if (PlaytimeUtil.minutes(minetopiaPlayer.getPlaytime()) % 60 == 0) FitnessUtils.performHealthCheck(minetopiaPlayer);
        }
    }

}