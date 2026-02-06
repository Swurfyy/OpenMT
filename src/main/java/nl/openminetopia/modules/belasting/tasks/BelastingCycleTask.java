package nl.openminetopia.modules.belasting.tasks;

import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.belasting.BelastingModule;
import org.bukkit.scheduler.BukkitRunnable;

public class BelastingCycleTask extends BukkitRunnable {

    private final BelastingModule module;

    public BelastingCycleTask(BelastingModule module) {
        this.module = module;
    }

    @Override
    public void run() {
        module.getTaxService().runCycle().exceptionally(ex -> {
            OpenMinetopia.getInstance().getLogger().warning("Belasting cycle failed: " + ex.getMessage());
            return null;
        });
    }
}
