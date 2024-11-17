package nl.openminetopia.modules.player.runnables;

import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class PlaytimeRunnable extends BukkitRunnable {

    private final Player player;
    private final MinetopiaPlayer minetopiaPlayer;

    public PlaytimeRunnable(MinetopiaPlayer minetopiaPlayer) {
        this.minetopiaPlayer = minetopiaPlayer;
        this.player = minetopiaPlayer.getBukkit().getPlayer();
    }

    @Override
    public void run() {
        if (player == null || !player.isOnline()) {
            cancel();
            return;
        }

        if (minetopiaPlayer == null) return;

        int newPlaytime = minetopiaPlayer.getPlaytime() + 1;

        // If the new playtime is a multiple of 60, update the playtime in the database, so it's only updated every minute
        if (newPlaytime % 60 == 0) {
            minetopiaPlayer.setPlaytime(newPlaytime, true);
            return;
        }
        minetopiaPlayer.setPlaytime(newPlaytime, false);
    }
}



