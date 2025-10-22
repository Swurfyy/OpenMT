package nl.openminetopia.modules.elytra.configuration;

import lombok.Getter;
import nl.openminetopia.utils.config.ConfigurateConfig;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.File;

@Getter
public class ElytraConfiguration extends ConfigurateConfig {

    private final boolean enabled;
    private final double maxVerticalSpeed;
    private final double maxHorizontalSpeed;
    private final int detectionInterval;
    private final boolean preventFireworkBoost;
    private final boolean preventGroundSpam;
    private final boolean preventConstantFlying;

    public ElytraConfiguration(File file) {
        super(file, "elytra.yml", "default/elytra.yml", true);

        ConfigurationNode elytraNode = rootNode.node("elytra");

        this.enabled = elytraNode.node("enabled").getBoolean(true);
        this.maxVerticalSpeed = elytraNode.node("max-vertical-speed").getDouble(0.5);
        this.maxHorizontalSpeed = elytraNode.node("max-horizontal-speed").getDouble(2.0);
        this.detectionInterval = elytraNode.node("detection-interval").getInt(5);
        this.preventFireworkBoost = elytraNode.node("prevent-firework-boost").getBoolean(true);
        this.preventGroundSpam = elytraNode.node("prevent-ground-spam").getBoolean(true);
        this.preventConstantFlying = elytraNode.node("prevent-constant-flying").getBoolean(true);
    }
}
