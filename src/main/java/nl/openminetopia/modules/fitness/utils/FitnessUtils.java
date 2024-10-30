package nl.openminetopia.modules.fitness.utils;

import lombok.experimental.UtilityClass;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.api.player.PlayerManager;
import nl.openminetopia.api.player.fitness.FitnessStatisticType;
import nl.openminetopia.api.player.objects.MinetopiaPlayer;
import nl.openminetopia.configuration.FitnessConfiguration;
import nl.openminetopia.modules.data.storm.StormDatabase;
import nl.openminetopia.modules.fitness.models.FitnessStatisticModel;
import nl.openminetopia.modules.fitness.objects.FitnessLevelEffect;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@UtilityClass
public class FitnessUtils {

    public static void applyFitness(Player player) {
        MinetopiaPlayer minetopiaPlayer = PlayerManager.getInstance().getMinetopiaPlayer(player);
        if (minetopiaPlayer == null || !minetopiaPlayer.isInPlace()) {
            clearFitnessEffects(player);
            return;
        }

        FitnessConfiguration config = OpenMinetopia.getFitnessConfiguration();
        int totalFitness = minetopiaPlayer.getFitness().getTotalFitness();

        // Get the closest matching fitness level effect
        FitnessLevelEffect effectLevel = config.getLevelEffects().entrySet().stream()
                .filter(entry -> totalFitness >= entry.getKey())
                .map(Map.Entry::getValue)
                .reduce((first, second) -> second) // Get the last (highest) matching effect
                .orElse(null);

        if (effectLevel == null) return; // No effect level applies

        // Apply walk speed and potion effects
        applyPlayerWalkSpeed(player, config, (float) effectLevel.getWalkSpeed());
        applyPotionEffects(player, effectLevel);
    }

    private static void applyPlayerWalkSpeed(Player player, FitnessConfiguration config, float walkSpeed) {
        Bukkit.getScheduler().runTask(OpenMinetopia.getInstance(), () -> {
            if (player.getWorld().hasStorm() && config.isRainSlowdownEnabled()) {
                player.setWalkSpeed(walkSpeed - 0.05f);
            } else {
                player.setWalkSpeed(walkSpeed);
            }
        });
    }

    private static void applyPotionEffects(Player player, FitnessLevelEffect effectLevel) {
        List<PotionEffect> activeEffects = effectLevel.getEffects().stream()
                .map(effect -> effect.split(":"))
                .filter(parts -> parts.length == 2)
                .map(parts -> {
                    PotionEffectType effectType = Registry.EFFECT.get(NamespacedKey.minecraft(parts[0].toLowerCase()));
                    int amplifier = Integer.parseInt(parts[1]) - 1;
                    return effectType != null && amplifier >= 0
                            ? new PotionEffect(effectType, PotionEffect.INFINITE_DURATION, amplifier, true, false)
                            : null;
                })
                .filter(Objects::nonNull)
                .toList();

        Bukkit.getScheduler().runTask(OpenMinetopia.getInstance(), () -> {
            activeEffects.forEach(effect -> {
                player.removePotionEffect(effect.getType()); // Remove existing effect
                player.addPotionEffect(effect); // Add new effect with updated duration and amplifier
            });
        });
    }

    public static void clearFitnessEffects(Player player) {
        FitnessConfiguration config = OpenMinetopia.getFitnessConfiguration();
        List<PotionEffectType> effectsToRemove = config.getLevelEffects().values().stream()
                .flatMap(levelEffect -> levelEffect.getEffects().stream())
                .map(effect -> effect.split(":")[0].toLowerCase())
                .distinct()
                .map(effectName -> Registry.EFFECT.get(NamespacedKey.minecraft(effectName)))
                .filter(Objects::nonNull)
                .toList();

        Bukkit.getScheduler().runTask(OpenMinetopia.getInstance(), () -> {
            effectsToRemove.forEach(player::removePotionEffect);
            player.setWalkSpeed(0.2f); // Reset walk speed to default
        });
    }

    public static int calculateFitness(int currentDistance, int amountOfCmPerPoint) {
        return (currentDistance - currentDistance % amountOfCmPerPoint) / amountOfCmPerPoint;
    }

    public void healthCheck(MinetopiaPlayer minetopiaPlayer) {
        Player player = minetopiaPlayer.getBukkit().getPlayer();
        if (player == null) return;

        FitnessStatisticModel healthStatistic = minetopiaPlayer.getFitness().getStatistic(FitnessStatisticType.HEALTH);
        double newHealthPoints = healthStatistic.getPoints();

        if (player.getFoodLevel() >= 18) {
            newHealthPoints += OpenMinetopia.getFitnessConfiguration().getPointsAbove9Hearts();
        } else if (player.getFoodLevel() <= 4) {
            newHealthPoints += OpenMinetopia.getFitnessConfiguration().getPointsBelow2Hearts();
        } else if (player.getFoodLevel() <= 10) {
            newHealthPoints += OpenMinetopia.getFitnessConfiguration().getPointsBelow5Hearts();
        }
        healthStatistic.setPoints(newHealthPoints);

        if (newHealthPoints >= 1 && newHealthPoints <= healthStatistic.getMaximum()) {
            healthStatistic.setFitnessGained(healthStatistic.getFitnessGained() + 1);
            healthStatistic.setPoints(0.0);
        }

        minetopiaPlayer.getFitness().setStatistic(FitnessStatisticType.HEALTH, healthStatistic);
    }
}