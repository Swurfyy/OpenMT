package nl.openminetopia.modules.belasting.tasks;

import nl.openminetopia.modules.belasting.BelastingModule;
import org.bukkit.scheduler.BukkitRunnable;

public class BelastingExclusionCleanupTask extends BukkitRunnable {

    private final BelastingModule module;

    public BelastingExclusionCleanupTask(BelastingModule module) {
        this.module = module;
    }

    @Override
    public void run() {
        module.getTaxService().cleanupExpiredExclusions();
    }
}
