package nl.openminetopia.modules.time.tasks;

import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.modules.places.PlacesModule;
import nl.openminetopia.modules.places.models.WorldModel;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalTime;
import java.util.Calendar;

public class TimeSyncRunnable extends BukkitRunnable {


    @Override
    public void run() {
        PlacesModule placesModule = OpenMinetopia.getModuleManager().getModule(PlacesModule.class);
        Calendar calendar = Calendar.getInstance();

        for (WorldModel worldModel : placesModule.worldModels) {
            World bukkitWorld = Bukkit.getWorld(worldModel.getName());
            if (bukkitWorld == null) return;

            bukkitWorld.setTime((1000L * calendar.get(Calendar.HOUR_OF_DAY)) + (16 * (calendar.get(Calendar.MINUTE) + 1)) - 6000);
        }
    }
}
