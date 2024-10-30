package nl.openminetopia.modules.fitness.listeners;

import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.fitness.FitnessStatisticType;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.configuration.FitnessConfiguration;
import nl.openminetopia.modules.fitness.models.FitnessStatisticModel;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class PlayerEatListener implements Listener {

    @EventHandler
    public void playerEat(final PlayerItemConsumeEvent event) {

        if (!event.getItem().getType().isEdible()) return;
        FitnessConfiguration configuration = OpenMinetopia.getFitnessConfiguration();

        OpenMinetopia.getInstance().getLogger().info("Player ate: " + event.getItem().getType().name());
        if (!configuration.getCheapFood().contains(event.getItem().getType().name()) && !configuration.getLuxuryFood().contains(event.getItem().getType().name())) return;

        OpenMinetopia.getInstance().getLogger().info("Player ate food form config: " + event.getItem().getType().name());

        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getMinetopiaPlayer(event.getPlayer());
        if (minetopiaPlayer == null) return;

        FitnessStatisticModel eatingStatistic = minetopiaPlayer.getFitness().getStatistic(FitnessStatisticType.EATING);

        if (configuration.getCheapFood().contains(event.getItem().getType().name())) {
            eatingStatistic.setPoints(eatingStatistic.getPoints() + configuration.getPointsForCheapFood());
            eatingStatistic.setSecondaryPoints(eatingStatistic.getSecondaryPoints() + 1);
        } else if (configuration.getLuxuryFood().contains(event.getItem().getType().name())) {
            eatingStatistic.setPoints(eatingStatistic.getPoints() + configuration.getPointsForLuxuryFood());
            eatingStatistic.setTertiaryPoints(eatingStatistic.getTertiaryPoints() + 1);
        }

        double currentEatingPoints = eatingStatistic.getPoints();

        if (eatingStatistic.getPoints() >= 1 && eatingStatistic.getFitnessGained() <= configuration.getMaxFitnessByDrinking()) {
            if (currentEatingPoints % (eatingStatistic.getSecondaryPoints() + eatingStatistic.getTertiaryPoints()) == 0) {
                minetopiaPlayer.getFitness().setStatistic(FitnessStatisticType.EATING, eatingStatistic);
                return;
            }
            eatingStatistic.setFitnessGained(eatingStatistic.getFitnessGained() + 1);
            eatingStatistic.setPoints(0.0);
        }
        minetopiaPlayer.getFitness().setStatistic(FitnessStatisticType.EATING, eatingStatistic);
    }
}
