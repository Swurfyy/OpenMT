package nl.openminetopia.modules.player.runnables;

import com.jazzkuh.modulemanager.spigot.handlers.tasks.TaskInfo;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.modules.player.PlayerModule;
import nl.openminetopia.modules.player.configuration.LevelCheckConfiguration;
import nl.openminetopia.modules.player.events.PlayerLevelChangeEvent;
import nl.openminetopia.modules.player.utils.LevelUtil;
import nl.openminetopia.utils.events.EventUtils;
import org.bukkit.scheduler.BukkitRunnable;

@TaskInfo(repeating = true, async = true, period = 20L * 30)
public class LevelCalculateRunnable extends BukkitRunnable {

    private final PlayerManager playerManager;

    public LevelCalculateRunnable(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    @Override
    public void run() {
        for (MinetopiaPlayer minetopiaPlayer : playerManager.getOnlinePlayers().values()){
            if (minetopiaPlayer == null || !minetopiaPlayer.getBukkit().isOnline()) {
                cancel();
                return;
            }
            int oldLevel = minetopiaPlayer.getLevel();

            PlayerModule playerModule = OpenMinetopia.getModuleManager().get(PlayerModule.class);
            LevelCheckConfiguration configuration = playerModule.getConfiguration();
            if (!minetopiaPlayer.isInPlace()) return;
            int calculatedLevel = LevelUtil.calculateLevel(minetopiaPlayer);
            if (configuration.isAutoLevelUp()) {
                PlayerLevelChangeEvent event = new PlayerLevelChangeEvent(minetopiaPlayer.getBukkit().getPlayer(), oldLevel, calculatedLevel);
                if (EventUtils.callCancellable(event)) return;

                minetopiaPlayer.setLevel(calculatedLevel);
            }
            minetopiaPlayer.setCalculatedLevel(calculatedLevel);
        }
    }
}
