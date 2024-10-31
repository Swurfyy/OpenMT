package nl.openminetopia.modules.fitness.runnables;

import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.fitness.Fitness;
import nl.openminetopia.api.player.fitness.FitnessStatisticType;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.configuration.FitnessConfiguration;
import nl.openminetopia.modules.fitness.models.FitnessBoosterModel;
import nl.openminetopia.modules.fitness.models.FitnessStatisticModel;
import nl.openminetopia.modules.fitness.utils.FitnessUtils;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class FitnessRunnable extends BukkitRunnable {

    private final Fitness fitness;
    private final Player player;

    public FitnessRunnable(Fitness fitness) {
        this.fitness = fitness;
        this.player = Bukkit.getPlayer(fitness.getUuid());
    }

    @Override
    public void run() {
        FitnessConfiguration config = OpenMinetopia.getFitnessConfiguration();

        if (player == null || !player.isOnline()) {
            cancel();
            return;
        }

        fitness.getPlayerModel().getBoosters().forEach(booster -> {
            if (booster.isExpired()) fitness.removeBooster(booster);
        });

        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getMinetopiaPlayer(player);
        if (minetopiaPlayer == null || !minetopiaPlayer.isInPlace()) {
            FitnessUtils.clearFitnessEffects(player);
            return;
        }

        updateFitnessStatistic(FitnessStatisticType.WALKING, Statistic.WALK_ONE_CM);
        updateFitnessStatistic(FitnessStatisticType.CLIMBING, Statistic.CLIMB_ONE_CM);
        updateFitnessStatistic(FitnessStatisticType.SPRINTING, Statistic.SPRINT_ONE_CM);
        updateFitnessStatistic(FitnessStatisticType.SWIMMING, Statistic.SWIM_ONE_CM);
        updateFitnessStatistic(FitnessStatisticType.FLYING, Statistic.AVIATE_ONE_CM);
        updateEatingFitness();

        int totalFitness = calculateTotalFitness() + calculateFitnessBoost();
        fitness.setTotalFitness(Math.min(totalFitness, config.getMaxFitnessLevel()));
        fitness.apply();
    }

    private void updateFitnessStatistic(FitnessStatisticType type, Statistic statistic) {
        FitnessStatisticModel statModel = fitness.getStatistic(type);
        int currentDistance = player.getStatistic(statistic);
        int newFitness = FitnessUtils.calculateFitness(currentDistance, (int) statModel.getProgressPerPoint());

        if (statModel.getFitnessGained() != newFitness && newFitness <= statModel.getMaximum()) {
            statModel.setFitnessGained(newFitness);
        }

        fitness.setStatistic(type, statModel);
    }

    private void updateEatingFitness() {
        FitnessConfiguration config = OpenMinetopia.getFitnessConfiguration();

        FitnessStatisticModel eatingStat = fitness.getStatistic(FitnessStatisticType.EATING);
        double eatingPoints = (eatingStat.getSecondaryPoints() * config.getPointsForCheapFood())
                + (eatingStat.getTertiaryPoints() * config.getPointsForLuxuryFood());

        eatingStat.setPoints(eatingPoints);
        if (eatingPoints >= 1 && eatingStat.getFitnessGained() <= eatingStat.getMaximum()) {
            eatingStat.setFitnessGained(eatingStat.getFitnessGained() + 1);
            eatingStat.setPoints(0.0);
        }

        fitness.setStatistic(FitnessStatisticType.EATING, eatingStat);
    }

    private int calculateTotalFitness() {
        FitnessConfiguration config = OpenMinetopia.getFitnessConfiguration();

        return fitness.getStatistics().stream()
                .mapToInt(FitnessStatisticModel::getFitnessGained)
                .sum() + config.getDefaultFitnessLevel();
    }

    private int calculateFitnessBoost() {
        return fitness.getBoosters().stream()
                .mapToInt(FitnessBoosterModel::getAmount)
                .sum();
    }
}