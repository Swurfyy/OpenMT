package nl.openminetopia.modules.belasting;

import com.jazzkuh.modulemanager.spigot.SpigotModuleManager;
import lombok.Getter;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.data.DataModule;
import nl.openminetopia.modules.belasting.calculator.PlotTaxCalculator;
import nl.openminetopia.modules.belasting.commands.*;
import nl.openminetopia.modules.belasting.configuration.BelastingConfiguration;
import nl.openminetopia.modules.belasting.configuration.BelastingScheduleConfiguration;
import nl.openminetopia.modules.belasting.gui.BelastingGUIManager;
import nl.openminetopia.modules.belasting.listeners.BelastingLoginListener;
import nl.openminetopia.modules.belasting.service.TaxService;
import nl.openminetopia.modules.belasting.storage.BelastingRepository;
import nl.openminetopia.modules.belasting.tasks.BelastingCycleTask;
import nl.openminetopia.modules.belasting.tasks.BelastingExclusionCleanupTask;
import nl.openminetopia.utils.modules.ExtendedSpigotModule;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
public class BelastingModule extends ExtendedSpigotModule {

    private BelastingConfiguration config;
    private BelastingScheduleConfiguration schedule;
    private BelastingRepository repository;
    private PlotTaxCalculator calculator;
    private TaxService taxService;
    private BelastingGUIManager guiManager;
    private BelastingCycleTask cycleTask;
    private BelastingExclusionCleanupTask exclusionCleanupTask;
    private BelastingLoginListener loginListener;

    public BelastingModule(SpigotModuleManager<@NotNull OpenMinetopia> moduleManager, DataModule dataModule) {
        super(moduleManager);
    }

    @Override
    public void onEnable() {
        config = new BelastingConfiguration(OpenMinetopia.getInstance().getDataFolder());
        config.saveConfiguration();

        schedule = new BelastingScheduleConfiguration(OpenMinetopia.getInstance().getDataFolder());
        // Don't save schedule here - it can corrupt the list format structure
        // Only save if we need to add missing cycles, not on every load

        repository = new BelastingRepository();
        calculator = new PlotTaxCalculator(config);
        taxService = new TaxService(config, schedule, repository, calculator);
        guiManager = new BelastingGUIManager(taxService, config);

        registerComponent(new BelastingCommand());
        registerComponent(new BelastingBetaalCommand());
        registerComponent(new BelastingExcludeCommand());
        registerComponent(new BelastingAdminUnpaidCommand());
        registerComponent(new BelastingAdminPaidCommand());
        registerComponent(new BelastingAdminListCommand());
        registerComponent(new BelastingAdminSimulateCommand());
        registerComponent(new BelastingAdminResetCycleCommand());
        registerComponent(new BelastingAdminOpenGuiCommand());
        loginListener = new BelastingLoginListener();
        registerComponent(loginListener);

        cycleTask = new BelastingCycleTask(this);
        cycleTask.runTaskTimer(OpenMinetopia.getInstance(), 200L, 72000L);

        exclusionCleanupTask = new BelastingExclusionCleanupTask(this);
        exclusionCleanupTask.runTaskTimer(OpenMinetopia.getInstance(), 36000L, 36000L);
    }

    @Override
    public void onDisable() {
        if (cycleTask != null) cycleTask.cancel();
        if (exclusionCleanupTask != null) exclusionCleanupTask.cancel();
    }

    /**
     * Reloads belasting config and all dependent services (calculator, taxService, guiManager, cycle task).
     * Call this when /omt reload is executed so GUI, messages, intervals and slot layout use the new config.
     */
    public void reload() {
        config = new BelastingConfiguration(OpenMinetopia.getInstance().getDataFolder());
        config.saveConfiguration();

        schedule = new BelastingScheduleConfiguration(OpenMinetopia.getInstance().getDataFolder());
        // Don't save schedule here - it can corrupt the list format structure
        // Only save if we need to add missing cycles, not on every reload

        calculator = new PlotTaxCalculator(config);
        taxService = new TaxService(config, schedule, repository, calculator);
        guiManager = new BelastingGUIManager(taxService, config);

        if (cycleTask != null) cycleTask.cancel();
        cycleTask = new BelastingCycleTask(this);
        cycleTask.runTaskTimer(OpenMinetopia.getInstance(), 200L, 72000L);
    }

    /**
     * Invalidate login cache for a player (called when invoice is paid)
     */
    public void invalidateLoginCache(UUID playerUuid) {
        if (loginListener != null) {
            loginListener.invalidateCache(playerUuid);
        }
    }
}
