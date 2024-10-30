package nl.openminetopia.modules.fitness.commands.subcommands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.fitness.FitnessStatisticType;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.modules.fitness.models.FitnessStatisticModel;
import nl.openminetopia.utils.ChatUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

@CommandAlias("fitness")
public class FitnessInfoCommand extends BaseCommand {

    @Subcommand("info")
    @Syntax("<player>")
    @CommandCompletion("@players")
    @CommandPermission("openminetopia.fitness.info")
    @Description("Get the fitness info of a player.")
    public void onInfoCommand(Player player, OfflinePlayer offlinePlayer) {
        if (offlinePlayer == null) {
            player.sendMessage(ChatUtils.color("<red>This player does not exist."));
            return;
        }

        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getMinetopiaPlayer(offlinePlayer);
        if (minetopiaPlayer == null) {
            player.sendMessage(ChatUtils.color("<red>There was an error loading the data of this player! Please try again later."));
            return;
        }

        player.sendMessage(ChatUtils.color("<dark_aqua>Fitness info of <aqua>" + offlinePlayer.getName() + "<dark_aqua>:"));
        player.sendMessage("");

        minetopiaPlayer.getFitness().getBoosters().forEach(booster -> player.sendMessage(
                ChatUtils.color("<dark_aqua>Booster: <aqua>" + booster.getAmount() + " - " + booster.getExpiresAt())
        ));
        player.sendMessage("");

        for (FitnessStatisticType type : FitnessStatisticType.values()) {
            FitnessStatisticModel statistic = minetopiaPlayer.getFitness().getStatistic(type);
            double kilometers = getKilometersFromStatistic(type, offlinePlayer);
            player.sendMessage(ChatUtils.color("<dark_aqua>" + type.name() + " kilometers: <aqua>" + Math.floor(kilometers) + "km"));
            player.sendMessage(ChatUtils.color("<dark_aqua>Fitness gained by " + type.name().toLowerCase() + ": <aqua>" +
                    statistic.getFitnessGained() + "<dark_aqua>/<aqua>" + statistic.getMaximum()));
            player.sendMessage("");
        }

        int totalFitness = minetopiaPlayer.getFitness().getTotalFitness();
        player.sendMessage(ChatUtils.color("<dark_aqua>Total fitness: <aqua>" + totalFitness));

        player.sendMessage(String.valueOf(player.getWalkSpeed()));
    }

    private double getKilometersFromStatistic(FitnessStatisticType type, OfflinePlayer offlinePlayer) {
        Statistic stat;
        switch (type) {
            case WALKING -> stat = Statistic.WALK_ONE_CM;
            case CLIMBING -> stat = Statistic.CLIMB_ONE_CM;
            case SPRINTING -> stat = Statistic.SPRINT_ONE_CM;
            case SWIMMING -> stat = Statistic.SWIM_ONE_CM;
            case FLYING -> stat = Statistic.AVIATE_ONE_CM;
            default -> {
                return 0.0;
            }
        }
        return offlinePlayer.getStatistic(stat) / 100000.0;
    }
}