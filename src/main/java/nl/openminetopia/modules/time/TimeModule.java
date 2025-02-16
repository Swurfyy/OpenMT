package nl.openminetopia.modules.time;

import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.Module;
import nl.openminetopia.modules.places.PlacesModule;
import nl.openminetopia.modules.time.tasks.TimeSyncRunnable;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;

public class TimeModule extends Module {

    @Override
    public void enable() {
        if (!OpenMinetopia.getDefaultConfiguration().isSyncTime()) return;

        PlacesModule placesModule = OpenMinetopia.getModuleManager().getModule(PlacesModule.class);
        placesModule.worldModels.forEach(worldModel -> {
            World bukkitWorld = Bukkit.getWorld(worldModel.getName());
            if (bukkitWorld == null) return;

            bukkitWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        });

        TimeSyncRunnable runnable = new TimeSyncRunnable();
        runnable.runTaskTimer(OpenMinetopia.getInstance(), 0L, 200L);
    }

    @Override
    public void disable() {

    }
}
