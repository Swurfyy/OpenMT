package nl.openminetopia.modules.fitness;

import lombok.Getter;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.fitness.Fitness;
import nl.openminetopia.modules.Module;
import nl.openminetopia.modules.data.storm.StormDatabase;
import nl.openminetopia.modules.fitness.commands.FitnessCommand;
import nl.openminetopia.modules.fitness.commands.subcommands.FitnessBoosterCommand;
import nl.openminetopia.modules.fitness.listeners.PlayerDeathListener;
import nl.openminetopia.modules.fitness.listeners.PlayerDrinkListener;
import nl.openminetopia.modules.fitness.listeners.PlayerChangeWorldListener;
import nl.openminetopia.modules.fitness.listeners.PlayerEatListener;
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

    public final Map<UUID, Long> lastDrinkingTimes = new HashMap<>();

    public void enable() {
        registerCommand(new FitnessCommand());
        registerCommand(new FitnessBoosterCommand());

        registerListener(new PlayerEatListener());
        registerListener(new PlayerDrinkListener());
        registerListener(new PlayerDeathListener());
        registerListener(new PlayerChangeWorldListener());
    }

    public void disable() {

    }
}