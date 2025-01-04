package nl.openminetopia.modules.fitness.listeners;

import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.configuration.FitnessConfiguration;
import nl.openminetopia.modules.fitness.utils.FitnessUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    @EventHandler
    public void playerDeath(final PlayerDeathEvent event) {
        FitnessConfiguration configuration = OpenMinetopia.getFitnessConfiguration();
        if (!configuration.isFitnessDeathPunishmentEnabled()) return;

        Player player = event.getEntity();
        PlayerManager.getInstance().getMinetopiaPlayer(player).whenComplete((minetopiaPlayer, throwable) -> {
            if (minetopiaPlayer == null) return;

            int punishmentInMillis = configuration.getFitnessDeathPunishmentDuration() * 60 * 1000;

            int amount = configuration.getFitnessDeathPunishmentAmount();
            long expiry = System.currentTimeMillis() + punishmentInMillis;

            FitnessUtils.clearFitnessEffects(player);

            minetopiaPlayer.getFitness().addBooster(amount, expiry);
            minetopiaPlayer.getFitness().getRunnable().run();
        });
    }
}
