package nl.openminetopia.modules.fitness;

import com.craftmend.storm.api.builders.QueryBuilder;
import com.craftmend.storm.api.enums.Where;
import nl.openminetopia.api.player.fitness.objects.Fitness;
import nl.openminetopia.api.player.fitness.objects.FitnessBooster;
import nl.openminetopia.api.player.fitness.statistics.FitnessStatistic;
import nl.openminetopia.api.player.fitness.statistics.enums.FitnessStatisticType;
import nl.openminetopia.api.player.fitness.statistics.types.*;
import nl.openminetopia.modules.Module;
import nl.openminetopia.modules.data.storm.StormDatabase;
import nl.openminetopia.modules.data.storm.models.FitnessBoosterModel;
import nl.openminetopia.modules.data.storm.models.FitnessModel;
import nl.openminetopia.modules.data.utils.StormUtils;
import nl.openminetopia.modules.fitness.commands.FitnessCommand;
import nl.openminetopia.modules.fitness.commands.subcommands.FitnessBoosterCommand;
import nl.openminetopia.modules.fitness.commands.subcommands.FitnessInfoCommand;
import nl.openminetopia.modules.fitness.listeners.PlayerDeathListener;
import nl.openminetopia.modules.fitness.listeners.PlayerDrinkListener;
import nl.openminetopia.modules.fitness.listeners.PlayerChangeWorldListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class FitnessModule extends Module {

    
    public void enable() {
        registerCommand(new FitnessCommand());
        registerCommand(new FitnessInfoCommand());
        registerCommand(new FitnessBoosterCommand());

        registerListener(new PlayerDrinkListener());
        registerListener(new PlayerDrinkListener());
        registerListener(new PlayerDeathListener());
        registerListener(new PlayerChangeWorldListener());
    }

    
    public void disable() {

    }

    public CompletableFuture<FitnessModel> getFitness(Fitness fitness) {
        CompletableFuture<FitnessModel> completableFuture = new CompletableFuture<>();

        StormDatabase.getExecutorService().submit(() -> {
            QueryBuilder<FitnessModel> query = StormDatabase.getInstance().getStorm().buildQuery(FitnessModel.class);

            try {
                CompletableFuture<Collection<FitnessModel>> models = query.where("uuid", Where.EQUAL, fitness.getUuid().toString()).execute();

                models.whenComplete((fitnessModels, throwable) -> {
                    FitnessModel model = fitnessModels.stream().findFirst().orElse(null);
                    if (model == null) {
                        model = new FitnessModel();
                        model.setUniqueId(fitness.getUuid());
                        model.setTotal(0);
                        model.setFitnessGainedByWalking(0);
                        model.setFitnessGainedByDrinking(0);
                        model.setDrinkingPoints(0.0);
                        model.setFitnessGainedBySprinting(0);
                        model.setFitnessGainedByClimbing(0);
                        model.setFitnessGainedBySwimming(0);
                        model.setFitnessGainedByFlying(0);
                        model.setFitnessGainedByHealth(0);
                        model.setHealthPoints(0);
                        model.setFitnessGainedByEating(0);
                        model.setLuxuryFood(0);
                        model.setCheapFood(0);
                        StormDatabase.getInstance().saveStormModel(model);
                        getStatistics(fitness);
                    }
                    completableFuture.complete(model);
                });
            } catch (Exception e) {
                e.printStackTrace();
                completableFuture.completeExceptionally(e);
            }
        });
        return completableFuture;
    }

    public CompletableFuture<Void> saveStatistics(Fitness fitness) {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();

        getFitness(fitness).thenAccept(model -> {
                    model.setTotal((fitness.getStatistic(FitnessStatisticType.TOTAL).getFitnessGained()));
                    model.setFitnessGainedByWalking(fitness.getStatistic(FitnessStatisticType.WALKING).getFitnessGained());
                    model.setFitnessGainedBySprinting(fitness.getStatistic(FitnessStatisticType.SPRINTING).getFitnessGained());
                    model.setFitnessGainedByClimbing(fitness.getStatistic(FitnessStatisticType.CLIMBING).getFitnessGained());
                    model.setFitnessGainedBySwimming(fitness.getStatistic(FitnessStatisticType.SWIMMING).getFitnessGained());
                    model.setFitnessGainedByFlying(fitness.getStatistic(FitnessStatisticType.FLYING).getFitnessGained());

                    DrinkingStatistic drinkingStatistic = (DrinkingStatistic) fitness.getStatistic(FitnessStatisticType.DRINKING);
                    model.setFitnessGainedByDrinking(drinkingStatistic.getFitnessGained());
                    model.setDrinkingPoints(drinkingStatistic.getPoints());

                    HealthStatistic healthStatistic = (HealthStatistic) fitness.getStatistic(FitnessStatisticType.HEALTH);
                    model.setFitnessGainedByHealth(healthStatistic.getFitnessGained());
                    model.setHealthPoints(healthStatistic.getPoints());

                    EatingStatistic eatingStatistic = (EatingStatistic) fitness.getStatistic(FitnessStatisticType.EATING);
                    model.setFitnessGainedByEating(eatingStatistic.getFitnessGained());
                    model.setLuxuryFood(eatingStatistic.getLuxuryFood());
                    model.setCheapFood(eatingStatistic.getCheapFood());
                    StormDatabase.getInstance().saveStormModel(model);
                }
        );


        completableFuture.complete(null);
        return completableFuture;
    }
    
    public CompletableFuture<List<FitnessStatistic>> getStatistics(Fitness fitness) {
        CompletableFuture<List<FitnessStatistic>> completableFuture = new CompletableFuture<>();

        StormUtils.getModelData(FitnessModel.class, query -> query.where("uuid", Where.EQUAL, fitness.getUuid().toString()), null,
                model -> {
                    List<FitnessStatistic> stats = new ArrayList<>();

                    stats.add(new TotalStatistic(model.getTotal()));
                    stats.add(new WalkingStatistic(model.getFitnessGainedByWalking()));
                    stats.add(new DrinkingStatistic(model.getFitnessGainedByDrinking(), model.getHealthPoints()));
                    stats.add(new SprintingStatistic(model.getFitnessGainedBySprinting()));
                    stats.add(new ClimbingStatistic(model.getFitnessGainedByClimbing()));
                    stats.add(new SwimmingStatistic(model.getFitnessGainedBySwimming()));
                    stats.add(new FlyingStatistic(model.getFitnessGainedByFlying()));
                    stats.add(new HealthStatistic(model.getFitnessGainedByHealth(), model.getHealthPoints()));
                    stats.add(new EatingStatistic(model.getFitnessGainedByEating(), model.getLuxuryFood(), model.getCheapFood()));

                    completableFuture.complete(stats);
                    return completableFuture;
                },
                List.of()
        );

        return completableFuture;
    }
    
    public CompletableFuture<FitnessStatistic> getStatistic(Fitness fitness, FitnessStatisticType type) {
        CompletableFuture<FitnessStatistic> completableFuture = new CompletableFuture<>();

        StormUtils.getModelData(FitnessModel.class,
                query -> query.where("uuid", Where.EQUAL, fitness.getUuid().toString()),
                null,
                model -> switch (type) {
                    case TOTAL -> new TotalStatistic(model.getTotal());
                    case WALKING -> new WalkingStatistic(model.getFitnessGainedByWalking());
                    case DRINKING -> new DrinkingStatistic(model.getFitnessGainedByDrinking(), model.getHealthPoints());
                    case SPRINTING -> new SprintingStatistic(model.getFitnessGainedBySprinting());
                    case CLIMBING -> new ClimbingStatistic(model.getFitnessGainedByClimbing());
                    case SWIMMING -> new SwimmingStatistic(model.getFitnessGainedBySwimming());
                    case FLYING -> new FlyingStatistic(model.getFitnessGainedByFlying());
                    case HEALTH -> new HealthStatistic(model.getFitnessGainedByHealth(), model.getHealthPoints());
                    case EATING -> new EatingStatistic(model.getFitnessGainedByEating(), model.getLuxuryFood(), model.getCheapFood());
                },
                null
        ).whenComplete((statistic, ex) -> {
            if (ex != null) {
                ex.printStackTrace();
                completableFuture.completeExceptionally(ex);
                return;
            }
            completableFuture.complete(statistic);
        });
        return completableFuture;
    }
    
    public CompletableFuture<Void> saveFitnessBoosters(Fitness fitness) {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();

        StormDatabase.getExecutorService().submit(() -> {

            try {
                Collection<FitnessBoosterModel> fitnessBoosterModels = StormDatabase.getInstance().getStorm().buildQuery(FitnessBoosterModel.class)
                        .where("fitness_id", Where.EQUAL, fitness.getFitnessModel().getId())
                        .execute()
                        .join();

                // loop through fitness.getBoosters() and see if the booster is already in the database
                // if it is not, add it to the database
                fitness.getBoosters().forEach(booster -> {
                    if (fitnessBoosterModels.stream().noneMatch(model -> model.getId().equals(booster.getId()))) {
                        FitnessBoosterModel model = new FitnessBoosterModel();
                        model.setFitnessId(fitness.getFitnessModel().getId());
                        model.setFitness(booster.getAmount());
                        model.setExpiresAt(booster.getExpiresAt());
                        StormDatabase.getInstance().saveStormModel(model);
                    }
                });
                completableFuture.complete(null);
            } catch (Exception e) {
                completableFuture.completeExceptionally(e);
                e.printStackTrace();
            }
        });

        return completableFuture;
    }
    
    public CompletableFuture<List<FitnessBooster>> getFitnessBoosters(Fitness fitness) {
        CompletableFuture<List<FitnessBooster>> completableFuture = new CompletableFuture<>();

        StormDatabase.getExecutorService().submit(() -> {
            List<FitnessBooster> fitnessBoosters = fitness.getFitnessModel().getBoosters().stream().map(model -> new FitnessBooster(model.getId(), model.getFitness(), model.getExpiresAt())).collect(Collectors.toList());

            completableFuture.complete(fitnessBoosters);
        });

        return completableFuture;
    }
    
    public CompletableFuture<Integer> addFitnessBooster(Fitness fitness, FitnessBooster booster) {
        CompletableFuture<Integer> completableFuture = new CompletableFuture<>();

        StormDatabase.getExecutorService().submit(() -> {
            FitnessBoosterModel fitnessBoosterModel = new FitnessBoosterModel();
            fitnessBoosterModel.setFitnessId(fitness.getFitnessModel().getId());
            fitnessBoosterModel.setFitness(booster.getAmount());
            fitnessBoosterModel.setExpiresAt(booster.getExpiresAt());

            int id = StormDatabase.getInstance().saveStormModel(fitnessBoosterModel).join();
            completableFuture.complete(id);
        });

        return completableFuture;
    }
    
    public CompletableFuture<Void> removeFitnessBooster(Fitness fitness, FitnessBooster booster) {
        return StormUtils.deleteModelData(FitnessBoosterModel.class,
                query -> query.where("id", Where.EQUAL, booster.getId())
        );
    }
}