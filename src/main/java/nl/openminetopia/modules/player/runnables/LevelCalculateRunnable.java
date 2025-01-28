package nl.openminetopia.modules.player.runnables;

import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.configuration.LevelCheckConfiguration;
import nl.openminetopia.modules.player.utils.LevelUtil;
import org.bukkit.scheduler.BukkitRunnable;

public class LevelCalculateRunnable extends BukkitRunnable {

    private final MinetopiaPlayer minetopiaPlayer;

    public LevelCalculateRunnable(MinetopiaPlayer minetopiaPlayer) {
        this.minetopiaPlayer = minetopiaPlayer;
    }

    @Override
    public void run() {
        if (minetopiaPlayer == null || !minetopiaPlayer.getBukkit().isOnline()) {
            cancel();
            return;
        }

        LevelCheckConfiguration configuration = OpenMinetopia.getLevelcheckConfiguration();
        if (!minetopiaPlayer.isInPlace()) return;
        int calculatedLevel = LevelUtil.calculateLevel(minetopiaPlayer);
        if (configuration.isAutoLevelUp()) {
            minetopiaPlayer.setLevel(calculatedLevel);
        }
        minetopiaPlayer.setCalculatedLevel(calculatedLevel);
    }
}
