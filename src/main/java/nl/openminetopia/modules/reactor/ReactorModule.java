package nl.openminetopia.modules.reactor;

import com.jazzkuh.modulemanager.spigot.SpigotModuleManager;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.BooleanFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import lombok.Getter;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.reactor.configuration.ReactorConfiguration;
import nl.openminetopia.modules.reactor.listeners.ReactorRegionListener;
import nl.openminetopia.modules.reactor.tasks.ReactorUpdateTask;
import nl.openminetopia.utils.modules.ExtendedSpigotModule;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

@Getter
public class ReactorModule extends ExtendedSpigotModule {

    public static BooleanFlag REACTOR_FLAG;

    private ReactorConfiguration config;
    private ReactorManager reactorManager;
    private ReactorUpdateTask updateTask;

    public ReactorModule(SpigotModuleManager<@NotNull OpenMinetopia> moduleManager) {
        super(moduleManager);
    }

    @Override
    public void onLoad() {
        loadFlags();
    }

    @Override
    public void onEnable() {
        // Load configuration
        config = new ReactorConfiguration(OpenMinetopia.getInstance().getDataFolder());
        
        // Check if feature is enabled
        if (!config.isEnabled()) {
            getLogger().info("ReactorModule is disabled in configuration. Skipping initialization.");
            return;
        }
        
        // Check if required plugins are enabled
        if (!Bukkit.getPluginManager().isPluginEnabled("BetterTeams")) {
            getLogger().error("BetterTeams is required but not enabled! Disabling ReactorModule.");
            return;
        }

        if (!Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) {
            getLogger().error("DecentHolograms is required but not enabled! Disabling ReactorModule.");
            return;
        }

        // Enable debug mode first (set to false in production)
        nl.openminetopia.modules.reactor.utils.BetterTeamsUtils.setDebugMode(true);
        
        // Re-initialize BetterTeams connection (in case it loaded after this class)
        // Use a delayed task to ensure BetterTeams is fully loaded
        Bukkit.getScheduler().runTaskLater(OpenMinetopia.getInstance(), () -> {
            nl.openminetopia.modules.reactor.utils.BetterTeamsUtils.reinitialize();
            
            // Verify initialization
            if (!nl.openminetopia.modules.reactor.utils.BetterTeamsUtils.isInitialized()) {
                getLogger().warn("BetterTeams initialization failed! Retrying in 2 seconds...");
                Bukkit.getScheduler().runTaskLater(OpenMinetopia.getInstance(), () -> {
                    nl.openminetopia.modules.reactor.utils.BetterTeamsUtils.reinitialize();
                    if (!nl.openminetopia.modules.reactor.utils.BetterTeamsUtils.isInitialized()) {
                        getLogger().error("BetterTeams initialization failed after retry! Some features may not work.");
                    } else {
                        getLogger().info("BetterTeams initialized successfully after retry!");
                    }
                }, 40L); // 2 seconds delay
            } else {
                getLogger().info("BetterTeams initialized successfully!");
            }
        }, 20L); // 1 second delay to ensure BetterTeams is fully loaded

        // Initialize manager
        reactorManager = new ReactorManager(this);

        // Register listeners
        registerComponent(new ReactorRegionListener(this));

        // Start update task (runs every second)
        updateTask = new ReactorUpdateTask(this);
        updateTask.runTaskTimer(OpenMinetopia.getInstance(), 0L, 20L); // 20 ticks = 1 second

        getLogger().info("ReactorModule enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (updateTask != null) {
            updateTask.cancel();
        }

        if (reactorManager != null) {
            reactorManager.disable();
        }

        getLogger().info("ReactorModule disabled.");
    }

    /**
     * Reloads the reactor module
     * Scans for new reactor regions and updates existing ones
     * Called when /omt reload is executed
     */
    public void reload() {
        // Reload configuration
        config = new ReactorConfiguration(OpenMinetopia.getInstance().getDataFolder());
        
        // Check if feature is enabled
        if (!config.isEnabled()) {
            getLogger().info("ReactorModule is disabled in configuration. Disabling all reactors.");
            if (reactorManager != null) {
                reactorManager.disable();
            }
            if (updateTask != null) {
                updateTask.cancel();
                updateTask = null;
            }
            return;
        }
        
        if (reactorManager == null) {
            getLogger().warn("Cannot reload ReactorModule: manager not initialized.");
            return;
        }

        // Scan for new reactor regions (existing ones are preserved)
        reactorManager.scanForReactors();
        
        getLogger().info("ReactorModule reloaded successfully!");
    }

    private void loadFlags() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            REACTOR_FLAG = new BooleanFlag("reactor");
            registry.register(REACTOR_FLAG);
            getLogger().info("Registered 'reactor' WorldGuard flag.");
        } catch (FlagConflictException e) {
            REACTOR_FLAG = (BooleanFlag) registry.get("reactor");
            getLogger().info("Reactor flag already exists, using existing flag.");
        } catch (Exception e) {
            getLogger().error("Failed to register reactor flag: " + e.getMessage());
        }
    }
}
