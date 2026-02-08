package nl.openminetopia.modules.reactor.configuration;

import lombok.Getter;
import nl.openminetopia.utils.config.ConfigurateConfig;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.File;

@Getter
public class ReactorConfiguration extends ConfigurateConfig {

    private final boolean enabled;
    
    // Required settings
    private final int requiredTeams;
    private final int requiredMembersPerTeam;
    
    // Timer settings
    private final int maxTimer;
    private final int incrementPerSecond;
    private final int decrementPerSecond;
    
    // Exhaustion settings
    private final long exhaustionDurationMs;
    
    // Reward settings
    private final double rewardAmount;

    public ReactorConfiguration(File dataFolder) {
        super(dataFolder, "reactor.yml", "default/reactor.yml", true);
        
        // Load enabled setting
        this.enabled = rootNode.node("enabled").getBoolean(true);
        
        // Load settings node
        ConfigurationNode settingsNode = rootNode.node("settings");
        
        // Required settings
        this.requiredTeams = settingsNode.node("required-teams").getInt(3);
        this.requiredMembersPerTeam = settingsNode.node("required-members-per-team").getInt(2);
        
        // Timer settings
        ConfigurationNode timerNode = settingsNode.node("timer");
        this.maxTimer = timerNode.node("max-timer").getInt(100);
        this.incrementPerSecond = timerNode.node("increment-per-second").getInt(1);
        this.decrementPerSecond = timerNode.node("decrement-per-second").getInt(1);
        
        // Exhaustion settings
        ConfigurationNode exhaustionNode = settingsNode.node("exhaustion");
        int exhaustionDurationMinutes = exhaustionNode.node("duration-minutes").getInt(30);
        this.exhaustionDurationMs = exhaustionDurationMinutes * 60 * 1000L; // Convert to milliseconds
        
        // Reward settings
        ConfigurationNode rewardNode = settingsNode.node("reward");
        this.rewardAmount = rewardNode.node("amount").getDouble(1000.0);
        
        // Save configuration to ensure all defaults are written
        saveConfiguration();
    }
}
