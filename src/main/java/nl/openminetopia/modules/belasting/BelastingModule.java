package nl.openminetopia.modules.belasting;

import com.jazzkuh.modulemanager.spigot.SpigotModuleManager;
import lombok.Getter;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.data.DataModule;
import nl.openminetopia.modules.belasting.calculator.PlotTaxCalculator;
import nl.openminetopia.modules.belasting.commands.*;
import nl.openminetopia.modules.belasting.configuration.BelastingConfiguration;
import nl.openminetopia.modules.belasting.gui.BelastingGUIManager;
import nl.openminetopia.modules.belasting.listeners.BelastingLoginListener;
import nl.openminetopia.modules.belasting.service.TaxService;
import nl.openminetopia.modules.belasting.storage.BelastingRepository;
import nl.openminetopia.modules.belasting.tasks.BelastingCycleTask;
import nl.openminetopia.modules.belasting.tasks.BelastingExclusionCleanupTask;
import nl.openminetopia.utils.modules.ExtendedSpigotModule;
import org.jetbrains.annotations.NotNull;

@Getter
public class BelastingModule extends ExtendedSpigotModule {

    private BelastingConfiguration config;
    private BelastingRepository repository;
    private PlotTaxCalculator calculator;
    private TaxService taxService;
    private BelastingGUIManager guiManager;
    private BelastingCycleTask cycleTask;
    private BelastingExclusionCleanupTask exclusionCleanupTask;

    public BelastingModule(SpigotModuleManager<@NotNull OpenMinetopia> moduleManager, DataModule dataModule) {
        super(moduleManager);
    }

    @Override
    public void onEnable() {
        config = new BelastingConfiguration(OpenMinetopia.getInstance().getDataFolder());
        config.saveConfiguration();

        repository = new BelastingRepository();
        calculator = new PlotTaxCalculator(config);
        taxService = new TaxService(config, repository, calculator);
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
        registerComponent(new BelastingLoginListener());

        long intervalTicks = (long) config.getTaxIntervalDays() * 24 * 60 * 60 * 20L;
        cycleTask = new BelastingCycleTask(this);
        cycleTask.runTaskTimer(OpenMinetopia.getInstance(), 200L, Math.max(intervalTicks, 6000L));

        exclusionCleanupTask = new BelastingExclusionCleanupTask(this);
        exclusionCleanupTask.runTaskTimer(OpenMinetopia.getInstance(), 36000L, 36000L);
    }

    @Override
    public void onDisable() {
        if (cycleTask != null) cycleTask.cancel();
        if (exclusionCleanupTask != null) exclusionCleanupTask.cancel();
    }
}
