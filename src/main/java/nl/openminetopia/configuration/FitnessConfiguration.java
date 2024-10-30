package nl.openminetopia.configuration;

import lombok.Getter;
import lombok.SneakyThrows;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.fitness.objects.FitnessLevelEffect;
import nl.openminetopia.utils.ConfigurateConfig;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class FitnessConfiguration extends ConfigurateConfig {

    private final int maxFitnessLevel;
    private final int defaultFitnessLevel;

    private final int maxFitnessByDrinking;
    private final double drinkingPointsPerPotion;
    private final double drinkingPointsPerWaterBottle;
    private final int drinkingPointsPerFitnessLevel;
    private final int drinkingCooldown;

    private final int maxFitnessByHealth;
    private final int healthCheckInterval;
    private final double pointsAbove9Hearts;
    private final double pointsBelow5Hearts;
    private final double pointsBelow2Hearts;
    private final double healthPointsPerFitnessLevel;

    private final int maxFitnessByWalking;
    private final int cmPerWalkingLevel;

    private final int maxFitnessBySprinting;
    private final int cmPerSprintingLevel;

    private final int maxFitnessByClimbing;
    private final int cmPerClimbingLevel;

    private final int maxFitnessBySwimming;
    private final int cmPerSwimmingLevel;

    private final int maxFitnessByFlying;
    private final int cmPerFlyingLevel;

    private final int maxFitnessByEating;
    private final double pointsForLuxuryFood;
    private final double pointsForCheapFood;
    private final double eatingPointsPerFitnessLevel;
    private final List<String> cheapFood;
    private final List<String> luxuryFood;

    private final boolean fitnessDeathPunishmentEnabled;
    private final int fitnessDeathPunishmentAmount;
    private final int fitnessDeathPunishmentDuration;

    private final boolean rainSlowdownEnabled;

    private final Map<Integer, FitnessLevelEffect> levelEffects = new HashMap<>();

    @SneakyThrows
    public FitnessConfiguration(File file) {
        super(file, "fitness.yml", "default-fitness.yml");

        this.maxFitnessLevel = rootNode.node("fitness", "max-fitness-level").getInt(225);
        this.defaultFitnessLevel = rootNode.node("fitness", "default-fitness-level").getInt(20);
        this.rainSlowdownEnabled = rootNode.node("fitness", "rain-slowdown-enabled").getBoolean(false);

        ConfigurationNode drinkingNode = rootNode.node("fitness", "statistics", "drinking");
        this.maxFitnessByDrinking = drinkingNode.node("max-fitness").getInt(20);
        this.drinkingPointsPerPotion = drinkingNode.node("points-per-potion").getDouble(0.05);
        this.drinkingPointsPerWaterBottle = drinkingNode.node("points-per-water-bottle").getDouble(0.02);
        this.drinkingPointsPerFitnessLevel = drinkingNode.node("points-per-fitness-level").getInt(1);
        this.drinkingCooldown = drinkingNode.node("drinking-cooldown").getInt(5);

        ConfigurationNode healthNode = rootNode.node("fitness", "statistics", "health");
        this.maxFitnessByHealth = healthNode.node("max-fitness").getInt(10);
        this.healthCheckInterval = healthNode.node("check-interval").getInt(3600);
        this.pointsAbove9Hearts = healthNode.node("points-above-9-hearts").getDouble(0.08);
        this.pointsBelow5Hearts = healthNode.node("points-below-5-hearts").getDouble(-0.066);
        this.pointsBelow2Hearts = healthNode.node("points-below-2-hearts").getDouble(-0.1);
        this.healthPointsPerFitnessLevel = healthNode.node("points-per-health-level").getDouble(1.0);

        ConfigurationNode eatingNode = rootNode.node("fitness", "statistics", "eating");
        this.maxFitnessByEating = eatingNode.node("max-fitness").getInt(20);
        this.pointsForLuxuryFood = eatingNode.node("points-for-luxury-food").getDouble(0.05);
        this.pointsForCheapFood = eatingNode.node("points-for-cheap-food").getDouble(0.02);
        this.eatingPointsPerFitnessLevel = eatingNode.node("points-per-fitness-level").getDouble(1.0);
        this.luxuryFood = eatingNode.node("food-items", "luxury").getList(String.class, List.of("COOKED_BEEF", "MUSHROOM_STEW", "COOKED_PORKCHOP", "COOKED_SALMON", "COOKED_COD", "BAKED_POTATO", "COOKED_RABBIT"));
        this.cheapFood = eatingNode.node("food-items", "cheap").getList(String.class, List.of("APPLE", "BREAD", "MELON_BLOCK", "RAW_FISH", "COOKED_CHICKEN", "COOKED_MUTTON", "COOKIE"));

        ConfigurationNode statisticsNode = rootNode.node("fitness", "statistics");
        this.maxFitnessByWalking = statisticsNode.node("walking", "max-fitness").getInt(30);
        this.cmPerWalkingLevel = statisticsNode.node("walking", "cm-per-level").getInt(1000000);

        this.maxFitnessBySprinting = statisticsNode.node("sprinting", "max-fitness").getInt(40);
        this.cmPerSprintingLevel = statisticsNode.node("sprinting", "cm-per-level").getInt(2000000);

        this.maxFitnessByClimbing = statisticsNode.node("climbing", "max-fitness").getInt(30);
        this.cmPerClimbingLevel = statisticsNode.node("climbing", "cm-per-level").getInt(500000);

        this.maxFitnessBySwimming = statisticsNode.node("swimming", "max-fitness").getInt(30);
        this.cmPerSwimmingLevel = statisticsNode.node("swimming", "cm-per-level").getInt(600000);

        this.maxFitnessByFlying = statisticsNode.node("flying", "max-fitness").getInt(30);
        this.cmPerFlyingLevel = statisticsNode.node("flying", "cm-per-level").getInt(3000000);

        ConfigurationNode deathPunishmentNode = rootNode.node("fitness", "death-punishment");
        this.fitnessDeathPunishmentEnabled = deathPunishmentNode.node("enabled").getBoolean(true);
        this.fitnessDeathPunishmentAmount = deathPunishmentNode.node("amount").getInt(-20);
        this.fitnessDeathPunishmentDuration = deathPunishmentNode.node("duration").getInt(1440);

        ConfigurationNode levelNode = rootNode.node("fitness", "levels");
        levelNode.childrenMap().forEach((key, val) -> {
            try {
                String[] range = key.toString().split("-");
                int minLevel = Integer.parseInt(range[0]);
                int maxLevel = Integer.parseInt(range[1]);

                FitnessLevelEffect fitnessLevelEffect = new FitnessLevelEffect(
                        val.node("walk-speed").getDouble(0.1),
                        val.node("effects").getList(String.class, List.of("JUMP_BOOST:1"))
                );

                for (int i = minLevel; i <= maxLevel; i++) {
                    this.levelEffects.put(i, fitnessLevelEffect);
                }
            } catch (SerializationException e) {
                OpenMinetopia.getInstance().getLogger().severe("An error occurred while loading the fitness levels.");
                e.printStackTrace();
            } catch (NumberFormatException e) {
                OpenMinetopia.getInstance().getLogger().severe("Invalid level range format in configuration: " + key);
            }
        });
    }
}