package nl.openminetopia.modules.fitness;

import lombok.Getter;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.fitness.Fitness;
import nl.openminetopia.modules.Module;
import nl.openminetopia.modules.data.storm.StormDatabase;
import nl.openminetopia.modules.fitness.commands.FitnessCommand;
import nl.openminetopia.modules.fitness.commands.subcommands.FitnessBoosterCommand;
import nl.openminetopia.modules.fitness.commands.subcommands.FitnessInfoCommand;
import nl.openminetopia.modules.fitness.listeners.PlayerDeathListener;
import nl.openminetopia.modules.fitness.listeners.PlayerDrinkListener;
import nl.openminetopia.modules.fitness.listeners.PlayerChangeWorldListener;
import nl.openminetopia.modules.fitness.listeners.PlayerEatListener;
import nl.openminetopia.modules.fitness.models.FitnessModel;
import nl.openminetopia.modules.player.models.PlayerModel;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
public class FitnessModule extends Module {

    public Map<UUID, Long> lastDrinkingTimes = new HashMap<>();
    public Collection<FitnessModel> fitnessModels = new ArrayList<>();

    public void enable() {
        registerCommand(new FitnessCommand());
        registerCommand(new FitnessInfoCommand());
        registerCommand(new FitnessBoosterCommand());

        registerListener(new PlayerEatListener());
        registerListener(new PlayerDrinkListener());
        registerListener(new PlayerDeathListener());
        registerListener(new PlayerChangeWorldListener());

        Bukkit.getScheduler().runTaskLater(OpenMinetopia.getInstance(), () -> {
            OpenMinetopia.getInstance().getLogger().info("Loading fitness...");

            this.getFitnessModels().whenComplete((fitnessModels, throwable) -> {
                if (throwable != null) {
                    OpenMinetopia.getInstance().getLogger().severe("Failed to load fitness: " + throwable.getMessage());
                    return;
                }

                this.fitnessModels = fitnessModels;
                OpenMinetopia.getInstance().getLogger().info("Loaded " + fitnessModels.size() + " fitness.");
            });
        }, 20L);
    }

    public void disable() {

    }

    public Fitness getFitnessFromPlayer(PlayerModel playerModel) {
        FitnessModel fitnessModel = playerModel.getFitness().stream().findFirst().orElse(null);
        if (fitnessModel == null) {
            fitnessModel = new FitnessModel();
            fitnessModel.setPlayerId(playerModel.getId());
            StormDatabase.getInstance().saveStormModel(fitnessModel);
        }
        return new Fitness(playerModel.getUniqueId(), fitnessModel);
    }

    public CompletableFuture<Collection<FitnessModel>> getFitnessModels() {
        CompletableFuture<Collection<FitnessModel>> completableFuture = new CompletableFuture<>();

        StormDatabase.getExecutorService().submit(() -> {
            try {
                Collection<FitnessModel> fitnessModels1 = StormDatabase.getInstance().getStorm().buildQuery(FitnessModel.class)
                        .execute().join();
                completableFuture.complete(fitnessModels1);
            } catch (Exception e) {
                completableFuture.completeExceptionally(e);
            }
        });

        return completableFuture;
    }
}