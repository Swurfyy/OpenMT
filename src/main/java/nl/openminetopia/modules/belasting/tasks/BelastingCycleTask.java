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
        // Run async to prevent server lag
        OpenMinetopia.getInstance().getServer().getScheduler().runTaskAsynchronously(OpenMinetopia.getInstance(), () -> {
            OpenMinetopia.getInstance().getLogger().info("[Belasting] Cycle task gestart - controleer of nieuwe cycle moet worden uitgevoerd...");
            module.getTaxService().runCycle().exceptionally(ex -> {
                OpenMinetopia.getInstance().getLogger().severe("[Belasting] Cycle task gefaald: " + ex.getMessage());
                ex.printStackTrace();
                return null;
            });
        });
    }
}
