package nl.openminetopia.modules.fitness.listeners;

import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.configuration.FitnessConfiguration;
import nl.openminetopia.modules.fitness.models.FitnessBoosterModel;
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
        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getMinetopiaPlayer(player);
        if (minetopiaPlayer == null) return;

        int punishmentInMillis = configuration.getFitnessDeathPunishmentDuration() * 60 * 1000;

        FitnessBoosterModel fitnessBooster = new FitnessBoosterModel();
        fitnessBooster.setFitnessId(minetopiaPlayer.getFitness().getFitnessModel().getId());
        fitnessBooster.setAmount(configuration.getFitnessDeathPunishmentAmount());
        fitnessBooster.setExpiresAt(System.currentTimeMillis() + punishmentInMillis);

        minetopiaPlayer.getFitness().addBooster(fitnessBooster);
        minetopiaPlayer.getFitness().getRunnable().run();
    }
}
